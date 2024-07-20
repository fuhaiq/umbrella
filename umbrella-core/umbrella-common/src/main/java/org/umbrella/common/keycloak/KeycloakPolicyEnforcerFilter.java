package org.umbrella.common.keycloak;

import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.authorization.KeycloakPolicyEnforcer;
import org.keycloak.adapters.authorization.integration.elytron.ServletHttpRequest;
import org.keycloak.adapters.authorization.integration.elytron.ServletHttpResponse;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Component
public class KeycloakPolicyEnforcerFilter extends OncePerRequestFilter {

    private final Map<PolicyEnforcerConfig, KeycloakPolicyEnforcer> policyEnforcerMapper = new ConcurrentHashMap<>();

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

    private final AuthenticationFailureHandler authenticationFailureHandler;

    private final Function<PolicyEnforcerConfig, KeycloakPolicyEnforcer> policyEnforcerProvider;

    private final ConfigurationResolver configResolver;

    public KeycloakPolicyEnforcerFilter(AuthenticationEntryPoint authenticationEntryPoint,
                                        Function<PolicyEnforcerConfig, KeycloakPolicyEnforcer> policyEnforcerProvider,
                                        ConfigurationResolver configResolver) {
        this.authenticationFailureHandler = new AuthenticationEntryPointFailureHandler(authenticationEntryPoint);
        this.policyEnforcerProvider = policyEnforcerProvider;
        this.configResolver = configResolver;
    }

    @Override
    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        @Nullable
        var jwt = (JwtAuthenticationToken) securityContextHolderStrategy.getContext().getAuthentication();

        try {
            var spiRequest = new ServletHttpRequest(request, () -> Optional.ofNullable(jwt).isPresent() ? jwt.getToken().getTokenValue() : null);
            var policyEnforcer = policyEnforcerMapper.computeIfAbsent(configResolver.resolve(spiRequest), policyEnforcerProvider);
            var authzContext = policyEnforcer.enforce(spiRequest, new ServletHttpResponse(response));
            if (!authzContext.isGranted())
                throw new KeycloakAuthenticationException(HttpStatus.FORBIDDEN, "权限不足");
            chain.doFilter(request, response);
        } catch (AuthenticationException failed) {
            this.securityContextHolderStrategy.clearContext();
            this.authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
        }
    }
}
