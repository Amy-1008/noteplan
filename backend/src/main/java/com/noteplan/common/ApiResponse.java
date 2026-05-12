package com.noteplan.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<T>(200, "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<Void>(200, "success", null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<T>(code, message, null);
    }
}
