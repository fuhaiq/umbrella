package org.umbrella.mq;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.umbrella.common.security.HttpSecurityConfiguration;

@SpringBootApplication
@EnableDiscoveryClient
@SecurityScheme(
        name = "Keycloak",
        openIdConnectUrl = "http://localhost:8180/realms/umbrella/.well-known/openid-configuration",
        scheme = "bearer",
        type = SecuritySchemeType.OPENIDCONNECT,
        in = SecuritySchemeIn.HEADER
)
@EnableFeignClients(basePackages = "org.umbrella.api.feign")
@Import({HttpSecurityConfiguration.class})
public class MqApplication {
    public static void main(String[] args) {
        SpringApplication.run(MqApplication.class, args);
    }
}
