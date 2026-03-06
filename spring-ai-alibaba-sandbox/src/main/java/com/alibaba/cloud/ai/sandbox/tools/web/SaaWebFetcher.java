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

import com.alibaba.cloud.ai.sandbox.RuntimeFunctionToolCallback;
import com.alibaba.cloud.ai.sandbox.SandboxAwareTool;
import io.agentscope.runtime.sandbox.box.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

public class SaaWebFetcher implements SandboxAwareTool<WebFetchToolRequest, WebFetchToolResponse> {

    private static final Logger logger = LoggerFactory.getLogger(SaaWebFetcher.class);

    private static final String TOOL_NAME = "web_fetch";

    private static final String TOOL_DESCRIPTION = "Fetch web page content with timeout, size limit and optional redirect following.";

    private static final int DEFAULT_TIMEOUT_MS = 15_000;

    private static final int DEFAULT_MAX_BYTES = 500_000;

    private static final int DEFAULT_MAX_REDIRECTS = 5;

    private Sandbox sandbox;

    @Override
    public WebFetchToolResponse apply(WebFetchToolRequest request, ToolContext toolContext) {
        Instant startedAt = Instant.now();

        String targetUrl = request.url == null ? "" : request.url.trim();
        if (targetUrl.isEmpty()) {
            return failure("INVALID_URL", "url cannot be empty", startedAt);
        }

        URI targetUri;
        try {
            targetUri = URI.create(targetUrl);
        }
        catch (Exception ex) {
            return failure("INVALID_URL", "url is not a valid URI", startedAt);
        }

        String scheme = targetUri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            return failure("INVALID_URL", "only http/https URLs are supported", startedAt);
        }

        String format = normalizeFormat(request.format);
        int timeoutMs = normalizePositiveInt(request.timeoutMs, DEFAULT_TIMEOUT_MS);
        int maxBytes = normalizePositiveInt(request.maxBytes, DEFAULT_MAX_BYTES);
        boolean followRedirects = request.followRedirects != null && request.followRedirects;
        int maxRedirects = normalizePositiveInt(request.maxRedirects, DEFAULT_MAX_REDIRECTS);

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();

        URI currentUri = targetUri;
        int redirectCount = 0;

        try {
            while (true) {
                HttpRequest requestEntity = HttpRequest.newBuilder(currentUri)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = httpClient.send(requestEntity, HttpResponse.BodyHandlers.ofByteArray());
                int statusCode = response.statusCode();

                if (isRedirect(statusCode)) {
                    if (!followRedirects) {
                        return failure(statusCode, currentUri.toString(), "REDIRECT_BLOCKED",
                                "Received redirect but followRedirects=false", startedAt);
                    }
                    if (redirectCount >= maxRedirects) {
                        return failure(statusCode, currentUri.toString(), "TOO_MANY_REDIRECTS",
                                "Redirect count exceeded maxRedirects=" + maxRedirects, startedAt);
                    }

                    String location = response.headers().firstValue("location").orElse(null);
                    if (location == null || location.isBlank()) {
                        return failure(statusCode, currentUri.toString(), "INVALID_REDIRECT",
                                "Redirect response missing location header", startedAt);
                    }

                    currentUri = currentUri.resolve(location);
                    redirectCount++;
                    continue;
                }

                String contentType = response.headers().firstValue("content-type").orElse("");
                if (!isTextLike(contentType)) {
                    return failure(statusCode, currentUri.toString(), "UNSUPPORTED_CONTENT_TYPE",
                            "content-type is not text-like: " + contentType, startedAt);
                }

                byte[] bodyBytes = response.body() == null ? new byte[0] : response.body();
                boolean truncated = bodyBytes.length > maxBytes;
                byte[] selectedBytes = truncated ? Arrays.copyOf(bodyBytes, maxBytes) : bodyBytes;
                String body = new String(selectedBytes, StandardCharsets.UTF_8);
                String content = transformContent(body, format);

                long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();

                return new WebFetchToolResponse(new WebFetchResponsePayload(statusCode, currentUri.toString(), contentType, content,
                        bodyBytes.length, elapsedMs, truncated, null, null, "Web fetch completed"));
            }
        }
        catch (java.net.http.HttpTimeoutException ex) {
            return failure("READ_TIMEOUT", ex.getMessage(), startedAt);
        }
        catch (Exception ex) {
            logger.error("web_fetch failed for url {}: {}", targetUrl, ex.getMessage());
            return failure("FETCH_FAILED", ex.getMessage(), startedAt);
        }
    }

    private WebFetchToolResponse failure(String errorCode, String errorMessage, Instant startedAt) {
        long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
        return new WebFetchToolResponse(new WebFetchResponsePayload(null, null, null, null, 0, elapsedMs, false, errorCode,
                errorMessage, "Web fetch failed"));
    }

    private WebFetchToolResponse failure(Integer statusCode, String finalUrl, String errorCode, String errorMessage,
            Instant startedAt) {
        long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
        return new WebFetchToolResponse(new WebFetchResponsePayload(statusCode, finalUrl, null, null, 0, elapsedMs, false, errorCode,
                errorMessage, "Web fetch failed"));
    }

    private static int normalizePositiveInt(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private static String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "text";
        }
        String normalized = format.toLowerCase(Locale.ROOT);
        if ("text".equals(normalized) || "html".equals(normalized) || "markdown".equals(normalized)) {
            return normalized;
        }
        return "text";
    }

    private static boolean isRedirect(int status) {
        return status >= 300 && status < 400;
    }

    private static boolean isTextLike(String contentType) {
        String lower = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return lower.startsWith("text/") || lower.contains("json") || lower.contains("xml")
                || lower.contains("javascript") || lower.contains("x-www-form-urlencoded");
    }

    private static String transformContent(String body, String format) {
        if ("html".equals(format)) {
            return body;
        }
        if ("markdown".equals(format)) {
            return body;
        }
        return body;
    }

    @Override
    public Sandbox getSandbox() {
        return this.sandbox;
    }

    @Override
    public void setSandbox(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    @Override
    public Class<?> getSandboxClass() {
        return Sandbox.class;
    }

	public RuntimeFunctionToolCallback<?, ?> buildTool() {
        return RuntimeFunctionToolCallback.builder(TOOL_NAME, this)
                .description(TOOL_DESCRIPTION)
                .inputType(WebFetchToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

}
