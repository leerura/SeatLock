package com.seatlock.seatlock.common;

import com.seatlock.seatlock.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;
    private final LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorResponse(errorCode.getCode(), errorCode.getMessage()),
                LocalDateTime.now()
        );
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorResponse(errorCode.getCode(), message),
                LocalDateTime.now()
        );
    }







    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private final String code;
        private final String message;
    }
}
