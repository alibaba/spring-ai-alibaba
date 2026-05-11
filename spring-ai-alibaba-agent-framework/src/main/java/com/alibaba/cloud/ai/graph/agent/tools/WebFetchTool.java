/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.StringUtils;

/**
 * Web fetch tool that retrieves content from URLs and processes it using an AI model for
 * summarization.
 *
 * <p>
 * Features:
 * <ul>
 * <li>Fetches HTML content and converts it to Markdown</li>
 * <li>Includes a 15-minute cache for faster repeated access</li>
 * <li>Automatic content truncation with configurable limits</li>
 * <li>Retry on network errors and 5xx server errors</li>
 * </ul>
 *
 * @see <a href="https://mikhail.io/2025/10/claude-code-web-tools/">Reference</a>
 */
public class WebFetchTool implements BiFunction<WebFetchTool.Request, ToolContext, String> {

	private static final Logger logger = LoggerFactory.getLogger(WebFetchTool.class);

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
			+ "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

	private static final Duration CACHE_TTL = Duration.ofMinutes(15);

	private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([^;\\s]+)", Pattern.CASE_INSENSITIVE);

	private static final String FETCH_SUMMARIZE_PROMPT = """
			Web page content:
			---
			{content}
			---

			{userQuery}

			Provide a concise response based only on the content above. In your response:
			- Enforce a strict 125-character maximum for quotes from any source document. Open Source Software is ok as long as we respect the license.
			- Use quotation marks for exact language from articles; any language outside of the quotation should never be word-for-word the same.
			- You are not a lawyer and never comment on the legality of your own prompts and responses.
			- Never produce or reproduce exact song lyrics.
			""";

	// @formatter:off
	public static final String DEFAULT_TOOL_DESCRIPTION = """
		Fetches content from a specified URL and processes it using an AI model.

		Features:
		- Takes a URL and a prompt as input
		- Fetches the URL content using HTTP GET method
		- Converts HTML to markdown
		- Processes the content with the prompt using a small, fast model
		- Returns the model's response about the content
		- Includes a self-cleaning 15-minute cache for faster responses
		- Automatic retry on network errors and 5xx server errors

		Usage notes:
		- IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool instead.
		- The URL must be a fully-formed valid URL (e.g., https://example.com)
		- HTTP URLs will be automatically upgraded to HTTPS
		- Only HTTP GET requests are supported (read-only)
		- The prompt should describe what information you want to extract from the page
		- This tool is read-only and does not modify any files or send any data
		- Results may be summarized if the content is very large
		- Retries up to 2 times (configurable) on transient failures with exponential backoff
		""";
	// @formatter:on

	private final ChatClient chatClient;

	private final HttpClient httpClient;

	private final int maxContentLength;

	private final FlexmarkHtmlConverter htmlToMarkdownConverter;

	private final Map<String, CacheEntry> urlCache;

	private final int maxCacheSize;

	private final Object cacheLock = new Object();

	private final int maxRetries;

