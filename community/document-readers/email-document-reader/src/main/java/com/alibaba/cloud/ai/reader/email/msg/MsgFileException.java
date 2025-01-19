/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.reader.email.msg;

/**
 * Base exception class for MSG file processing errors.
 * This class consolidates various MSG file related exceptions.
 * 
 * @author xiadong
 * @since 0.8.0
 */
public class MsgFileException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new MsgFileException with the specified detail message.
     *
     * @param message the detail message
     */
    public MsgFileException(String message) {
        super(message);
    }

    /**
     * Constructs a new MsgFileException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public MsgFileException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when the file is not a valid CFB file.
     */
    public static class NotCFBFileException extends MsgFileException {
        private static final long serialVersionUID = 1L;

        public NotCFBFileException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when encountering an unknown storage type.
     */
    public static class UnknownStorageTypeException extends MsgFileException {
        private static final long serialVersionUID = 1L;

        public UnknownStorageTypeException(String message) {
            super(message);
        }
    }
} 