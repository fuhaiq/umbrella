package org.keycloak.adapters.authorization;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.AuthorizationContext;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.adapters.authorization.spi.HttpResponse;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.common.util.Base64;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.util.JsonSerialization;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.umbrella.common.keycloak.KeycloakAuthenticationException;
import org.keycloak.authorization.client.util.HttpResponseException;

import java.util.*;
import java.util.function.Function;

import static org.keycloak.adapters.authorization.util.JsonUtils.asAccessToken;

@Slf4j
public class KeycloakPolicyEnforcer extends PolicyEnforcer {

    private final PolicyEnforcerConfig enforcerConfig;

    public KeycloakPolicyEnforcer(Builder builder) {
        super(builder);
        this.enforcerConfig = builder.getEnforcerConfig();
    }

    private final Function<Boolean, AuthorizationContext> emptyAuthContext = granted -> new ClientAuthorizationContext(getAuthzClient()) {
        @Override
        public boolean hasPermission(String resourceName, String scopeName) {
            return granted;
        }

        @Override
        public boolean hasResourcePermission(String resourceName) {
            return granted;
        }

        @Override
        public boolean hasScopePermission(String scopeName) {
            return granted;
        }

        @Override
        public List<Permission> getPermissions() {
            return List.of();
        }

        @Override
        public boolean isGranted() {
            return granted;
        }
    };

    @Override
    public AuthorizationContext enforce(HttpRequest request, @Nullable HttpResponse response) {
        return enforce(request);
    }

    private AuthorizationContext enforce(HttpRequest request) {
        var enforcementMode = enforcerConfig.getEnforcementMode();
        var principal = request.getPrincipal();
        var anonymous = principal == null || principal.getRawToken() == null;
        if (PolicyEnforcerConfig.EnforcementMode.DISABLED.equals(enforcementMode)) {
            if (anonymous) {
                throw new KeycloakAuthenticationException(HttpStatus.UNAUTHORIZED, "未认证");
            }
            return emptyAuthContext.apply(true);
        }

        var pathConfig = getPathConfig(request);

        if (anonymous) {
            if (!isDefaultAccessDeniedUri(request)) {
                if (pathConfig != null) {
                    if (PolicyEnforcerConfig.EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                        return emptyAuthContext.apply(true);
                    } else {
                        challenge(pathConfig, getRequiredScopes(pathConfig, request), request);
                    }
                } else {
                    handleAccessDenied();
                }
            }
            return emptyAuthContext.apply(false);
        }

        AccessToken accessToken = principal.getToken();


        if (accessToken != null) {
            if (pathConfig == null) {
                if (PolicyEnforcerConfig.EnforcementMode.PERMISSIVE.equals(enforcementMode)) {
                    return createAuthorizationContext(accessToken, null);
                }

                if (isDefaultAccessDeniedUri(request)) {
                    return createAuthorizationContext(accessToken, null);
                }

                handleAccessDenied();

                return emptyAuthContext.apply(false);
            }

            if (PolicyEnforcerConfig.EnforcementMode.DISABLED.equals(pathConfig.getEnforcementMode())) {
                return createAuthorizationContext(accessToken, pathConfig);
            }

            PolicyEnforcerConfig.MethodConfig methodConfig = getRequiredScopes(pathConfig, request);
            Map<String, List<String>> claims = resolveClaims(pathConfig, request);

            if (isAuthorized(pathConfig, methodConfig, accessToken, request, claims)) {
                try {
                    return createAuthorizationContext(accessToken, pathConfig);
                } catch (Exception e) {
                    log.error("内部错误", e);
                    throw new KeycloakAuthenticationException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing path [" + pathConfig.getPath() + "].", e);
                }
            }

            AccessToken original = accessToken;

            accessToken = requestAuthorizationToken(pathConfig, methodConfig, request, claims);

            if (accessToken != null) {
                AccessToken.Authorization authorization = original.getAuthorization();

                if (authorization == null) {
                    authorization = new AccessToken.Authorization();
                    authorization.setPermissions(new ArrayList<>());
                }

                AccessToken.Authorization newAuthorization = accessToken.getAuthorization();

                if (newAuthorization != null) {
                    Collection<Permission> grantedPermissions = authorization.getPermissions();
                    Collection<Permission> newPermissions = newAuthorization.getPermissions();

                    for (Permission newPermission : newPermissions) {
                        if (!grantedPermissions.contains(newPermission)) {
                            grantedPermissions.add(newPermission);
                        }
                    }
                }

                original.setAuthorization(authorization);

                if (isAuthorized(pathConfig, methodConfig, accessToken, request, claims)) {
                    try {
                        return createAuthorizationContext(accessToken, pathConfig);
                    } catch (Exception e) {
                        log.error("内部错误", e);
                        throw new KeycloakAuthenticationException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing path [" + pathConfig.getPath() + "].", e);
                    }
                }
            }

            if (methodConfig != null && PolicyEnforcerConfig.ScopeEnforcementMode.DISABLED.equals(methodConfig.getScopesEnforcementMode())) {
                return emptyAuthContext.apply(true);
            }

            if (!challenge(pathConfig, methodConfig, request)) {
                handleAccessDenied();
            }
        }

        return emptyAuthContext.apply(false);
    }


