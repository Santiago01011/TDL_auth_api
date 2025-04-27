package com.ToDoList.auth.exception;

public class VerificationCodeExpiredException extends AppException {
    public VerificationCodeExpiredException(String message) {
      super("VERIFICATION_EXPIRED", message);
    }
}
