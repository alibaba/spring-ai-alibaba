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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class WebFetchToolRequest {

    @JsonProperty(required = true, value = "url")
    @JsonPropertyDescription("Target URL to fetch (http/https)")
    public String url;

    @JsonProperty("format")
    @JsonPropertyDescription("Response content format: text|html|markdown, default text")
    public String format;

    @JsonProperty("timeoutMs")
    @JsonPropertyDescription("Request timeout in milliseconds, default 15000")
    public Integer timeoutMs;

    @JsonProperty("maxBytes")
    @JsonPropertyDescription("Maximum bytes to read from response body, default 500000")
    public Integer maxBytes;

    @JsonProperty("followRedirects")
    @JsonPropertyDescription("Whether to follow HTTP redirects, default false")
    public Boolean followRedirects;

    @JsonProperty("maxRedirects")
    @JsonPropertyDescription("Maximum redirects if followRedirects=true, default 5")
    public Integer maxRedirects;

    public WebFetchToolRequest() {
    }

    public WebFetchToolRequest(String url, String format, Integer timeoutMs, Integer maxBytes, Boolean followRedirects,
            Integer maxRedirects) {
        this.url = url;
        this.format = format;
        this.timeoutMs = timeoutMs;
        this.maxBytes = maxBytes;
        this.followRedirects = followRedirects;
        this.maxRedirects = maxRedirects;
    }
}
