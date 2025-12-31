package com.alibaba.cloud.ai.studio.admin.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {

    /**
     * 错误消息
     */
    private String message;

    /**
     * 字段错误详情列表
     */
    private List<FieldError> fieldErrors;

    /**
     * 字段错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        
        /**
         * 错误字段名
         */
        private String field;
        
        /**
         * 被拒绝的值
         */
        private Object rejectedValue;
        
        /**
         * 错误消息
         */
        private String message;
    }
}
