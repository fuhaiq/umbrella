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

package org.umbrella.common.util;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 响应信息主体
 *
 * @param <T>
 * @author lengleng
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private int code;

    @Getter
    @Setter
    private String msg;

    @Getter
    @Setter
    private T data;

    public static <T> R<T> ok() {
        return restResult(null, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage());
    }

    public static <T> R<T> ok(T data) {
        return restResult(data, ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage());
    }

    public static <T> R<T> failed() {
        return restResult(null, ErrorCode.FAIL.getCode(), ErrorCode.FAIL.getMessage());
    }

    public static <T> R<T> failed(T data) {
        return restResult(data, ErrorCode.FAIL.getCode(), ErrorCode.FAIL.getMessage());
    }

    public static <T> R<T> failed(ErrorCode errorCode) {
        return restResult(null, errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> R<T> failed(T data, ErrorCode errorCode) {
        return restResult(data, errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> R<T> restResult(T data, int code, String msg) {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }

}
