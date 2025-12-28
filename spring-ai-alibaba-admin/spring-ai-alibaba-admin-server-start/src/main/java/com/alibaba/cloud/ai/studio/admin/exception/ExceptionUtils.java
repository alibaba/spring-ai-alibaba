package com.alibaba.cloud.ai.studio.admin.exception;

import com.alibaba.nacos.common.utils.StringUtils;

import java.util.Objects;

public class ExceptionUtils {
    
    public static String getAllExceptionMsg(Throwable e) {
        Throwable cause = e;
        StringBuilder strBuilder = new StringBuilder();
        
        while (cause != null && !StringUtils.isEmpty(cause.getMessage())) {
            strBuilder.append("caused: ").append(cause.getMessage()).append(';');
            cause = cause.getCause();
        }
        
        return strBuilder.toString();
    }
    
    public static Throwable getCause(final Throwable t) {
        final Throwable cause = t.getCause();
        if (Objects.isNull(cause)) {
            return t;
        }
        return cause;
    }

}
