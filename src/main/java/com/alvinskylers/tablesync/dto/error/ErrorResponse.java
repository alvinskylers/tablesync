package com.alvinskylers.tablesync.dto.error;

public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message, Object details) {}

    public record FieldErrorDetail(String field, String issue) {}

    public static ErrorResponse of (String code, String message, Object details) {
        return new ErrorResponse(new ErrorBody(code, message, details));
    }

}
