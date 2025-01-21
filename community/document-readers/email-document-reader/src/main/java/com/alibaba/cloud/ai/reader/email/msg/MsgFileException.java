package com.alibaba.cloud.ai.reader.email.msg;

/**
 * MSG文件解析异常
 * 用于处理MSG文件解析过程中的错误
 */
public class MsgFileException extends RuntimeException {
    
    public MsgFileException(String message) {
        super(message);
    }
    
    public MsgFileException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public MsgFileException(Throwable cause) {
        super(cause);
    }
} 