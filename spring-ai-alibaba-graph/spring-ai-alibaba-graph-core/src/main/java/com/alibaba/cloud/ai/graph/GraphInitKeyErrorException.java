package com.alibaba.cloud.ai.graph;

public class GraphInitKeyErrorException extends RuntimeException {
    public GraphInitKeyErrorException() {
    }

    public GraphInitKeyErrorException(String message) {
        super(message);
    }

    public GraphInitKeyErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public GraphInitKeyErrorException(Throwable cause) {
        super(cause);
    }

    public GraphInitKeyErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
