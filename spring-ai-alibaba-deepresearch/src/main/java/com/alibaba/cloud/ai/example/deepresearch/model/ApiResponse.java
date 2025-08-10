package com.alibaba.cloud.ai.example.deepresearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiResponse<T>(

        @JsonProperty("code") Integer code,

        @JsonProperty("status") String status,

        @JsonProperty("message") String message,

        @JsonProperty("data") T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", "", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, "error", message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(500, "error", message, data);
    }
}
