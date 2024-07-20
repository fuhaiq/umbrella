/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.umbrella.common.security;

import cn.hutool.core.util.CharsetUtil;
import com.alibaba.nacos.common.http.param.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.umbrella.common.util.ErrorCode;
import org.umbrella.common.keycloak.KeycloakAuthenticationException;
import org.umbrella.common.util.R;

import java.io.PrintWriter;

/**
 * @author lengleng
 * @date 2019/2/1
 * <p>
 * 客户端异常处理 AuthenticationException 不同细化异常处理
 */
@RequiredArgsConstructor
public class ResourceAuthExceptionEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        response.setCharacterEncoding(CharsetUtil.UTF_8);
        response.setContentType(MediaType.APPLICATION_JSON);
        R<String> ret = R.failed(); // 默认系统异常

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        if (authException != null) {
            ret = R.failed(authException.getMessage(), ErrorCode.AUTHORIZATION_ERR);
        }

        if (authException instanceof KeycloakAuthenticationException ex) {
            var statue = ex.getStatus();
            response.setStatus(statue.value());
            var headers = ex.getHeaders();
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(response::setHeader);
            }
        }
        PrintWriter printWriter = response.getWriter();
        printWriter.append(objectMapper.writeValueAsString(ret));
    }

}
