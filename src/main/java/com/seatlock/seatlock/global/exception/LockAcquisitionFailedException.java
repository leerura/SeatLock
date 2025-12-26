package com.seatlock.seatlock.global.exception;

public class LockAcquisitionFailedException extends RuntimeException{

    private final ErrorCode errorCode;

    public LockAcquisitionFailedException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
