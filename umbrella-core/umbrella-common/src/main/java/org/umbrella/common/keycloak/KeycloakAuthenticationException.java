package org.umbrella.common.keycloak;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

import java.util.Map;

@Getter
public class KeycloakAuthenticationException extends AuthenticationException {

    private final HttpStatus status;

    private final Map<String, String> headers;

    public KeycloakAuthenticationException(HttpStatus status) {
        this(status, null, null, null);
    }

    public KeycloakAuthenticationException(HttpStatus status, Throwable cause) {
        this(status, null, null, cause);
    }

    public KeycloakAuthenticationException(HttpStatus status, String msg) {
        this(status, msg, null, null);
    }

    public KeycloakAuthenticationException(HttpStatus status, String msg, Map<String, String> headers) {
        this(status, msg, headers, null);
    }

    public KeycloakAuthenticationException(HttpStatus status, String msg, Throwable cause) {
        this(status, msg, null, cause);
    }

    public KeycloakAuthenticationException(HttpStatus status, String msg, Map<String, String> headers, Throwable cause) {
        super(msg, cause);
        this.status = status;
        this.headers = headers;
    }
}
