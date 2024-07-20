package org.umbrella.common.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Component
@Slf4j
public class KeycloakSysUserAuthSupplier implements Supplier<Authentication> {
    private final Keycloak keycloak;

    private final JwtDecoder jwtDecoder;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();

    private final Lock writeLock = lock.writeLock();

    private AccessTokenResponse accessTokenResponse;

    public KeycloakSysUserAuthSupplier(Keycloak keycloak, JwtDecoder jwtDecoder) {
        this.keycloak = keycloak;
        accessTokenResponse = keycloak.tokenManager().getAccessToken();
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Authentication get() {
        readLock.lock();
        try {
            return new JwtAuthenticationToken(jwtDecoder.decode(accessTokenResponse.getToken()));
        } finally {
            readLock.unlock();
        }
    }

    @Scheduled(timeUnit = TimeUnit.MINUTES, fixedRate = 1, initialDelay = 1) // 延时2分钟后执行,2分钟刷新一次
    public void refreshAccessToken() {
        writeLock.lock();
        try {
            accessTokenResponse = keycloak.tokenManager().refreshToken();
        } finally {
            writeLock.unlock();
        }
    }
}
