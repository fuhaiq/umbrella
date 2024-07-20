package org.umbrella.common.feign;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.umbrella.common.util.WebUtils;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class OAuthRequestInterceptor implements RequestInterceptor {

    private final BearerTokenResolver tokenResolver;

    private final Supplier<Authentication> sysUserAuthSupplier;

    @Override
    public void apply(RequestTemplate template) {
        String token;

        var optional = WebUtils.getRequest();

        if (optional.isEmpty()) { // 非用户发起的远程请求,可能来自内部定时任务
            // 检查请求头是否来自内部调用
            var fromInner = template.headers().get(StringPool.YES).contains(StringPool.YES);
            if(!fromInner) return;

            var jwt = (JwtAuthenticationToken) sysUserAuthSupplier.get();
            token = jwt.getToken().getTokenValue(); // 使用内部账号
        } else {
            var request = optional.get();

            token = tokenResolver.resolve(request);
            if (StrUtil.isBlank(token)) {
                return;
            }
        }

        template.header(HttpHeaders.AUTHORIZATION,
                String.format("%s %s", OAuth2AccessToken.TokenType.BEARER.getValue(), token));
    }
}
