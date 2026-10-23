package edu.net.environment.config;

import edu.net.environment.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);
    @org.springframework.web.bind.annotation.ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResult<Object>> business(ApiException ex) {
        return ResponseEntity.status(ex.status()).body(new ApiResult<>(ex.status(),ex.getMessage(),null));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<Object>> validation(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).findFirst().orElse("参数不正确");
        return ResponseEntity.badRequest().body(new ApiResult<>(400,message,null));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResult<Object>> input(Exception ex) {
        return ResponseEntity.badRequest().body(new ApiResult<>(400,"请求格式或参数不正确",null));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<Object>> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(new ApiResult<>(409,"记录已存在或关联数据不正确，请刷新后重试",null));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> unexpected(Exception ex) {
        log.error("Request failed",ex);
        return ResponseEntity.internalServerError().body(new ApiResult<>(500,"服务暂时不可用，请稍后重试",null));
    }
}
