package com.TrashTDL.ServerlessAuth.exception;

public class VerificationCodeExpiredException extends AppException {
    public VerificationCodeExpiredException(String message) {
      super("VERIFICATION_EXPIRED", message);
    }
}
