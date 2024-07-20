package org.umbrella.common.keycloak;

import lombok.Data;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@RefreshScope
@Component
@ConfigurationProperties(prefix = "keycloak.enforcer")
@Data
public class KeycloakPolicyEnforcerProperty {
    private PolicyEnforcerConfig policyEnforcerConfig;
}