	private WebFetchTool(ChatClient chatClient, int maxContentLength, int maxCacheSize, int maxRetries) {
		this.chatClient = chatClient;
		this.maxContentLength = maxContentLength;
		this.maxCacheSize = maxCacheSize;
		this.maxRetries = maxRetries;
		this.httpClient = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.ALWAYS)
			.connectTimeout(DEFAULT_CONNECT_TIMEOUT)
			.build();
		this.htmlToMarkdownConverter = FlexmarkHtmlConverter.builder().build();
		this.urlCache = new ConcurrentHashMap<>();
	}

	@JsonClassDescription("Request to fetch and process web content")
	public record Request(
			@JsonProperty(required = true, value = "url")
			@JsonPropertyDescription("The URL to fetch content from")
			String url,

			@JsonProperty(required = true, value = "prompt")
			@JsonPropertyDescription("The prompt to run on the fetched content (e.g., 'Summarize the main points', 'Extract the key takeaways')")
			String prompt) {
	}

	@Override
	public String apply(Request request, ToolContext toolContext) {
		String url = request.url();
		String prompt = request.prompt();

		// Validate URL
		if (!StringUtils.hasText(url)) {
			return "Error: URL cannot be empty or null";
		}

		url = url.trim();

		// Validate URL format
		try {
			URI uri = URI.create(url);
			if (uri.getScheme() == null || uri.getHost() == null) {
				return "Error: Invalid URL format. Please provide a fully-formed URL (e.g., https://example.com)";
			}
		}
		catch (IllegalArgumentException e) {
			return "Error: Invalid URL format: " + e.getMessage();
		}

		// Upgrade HTTP to HTTPS if needed
		if (url.startsWith("http://")) {
			url = "https://" + url.substring(7);
		}

		// Check cache first
		String cacheKey = buildCacheKey(url, prompt);
		String content = getCachedContent(cacheKey);

		if (content != null) {
			logger.debug("Cache hit for URL: {} with prompt hash: {}", url, prompt.hashCode());
			return content;
		}

		logger.debug("Cache miss for URL: {} with prompt hash: {}", url, prompt.hashCode());

		// Fetch HTML content with retry logic
		String htmlContent;
		try {
			HttpResponse<String> response = fetchHtmlWithRetry(url);
			if (response.statusCode() >= 400) {
				return "Error: Failed to fetch URL. HTTP status code: " + response.statusCode();
			}
			htmlContent = response.body();
			if (htmlContent == null || htmlContent.isBlank()) {
				return "Error: Retrieved empty content from URL";
			}
		}
		catch (WebFetchException e) {
			logger.error("Failed to fetch URL: {}", url, e);
			return "Error fetching URL: " + e.getMessage();
		}

		// Convert HTML to Markdown
		String mdContent = this.htmlToMarkdownConverter.convert(htmlContent);
		mdContent = truncate(mdContent);

		// Summarize with AI
		String summary = summarize(mdContent, prompt);

		// Cache the content
		cacheContent(cacheKey, summary);

		return summary;
	}

	private String buildCacheKey(String url, String prompt) {
		return url + "::prompt::" + prompt.hashCode();
	}

	private HttpResponse<String> fetchHtmlWithRetry(String url) {
		int attempt = 0;
		Exception lastException = null;

		while (attempt <= this.maxRetries) {
			try {
				if (attempt > 0) {
					long backoffMs = (long) Math.pow(2, attempt - 1) * 1000;
					logger.debug("Retrying fetch for URL: {} (attempt {}/{}), waiting {}ms", url, attempt,
							this.maxRetries, backoffMs);
					Thread.sleep(backoffMs);
				}

				HttpResponse<String> response = fetchHtml(url);

				if (response.statusCode() >= 500 && response.statusCode() < 600) {
					lastException = new WebFetchException("Server error: HTTP " + response.statusCode(), null);
					logger.warn("Fetch attempt {} returned server error {} for URL: {}", attempt + 1,
							response.statusCode(), url);
					attempt++;
					continue;
				}

				return response;
			}
			catch (WebFetchException e) {
				lastException = e;
				if (e.getCause() instanceof InterruptedException) {
					throw e;
				}
				logger.warn("Fetch attempt {} failed for URL: {}: {}", attempt + 1, url, e.getMessage());
				attempt++;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new WebFetchException("Retry interrupted", e);
			}
		}

		if (lastException == null) {
			throw new WebFetchException("Failed after " + (this.maxRetries + 1) + " attempts", null);
		}
		else if (lastException instanceof WebFetchException) {
			throw new WebFetchException("Failed after " + (this.maxRetries + 1) + " attempts", lastException);
		}
		else {
			throw new WebFetchException(
					"Failed after " + (this.maxRetries + 1) + " attempts: " + lastException.getMessage(),
					lastException);
		}
	}

	private HttpResponse<String> fetchHtml(String url) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(DEFAULT_REQUEST_TIMEOUT)
			.header("User-Agent", USER_AGENT)
			.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
			.header("Accept-Language", "en-US,en;q=0.5")
			.GET()
			.build();

		try {
			HttpResponse<byte[]> byteResponse = this.httpClient.send(request,
					HttpResponse.BodyHandlers.ofByteArray());

			Charset charset = extractCharset(byteResponse).orElse(StandardCharsets.UTF_8);
			String body = new String(byteResponse.body(), charset);

			return new HttpResponse<>() {
				@Override
				public int statusCode() {
					return byteResponse.statusCode();
				}

				@Override
				public HttpRequest request() {
					return byteResponse.request();
				}

				@Override
				public Optional<HttpResponse<String>> previousResponse() {
					return Optional.empty();
				}

				@Override
				public java.net.http.HttpHeaders headers() {
					return byteResponse.headers();
				}

				@Override
				public String body() {
					return body;
				}

				@Override
				public Optional<javax.net.ssl.SSLSession> sslSession() {
					return byteResponse.sslSession();
				}

				@Override
				public URI uri() {
					return byteResponse.uri();
				}

				@Override
				public java.net.http.HttpClient.Version version() {
					return byteResponse.version();
				}
			};
		}
		catch (IOException e) {
			throw new WebFetchException("Network error while fetching URL: " + e.getMessage(), e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new WebFetchException("Request was interrupted", e);
		}
	}

	private Optional<Charset> extractCharset(HttpResponse<?> response) {
		return response.headers()
			.firstValue("Content-Type")
			.flatMap(contentType -> {
				Matcher matcher = CHARSET_PATTERN.matcher(contentType);
				if (matcher.find()) {
					String charsetName = matcher.group(1);
					try {
						return Optional.of(Charset.forName(charsetName));
					}
					catch (Exception e) {
						logger.warn("Unsupported charset '{}', falling back to UTF-8", charsetName);
						return Optional.empty();
					}
				}
				return Optional.empty();
			});
	}

	private String summarize(String content, String userQuery) {
		try {
			String response = this.chatClient.prompt()
				.user(u -> u.text(FETCH_SUMMARIZE_PROMPT).param("content", content).param("userQuery", userQuery))
				.call()
				.content();
			return response != null ? response : "Error: Received empty response from AI model";
		}
		catch (Exception e) {
			logger.error("Failed to summarize content", e);
			return "Error summarizing content: " + e.getMessage();
		}
	}

	private String truncate(String content) {
		if (content == null) {
			return "";
		}
		if (content.length() > this.maxContentLength) {
			logger.warn("Content too long ({} characters). Truncating to {} characters.", content.length(),
					this.maxContentLength);
			return content.substring(0, this.maxContentLength);
		}
		return content;
	}

	private String getCachedContent(String url) {
		CacheEntry entry = this.urlCache.get(url);
		if (entry != null && !entry.isExpired()) {
			return entry.content();
		}
		if (entry != null) {
			this.urlCache.remove(url);
		}
		return null;
	}

	private void cacheContent(String cacheKey, String content) {
		if (this.urlCache.size() > this.maxCacheSize) {
			synchronized (this.cacheLock) {
				if (this.urlCache.size() > this.maxCacheSize) {
					this.urlCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
					logger.debug("Cleaned up expired cache entries. Current cache size: {}", this.urlCache.size());
				}
			}
		}
		this.urlCache.put(cacheKey, new CacheEntry(content, System.currentTimeMillis()));
	}

	private record CacheEntry(String content, long timestamp) {

		boolean isExpired() {
			return System.currentTimeMillis() - timestamp > CACHE_TTL.toMillis();
		}

	}

	/**
	 * Custom exception for web fetch errors.
	 */
	public static class WebFetchException extends RuntimeException {

		public WebFetchException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	public static Builder builder(ChatClient chatClient) {
		return new Builder(chatClient);
	}

	public static class Builder {

		private final ChatClient chatClient;

		private int maxContentLength = 100_000;

		private int maxCacheSize = 100;

		private int maxRetries = 2;

		private String name = "web_fetch";

		private String description = DEFAULT_TOOL_DESCRIPTION;

		private Builder(ChatClient chatClient) {
			if (chatClient == null) {
				throw new IllegalArgumentException("ChatClient must not be null");
			}
			this.chatClient = chatClient;
		}

		public Builder maxContentLength(int maxContentLength) {
			if (maxContentLength <= 0) {
				throw new IllegalArgumentException("maxContentLength must be positive");
			}
			this.maxContentLength = maxContentLength;
			return this;
		}

		public Builder maxCacheSize(int maxCacheSize) {
			if (maxCacheSize <= 0) {
				throw new IllegalArgumentException("maxCacheSize must be positive");
			}
			this.maxCacheSize = maxCacheSize;
			return this;
		}

		public Builder maxRetries(int maxRetries) {
			if (maxRetries < 0) {
				throw new IllegalArgumentException("maxRetries must be non-negative");
			}
			this.maxRetries = maxRetries;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		public ToolCallback build() {
			return FunctionToolCallback.builder(this.name, buildWebFetchTool())
				.description(this.description)
				.inputType(Request.class)
				.build();
		}

		/**
		 * Builds the WebFetchTool instance directly (for testing).
		 */
		WebFetchTool buildWebFetchTool() {
			return new WebFetchTool(this.chatClient, this.maxContentLength, this.maxCacheSize, this.maxRetries);
		}

	}

}