    private AccessToken requestAuthorizationToken(PolicyEnforcerConfig.PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, HttpRequest request, Map<String, List<String>> claims) {
        if (enforcerConfig.getUserManagedAccess() != null) {
            return null;
        }

        try {
            TokenPrincipal principal = request.getPrincipal();
            String accessTokenString = principal.getRawToken();
            AccessToken accessToken = principal.getToken();
            AuthorizationRequest authzRequest = new AuthorizationRequest();

            if (isBearerAuthorization(request) || accessToken.getAuthorization() != null) {
                authzRequest.addPermission(pathConfig.getId(), methodConfig.getScopes());
            }

            if (!claims.isEmpty()) {
                authzRequest.setClaimTokenFormat("urn:ietf:params:oauth:token-type:jwt");
                authzRequest.setClaimToken(Base64.encodeBytes(JsonSerialization.writeValueAsBytes(claims)));
            }

            if (accessToken.getAuthorization() != null) {
                authzRequest.setRpt(accessTokenString);
            }

            AuthorizationResponse authzResponse;

            if (isBearerAuthorization(request)) {
                authzRequest.setSubjectToken(accessTokenString);
                authzResponse = getAuthzClient().authorization().authorize(authzRequest);
            } else {
                authzResponse = getAuthzClient().authorization(accessTokenString).authorize(authzRequest);
            }

            if (authzResponse != null) {
                return asAccessToken(authzResponse.getToken());
            }
        } catch (AuthorizationDeniedException denied) {
            throw new KeycloakAuthenticationException(HttpStatus.FORBIDDEN, "权限不足", denied);
        } catch (Exception error) {
            if(error.getCause() instanceof HttpResponseException failed) {
                throw new KeycloakAuthenticationException(HttpStatus.valueOf(failed.getStatusCode()), failed.getMessage(), failed);
            } else {
                throw new KeycloakAuthenticationException(HttpStatus.INTERNAL_SERVER_ERROR, error);
            }
        }
        return null;
    }


    private AuthorizationContext createAuthorizationContext(AccessToken accessToken, PolicyEnforcerConfig.PathConfig pathConfig) {
        return new ClientAuthorizationContext(accessToken, pathConfig, getAuthzClient());
    }

    private boolean challenge(PolicyEnforcerConfig.PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, HttpRequest request) {
        if (isBearerAuthorization(request)) {
            String ticket = getPermissionTicket(pathConfig, methodConfig, getAuthzClient(), request);

            if (ticket != null) {

                var wwwAuthenticate = "UMA realm=\"" + getAuthzClient().getConfiguration().getRealm() + "\"" + ",as_uri=\"" +
                        getAuthzClient().getServerConfiguration().getIssuer() + "\"" + ",ticket=\"" + ticket + "\"";

                throw new KeycloakAuthenticationException(HttpStatus.UNAUTHORIZED, "未认证", Map.of(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate));
            } else {
                throw new KeycloakAuthenticationException(HttpStatus.UNAUTHORIZED, "未认证");
            }
        }

        handleAccessDenied();

        return true;
    }

    private String getPermissionTicket(PolicyEnforcerConfig.PathConfig pathConfig, PolicyEnforcerConfig.MethodConfig methodConfig, AuthzClient authzClient, HttpRequest httpFacade) {
        if (enforcerConfig.getUserManagedAccess() != null) {
            ProtectionResource protection = authzClient.protection();
            PermissionResource permission = protection.permission();
            PermissionRequest permissionRequest = new PermissionRequest();

            permissionRequest.setResourceId(pathConfig.getId());
            permissionRequest.setScopes(new HashSet<>(methodConfig.getScopes()));

            Map<String, List<String>> claims = resolveClaims(pathConfig, httpFacade);

            if (!claims.isEmpty()) {
                permissionRequest.setClaims(claims);
            }

            return permission.create(permissionRequest).getTicket();
        }

        return null;
    }

    private boolean isBearerAuthorization(HttpRequest request) {
        List<String> authHeaders = request.getHeaders("Authorization");

        if (authHeaders != null) {
            for (String authHeader : authHeaders) {
                String[] split = authHeader.trim().split("\\s+");
                if (split == null || split.length != 2) continue;
                if (!split[0].equalsIgnoreCase("Bearer")) continue;
                return true;
            }
        }

        return getAuthzClient().getConfiguration().isBearerOnly();
    }

    private PolicyEnforcerConfig.MethodConfig getRequiredScopes(PolicyEnforcerConfig.PathConfig pathConfig, HttpRequest request) {
        String method = request.getMethod();

        for (PolicyEnforcerConfig.MethodConfig methodConfig : pathConfig.getMethods()) {
            if (methodConfig.getMethod().equals(method)) {
                return methodConfig;
            }
        }

        PolicyEnforcerConfig.MethodConfig methodConfig = new PolicyEnforcerConfig.MethodConfig();

        methodConfig.setMethod(request.getMethod());
        var scopes = new ArrayList<String>();

        if (Boolean.TRUE.equals(enforcerConfig.getHttpMethodAsScope())) {
            scopes.add(request.getMethod());
        } else {
            scopes.addAll(pathConfig.getScopes());
        }

        methodConfig.setScopes(scopes);
        methodConfig.setScopesEnforcementMode(PolicyEnforcerConfig.ScopeEnforcementMode.ANY);

        return methodConfig;
    }

    private void handleAccessDenied() {
        String accessDeniedPath = enforcerConfig.getOnDenyRedirectTo();

        if (accessDeniedPath != null) {
            throw new KeycloakAuthenticationException(HttpStatus.FORBIDDEN, "权限不足");
        } else {
            throw new KeycloakAuthenticationException(HttpStatus.FORBIDDEN, "权限不足");
        }
    }

    private PolicyEnforcerConfig.PathConfig getPathConfig(HttpRequest request) {
        return isDefaultAccessDeniedUri(request) ? null : getPathMatcher().matches(getPath(request));
    }

    private boolean isDefaultAccessDeniedUri(HttpRequest request) {
        String accessDeniedPath = enforcerConfig.getOnDenyRedirectTo();
        return accessDeniedPath != null && request.getURI().contains(accessDeniedPath);
    }

    private String getPath(HttpRequest request) {
        return request.getRelativePath();
    }
}
