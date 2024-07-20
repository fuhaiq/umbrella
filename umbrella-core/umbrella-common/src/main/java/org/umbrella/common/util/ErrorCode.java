package org.umbrella.common.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 错误码 格式: 2位业务编号 + 3位错误码
 * 00 通用业务
 */
@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    // 通用错误码
    SUCCESS(0, "成功"),
    VALIDATE_ERR(10001, "参数校验失败"),
    AUTHORIZATION_ERR(10002, "认证失败"),
    BAD_MESSAGE_ERR(10003, "消息格式错误"),
    NOT_FOUND(10004, "未找到资源"),

    FAIL(-1, "系统错误");

    private final int code;
    private final String message;
}
