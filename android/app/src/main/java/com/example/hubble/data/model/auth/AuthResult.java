package com.example.hubble.data.model.auth;

public class AuthResult<T> {
    public enum Status { LOADING, SUCCESS, ERROR }

    private final Status status;
    private final T data;
    private final String message;
    private final int errorCode;

    private AuthResult(Status status, T data, String message, int errorCode) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
    }

    public static <T> AuthResult<T> loading() {
        return new AuthResult<>(Status.LOADING, null, null, -1);
    }

    public static <T> AuthResult<T> success(T data) {
        return new AuthResult<>(Status.SUCCESS, data, null, -1);
    }

    public static <T> AuthResult<T> error(String message) {
        return new AuthResult<>(Status.ERROR, null, message, -1);
    }

    public static <T> AuthResult<T> error(String message, int errorCode) {
        return new AuthResult<>(Status.ERROR, null, message, errorCode);
    }

    public Status getStatus() { return status; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public int getErrorCode() { return errorCode; }

    public boolean isLoading() { return status == Status.LOADING; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isError() { return status == Status.ERROR; }
}
