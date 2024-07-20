package org.umbrella.common.security;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.keycloak.adapters.authorization.KeycloakPolicyEnforcer;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.authorization.spi.ConfigurationResolver;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.umbrella.common.keycloak.KeycloakPolicyEnforcerProperty;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

@Configuration
public class ResourceServerConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public Function<PolicyEnforcerConfig, KeycloakPolicyEnforcer> policyEnforcerProvider() {
        return policyEnforcerConfig -> {
            var authServerUrl = policyEnforcerConfig.getAuthServerUrl();
            var builder = PolicyEnforcer.builder()
                    .authServerUrl(authServerUrl)
                    .realm(policyEnforcerConfig.getRealm())
                    .clientId(policyEnforcerConfig.getResource())
                    .credentials(policyEnforcerConfig.getCredentials())
                    .bearerOnly(true)
                    .enforcerConfig(policyEnforcerConfig);
            return new KeycloakPolicyEnforcer(builder);
        };
    }

    @Bean
    public ResourceAuthExceptionEntryPoint resourceAuthExceptionEntryPoint() {
        return new ResourceAuthExceptionEntryPoint(new JsonMapper());
    }

    @Bean
    public ConfigurationResolver configurationResolver(KeycloakPolicyEnforcerProperty keycloakPolicyEnforcerProperty) {
        return request -> keycloakPolicyEnforcerProperty.getPolicyEnforcerConfig();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        jwtDecoder.setJwtValidator(new JwtTimestampValidator(Duration.of(-5, ChronoUnit.SECONDS)));
        return jwtDecoder;
    }
}
