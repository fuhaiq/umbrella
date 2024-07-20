package org.umbrella.common.core.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.umbrella.common.util.ErrorCode;
import org.umbrella.common.util.R;

import java.util.HashMap;

@RestControllerAdvice
@Slf4j
public class ExceptionAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public R methodArgumentNotValidException(MethodArgumentNotValidException e) {
        var map = new HashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(err -> map.put(err.getField(), err.getDefaultMessage()));
        return R.failed(map, ErrorCode.VALIDATE_ERR);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public R httpMessageNotReadableException(HttpMessageNotReadableException e) {
        return R.failed(e.getMessage(), ErrorCode.BAD_MESSAGE_ERR);
    }

    @ExceptionHandler({FeignException.class})
    public ResponseEntity<Object> handleConflict(FeignException e) {
        log.warn("Feign调用异常{}", e.getMessage());
        return ResponseEntity.status(e.status()).body(e.contentUTF8());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NoResourceFoundException.class})
    public R noResourceFoundException(NoResourceFoundException e) {
        return R.failed(e.getMessage(), ErrorCode.NOT_FOUND);
    }


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({Throwable.class})
    public R throwable(Throwable e) {
        log.error("系统异常", e);
        return R.failed(e.getMessage());
    }

}
