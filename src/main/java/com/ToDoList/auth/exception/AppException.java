package com.ToDoList.auth.exception;

public abstract class AppException extends RuntimeException {
    private final String errorCode;
    
    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}