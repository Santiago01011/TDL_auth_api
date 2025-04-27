package com.ToDoList.auth.exception;

import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "VERIFICATION_EXPIRED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ApiError payload = new ApiError(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(payload, status);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException ex) {
        ApiError payload = new ApiError("BAD_REQUEST", ex.getMessage());
        return new ResponseEntity<>(payload, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        ApiError payload = new ApiError("UNAUTHORIZED", "Invalid credentials.");
        return new ResponseEntity<>(payload, HttpStatus.UNAUTHORIZED);
    }
}