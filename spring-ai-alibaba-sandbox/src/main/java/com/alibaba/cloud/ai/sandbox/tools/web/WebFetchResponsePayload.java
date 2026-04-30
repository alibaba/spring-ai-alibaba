/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.sandbox.tools.web;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonClassDescription("The result contains web fetch output and execution message")
public class WebFetchResponsePayload {

    @JsonProperty("status")
    public Integer status;

    @JsonProperty("finalUrl")
    public String finalUrl;

    @JsonProperty("contentType")
    public String contentType;

    @JsonProperty("content")
    public String content;

    @JsonProperty("bytes")
    public Integer bytes;

    @JsonProperty("elapsedMs")
    public Long elapsedMs;

    @JsonProperty("truncated")
    public Boolean truncated;

    @JsonProperty("errorCode")
    public String errorCode;

    @JsonProperty("errorMessage")
    public String errorMessage;

    @JsonProperty("message")
    public String message;

    public WebFetchResponsePayload() {
    }

    public WebFetchResponsePayload(Integer status, String finalUrl, String contentType, String content, Integer bytes,
            Long elapsedMs, Boolean truncated, String errorCode, String errorMessage, String message) {
        this.status = status;
        this.finalUrl = finalUrl;
        this.contentType = contentType;
        this.content = content;
        this.bytes = bytes;
        this.elapsedMs = elapsedMs;
        this.truncated = truncated;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.message = message;
    }
}
