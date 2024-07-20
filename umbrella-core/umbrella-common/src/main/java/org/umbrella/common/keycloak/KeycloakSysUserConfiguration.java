package org.umbrella.common.keycloak;

import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@RequiredArgsConstructor
@EnableScheduling
public class KeycloakSysUserConfiguration {

    private final KeycloakSysUserProperty keycloakSysUserProperty;

    @Bean(destroyMethod = "close")
    public Keycloak keycloak() {
        final int KEYCLOAK_REST_CLIENT_POOL_SIZE = 5;
        return KeycloakBuilder.builder()
                .serverUrl(keycloakSysUserProperty.getServerUrl())
                .realm(keycloakSysUserProperty.getRealm())
                .username(keycloakSysUserProperty.getUsername())
                .password(keycloakSysUserProperty.getPassword())
                .clientId(keycloakSysUserProperty.getClientId())
                .clientSecret(keycloakSysUserProperty.getClientSecret())
                .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(KEYCLOAK_REST_CLIENT_POOL_SIZE).build())
                .build();
    }
}
