package org.umbrella.common.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import java.util.function.Supplier;

public class OpenFeignConfiguration {

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        var resolver = new DefaultBearerTokenResolver();
        resolver.setAllowUriQueryParameter(true);
        resolver.setAllowFormEncodedBodyParameter(true);
        return resolver;
    }

    @Bean
    public RequestInterceptor oauthRequestInterceptor(BearerTokenResolver tokenResolver, Supplier<Authentication> sysUserAuthSupplier) {
        return new OAuthRequestInterceptor(tokenResolver, sysUserAuthSupplier);
    }
}
