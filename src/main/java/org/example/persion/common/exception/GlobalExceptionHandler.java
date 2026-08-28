package org.example.persion.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.persion.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception boundary. Business errors may expose their intentional
 * messages; unexpected internal exceptions are logged server-side and return a
 * generic response so SQL/path/implementation details do not leak to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("Unhandled application exception", exception);
        return Result.error("系统繁忙，请稍后重试");
    }
}
