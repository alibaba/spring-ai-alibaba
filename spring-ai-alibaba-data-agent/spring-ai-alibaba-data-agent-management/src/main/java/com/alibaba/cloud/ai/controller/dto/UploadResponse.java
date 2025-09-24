/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.controller.dto;

/**
 * 通用上传响应实体。
 */
public class UploadResponse {

    private boolean success;
    private String message;
    private String url;
    private String filename;

    public UploadResponse() {
    }

    public static UploadResponse ok(String message, String url, String filename) {
        UploadResponse r = new UploadResponse();
        r.setSuccess(true);
        r.setMessage(message);
        r.setUrl(url);
        r.setFilename(filename);
        return r;
    }

    public static UploadResponse error(String message) {
        UploadResponse r = new UploadResponse();
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}


