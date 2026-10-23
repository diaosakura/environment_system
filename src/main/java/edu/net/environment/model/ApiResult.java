package edu.net.environment.model;

public record ApiResult<T>(int code, String message, T data) {
    public static <T> ApiResult<T> ok(T data) { return new ApiResult<>(0, "操作成功", data); }
}
