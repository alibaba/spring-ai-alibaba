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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.utils.InMemoryFileStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class HttpNode implements NodeAction {

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

	private static final int DEFAULT_MAX_RETRIES = 3;

	private static final long DEFAULT_MAX_RETRY_INTERVAL = 1000;

	private final WebClient webClient;

	private final HttpMethod method;

	private final String url;

	private final Map<String, String> headers;

	private final Map<String, String> queryParams;

	private final HttpRequestNodeBody body;

	private final AuthConfig authConfig;

	private final RetryConfig retryConfig;

	private final String outputKey;

	private HttpNode(Builder builder) {
		this.webClient = builder.webClient;
		this.method = builder.method;
		this.url = builder.url;
		this.headers = builder.headers;
		this.queryParams = builder.queryParams;
		this.body = builder.body;
		this.authConfig = builder.authConfig;
		this.retryConfig = builder.retryConfig;
		this.outputKey = builder.outputKey;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		try {
			String finalUrl = replaceVariables(url, state);
			Map<String, String> finalHeaders = replaceVariables(headers, state);
			Map<String, String> finalQueryParams = replaceVariables(queryParams, state);

			UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(finalUrl);
			finalQueryParams.forEach(uriBuilder::queryParam);
			URI finalUri = uriBuilder.build().toUri();

			WebClient.RequestBodySpec requestSpec = webClient.method(method)
				.uri(finalUri)
				.headers(headers -> headers.setAll(finalHeaders));

			applyAuth(requestSpec);
			initBody(body, requestSpec, state);

			Mono<ResponseEntity<byte[]>> responseMono = requestSpec
				.exchangeToMono((ClientResponse resp) -> resp.toEntity(byte[].class))
				.retryWhen(Retry.backoff(retryConfig.maxRetries, Duration.ofMillis(retryConfig.maxRetryInterval)));
			ResponseEntity<byte[]> responseEntity = responseMono.block();
			Map<String, Object> httpResponse = processResponse(responseEntity, state);

			Map<String, Object> updatedState = new HashMap<>();
			updatedState.put("messages", httpResponse);
			if (StringUtils.hasLength(this.outputKey)) {
				updatedState.put(this.outputKey, httpResponse);
			}
			return updatedState;
		}
		catch (WebClientResponseException e) {
			throw RunnableErrors.nodeInterrupt
				.exception(format("%s HTTP request failed: %s", this.outputKey, e.getStatusText()));
		}
		catch (RestClientException e) {
			throw RunnableErrors.nodeInterrupt
				.exception(format("%s HTTP request failed: %s", this.outputKey, e.getMessage()));
		}
	}

	private String replaceVariables(String template, OverAllState state) {
		Matcher matcher = VARIABLE_PATTERN.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String key = matcher.group(1);
			Object value = state.value(key).orElse("");
			matcher.appendReplacement(result, value.toString());
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private Map<String, String> replaceVariables(Map<String, String> map, OverAllState state) {
		Map<String, String> result = new HashMap<>();
		map.forEach((k, v) -> result.put(k, replaceVariables(v, state)));
		return result;
	}

	private void initBody(HttpRequestNodeBody body, WebClient.RequestBodySpec requestSpec, OverAllState state)
			throws GraphRunnerException {
		switch (body.getType()) {
			case NONE:
				break;
			case RAW_TEXT:
				if (body.getData().size() != 1) {
					throw RunnableErrors.nodeInterrupt.exception("RAW_TEXT body must contain exactly one item");
				}
				String rawText = replaceVariables(body.getData().get(0).getValue(), state);
				requestSpec.headers(h -> h.setContentType(MediaType.TEXT_PLAIN));
				requestSpec.bodyValue(rawText);
				break;
			case JSON:
				if (body.getData().size() != 1) {
					throw RunnableErrors.nodeInterrupt.exception("JSON body must contain exactly one item");
				}
				String jsonTemplate = replaceVariables(body.getData().get(0).getValue(), state);
				Object jsonObject;
				try {
					jsonObject = new ObjectMapper().readValue(jsonTemplate, Object.class);
				}
				catch (com.fasterxml.jackson.core.JsonProcessingException e) {
					throw RunnableErrors.nodeInterrupt.exception("Failed to parse JSON body: " + e.getMessage());
				}
				requestSpec.headers(h -> h.setContentType(MediaType.APPLICATION_JSON));
				requestSpec.bodyValue(jsonObject);
				break;
			case X_WWW_FORM_URLENCODED:
				MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
				for (BodyData item : body.getData()) {
					String key = replaceVariables(item.getKey(), state);
					String value = replaceVariables(item.getValue(), state);
					form.add(key, value);
				}
				requestSpec.headers(h -> h.setContentType(MediaType.APPLICATION_FORM_URLENCODED));
				requestSpec.body(BodyInserters.fromFormData(form));
				break;
			case FORM_DATA:
				MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
				for (BodyData item : body.getData()) {
					String key = replaceVariables(item.getKey(), state);
					if (item.getType() == BodyType.BINARY) {
						ByteArrayResource resource = new ByteArrayResource(item.getFileBytes()) {
							@Override
							public String getFilename() {
								return item.getFilename();
							}
						};
						multipart.add(key, resource);
					}
					else {
						String value = replaceVariables(item.getValue(), state);
						multipart.add(key, value);
					}
				}
				requestSpec.headers(h -> h.setContentType(MediaType.MULTIPART_FORM_DATA));
				requestSpec.body(BodyInserters.fromMultipartData(multipart));
				break;
			case BINARY:
				if (body.getData().size() != 1) {
					throw RunnableErrors.nodeInterrupt.exception("BINARY body must contain exactly one item");
				}
				BodyData fileItem = body.getData().get(0);
				ByteArrayResource resource = new ByteArrayResource(fileItem.getFileBytes()) {
					@Override
					public String getFilename() {
						return fileItem.getFilename();
					}
				};
				MediaType mediaType = StringUtils.hasText(fileItem.getMimeType())
						? MediaType.parseMediaType(fileItem.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM;
				requestSpec.headers(h -> h.setContentType(mediaType));
				requestSpec.body(BodyInserters.fromResource(resource));
				break;
			default:
				throw RunnableErrors.nodeInterrupt.exception("Unsupported body type: " + body.getType());
		}
	}

	private void applyAuth(WebClient.RequestBodySpec requestSpec) {
		if (authConfig != null) {
			switch (authConfig.type) {
				case BASIC:
					requestSpec.headers(headers -> headers.setBasicAuth(authConfig.username, authConfig.password));
					break;
				case BEARER:
					requestSpec.headers(headers -> headers.setBearerAuth(authConfig.token));
					break;
			}
		}
	}

	private Map<String, Object> processResponse(ResponseEntity<byte[]> responseEntity, OverAllState state) {
		Map<String, Object> result = new HashMap<>();
		result.put("status", responseEntity.getStatusCodeValue());
		result.put("headers", responseEntity.getHeaders());
		byte[] body = responseEntity.getBody();
		if (body == null) {
			return result;
		}
		if (isFileResponse(responseEntity)) {
			String filename = extractFilename(responseEntity.getHeaders());
			String mimeType = Optional.ofNullable(responseEntity.getHeaders().getContentType())
				.map(MediaType::toString)
				.orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

			InMemoryFileStorage.FileRecord record = InMemoryFileStorage.save(body, mimeType, filename);

			result.put("files", Collections.singletonList(record.getId()));
		}
		else {
			String text = new String(body, StandardCharsets.UTF_8);
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> map = objectMapper.readValue(text, Map.class);
				result.put("body", map);
			}
			catch (Exception ex) {
				result.put("body", text);
			}
		}
		return result;
	}

	private String extractFilename(HttpHeaders headers) {
		if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			String cd = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
			if (cd != null && cd.contains("filename=")) {
				return cd.split("filename=")[1].replace("\"", "");
			}
		}
		return UUID.randomUUID().toString();
	}

	private boolean isFileResponse(ResponseEntity<?> response) {
		HttpHeaders headers = response.getHeaders();
		String contentType = Optional.ofNullable(headers.getContentType()).map(MediaType::toString).orElse("");
		if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			String cd = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
			if (cd != null && (cd.startsWith("attachment") || cd.contains("filename="))) {
				return true;
			}
		}
		if (contentType.startsWith("text/")) {
			return false;
		}
		List<String> textTypes = List.of("json", "xml", "javascript", "x-www-form-urlencoded", "yaml");
		if (contentType.startsWith("application/")) {
			for (String tt : textTypes) {
				if (contentType.contains(tt)) {
					return false;
				}
			}
			try {
				new String((byte[]) Objects.requireNonNull(response.getBody()), StandardCharsets.UTF_8);
				return false;
			}
			catch (Exception ex) {
				return true;
			}
		}
		return contentType.startsWith("image/") || contentType.startsWith("audio/") || contentType.startsWith("video/");
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private WebClient webClient = WebClient.create();

		private HttpMethod method = HttpMethod.GET;

		private String url;

		private Map<String, String> headers = new HashMap<>();

		private Map<String, String> queryParams = new HashMap<>();

		private HttpRequestNodeBody body = new HttpRequestNodeBody();

		private AuthConfig authConfig;

		private RetryConfig retryConfig = new RetryConfig(DEFAULT_MAX_RETRIES, DEFAULT_MAX_RETRY_INTERVAL, true);

		private String outputKey;

		public Builder webClient(WebClient webClient) {
			this.webClient = webClient;
			return this;
		}

		public Builder method(HttpMethod method) {
			this.method = method;
			return this;
		}

		public Builder url(String url) {
			this.url = url;
			return this;
		}

		public Builder header(String name, String value) {
			this.headers.put(name, value);
			return this;
		}

		public Builder queryParam(String name, String value) {
			this.queryParams.put(name, value);
			return this;
		}

		public Builder body(HttpRequestNodeBody body) {
			this.body = body;
			return this;
		}

		public Builder auth(AuthConfig authConfig) {
			this.authConfig = authConfig;
			return this;
		}

		public Builder retryConfig(RetryConfig retryConfig) {
			this.retryConfig = retryConfig;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public HttpNode build() {
			return new HttpNode(this);
		}

	}

	public static class HttpRequestNodeBody {

		private BodyType type;

		private List<BodyData> data = new ArrayList<>();

		public HttpRequestNodeBody() {
			this.type = BodyType.NONE;
		}

		public HttpRequestNodeBody(BodyType type, List<BodyData> data) {
			this.type = type;
			this.data = data != null ? data : List.of();
		}

		public static HttpRequestNodeBody from(Object raw) throws JsonProcessingException {
			if (raw == null) {
				return new HttpRequestNodeBody(BodyType.NONE, null);
			}
			if (raw instanceof String) {
				String text = ((String) raw).trim();
				if (text.isEmpty()) {
					return new HttpRequestNodeBody(BodyType.NONE, null);
				}
				BodyData bd = new BodyData();
				bd.setType(BodyType.RAW_TEXT);
				bd.setValue(text);
				return new HttpRequestNodeBody(BodyType.RAW_TEXT, List.of(bd));
			}
			if (raw instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) raw;
				Object t = m.get("type");
				String typeStr = (t instanceof String) ? ((String) t).trim().toUpperCase().replace("-", "_") : "NONE";
				BodyType type;
				try {
					type = BodyType.valueOf(typeStr);
				}
				catch (Exception ex) {
					type = BodyType.NONE;
				}

				Object dataField = m.get("data");

				switch (type) {
					case NONE:
						return new HttpRequestNodeBody(BodyType.NONE, null);

					case RAW_TEXT:
						if (dataField instanceof String) {
							String txt = ((String) dataField).trim();
							if (!txt.isEmpty()) {
								BodyData bd1 = new BodyData();
								bd1.setType(BodyType.RAW_TEXT);
								bd1.setValue(txt);
								return new HttpRequestNodeBody(BodyType.RAW_TEXT, List.of(bd1));
							}
						}
						return new HttpRequestNodeBody(BodyType.NONE, null);

					case JSON:
						if (dataField instanceof Map<?, ?> || dataField instanceof List<?>) {
							ObjectMapper objectMapper = new ObjectMapper();
							String jsonString = objectMapper.writeValueAsString(dataField);
							BodyData bd2 = new BodyData();
							bd2.setType(BodyType.JSON);
							bd2.setValue(jsonString);
							return new HttpRequestNodeBody(BodyType.JSON, List.of(bd2));
						}
						return new HttpRequestNodeBody(BodyType.NONE, null);

					case FORM_DATA:
						if (dataField instanceof List<?>) {
							@SuppressWarnings("unchecked")
							List<Map<String, Object>> rawList = (List<Map<String, Object>>) dataField;
							List<BodyData> listData = new ArrayList<>();
							for (Map<String, Object> item : rawList) {
								BodyData bd3 = new BodyData();
								Object key0 = item.get("key");
								if (key0 instanceof String) {
									bd3.setKey((String) key0);
								}
								Object type0 = item.get("type");
								if (type0 instanceof String) {
									try {
										bd3.setType(BodyType.valueOf(((String) type0).toUpperCase()));
									}
									catch (Exception ex) {
										bd3.setType(BodyType.NONE);
									}
								}
								Object val0 = item.get("value");
								if (val0 instanceof String) {
									bd3.setValue((String) val0);
								}
								Object fileBytes = item.get("fileBytes");
								if (fileBytes instanceof byte[]) {
									bd3.setFileBytes((byte[]) fileBytes);
								}
								Object filename = item.get("filename");
								if (filename instanceof String) {
									bd3.setFilename((String) filename);
								}
								Object mimeType = item.get("mimeType");
								if (mimeType instanceof String) {
									bd3.setMimeType((String) mimeType);
								}
								listData.add(bd3);
							}
							return new HttpRequestNodeBody(BodyType.FORM_DATA, listData);
						}
						return new HttpRequestNodeBody(BodyType.NONE, null);

					case X_WWW_FORM_URLENCODED:
						if (dataField instanceof List<?>) {
							@SuppressWarnings("unchecked")
							List<Map<String, Object>> rawList2 = (List<Map<String, Object>>) dataField;
							List<BodyData> list2 = new ArrayList<>();
							for (Map<String, Object> item : rawList2) {
								BodyData bd4 = new BodyData();
								Object key0 = item.get("key");
								if (key0 instanceof String) {
									bd4.setKey((String) key0);
								}
								bd4.setType(BodyType.X_WWW_FORM_URLENCODED);
								Object val0 = item.get("value");
								if (val0 instanceof String) {
									bd4.setValue((String) val0);
								}
								list2.add(bd4);
							}
							return new HttpRequestNodeBody(BodyType.X_WWW_FORM_URLENCODED, list2);
						}
						return new HttpRequestNodeBody(BodyType.NONE, null);

					case BINARY:
						if (dataField instanceof List<?>) {
							@SuppressWarnings("unchecked")
							List<Map<String, Object>> rawList3 = (List<Map<String, Object>>) dataField;
							List<BodyData> list3 = new ArrayList<>();
							for (Map<String, Object> item : rawList3) {
								BodyData bd5 = new BodyData();
								bd5.setType(BodyType.BINARY);
								Object fb = item.get("fileBytes");
								if (fb instanceof byte[]) {
									bd5.setFileBytes((byte[]) fb);
								}
								Object fn = item.get("filename");
								if (fn instanceof String) {
									bd5.setFilename((String) fn);
								}
								Object mt = item.get("mimeType");
								if (mt instanceof String) {
									bd5.setMimeType((String) mt);
								}
								list3.add(bd5);
							}
							return new HttpRequestNodeBody(BodyType.BINARY, list3);
						}
						return new HttpRequestNodeBody(BodyType.NONE, null);

					default:
						return new HttpRequestNodeBody(BodyType.NONE, null);
				}
			}
			throw new IllegalArgumentException("Unsupported body type: " + raw.getClass());
		}

		public BodyType getType() {
			return type;
		}

		public void setType(BodyType type) {
			this.type = type;
		}

		public List<BodyData> getData() {
			return data;
		}

		public void setData(List<BodyData> data) {
			this.data = data;
		}

		public boolean hasContent() {
			return this.type != null && this.type != BodyType.NONE && this.data != null && !this.data.isEmpty();
		}

	}

	public static class AuthConfig {

		enum AuthType {

			BASIC, BEARER

		}

		final AuthType type;

		String username;

		String password;

		String token;

		public static AuthConfig basic(String username, String password) {
			AuthConfig config = new AuthConfig(AuthType.BASIC);
			config.username = username;
			config.password = password;
			return config;
		}

		public static AuthConfig bearer(String token) {
			AuthConfig config = new AuthConfig(AuthType.BEARER);
			config.token = token;
			return config;
		}

		private AuthConfig(AuthType type) {
			this.type = type;
		}

		public AuthType getType() {
			return type;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		public String getToken() {
			return token;
		}

		public String getTypeName() {
			return this.type.name().toLowerCase();
		}

		public boolean isBasic() {
			return this.type == AuthType.BASIC;
		}

		public boolean isBearer() {
			return this.type == AuthType.BEARER;
		}

	}

	public static class RetryConfig {

		final int maxRetries;

		final long maxRetryInterval;

		final boolean enable;

		public RetryConfig(int maxRetries, long maxRetryInterval, boolean enable) {
			this.maxRetries = maxRetries > 0 ? maxRetries : DEFAULT_MAX_RETRIES;
			this.maxRetryInterval = maxRetryInterval > 0 ? maxRetryInterval : DEFAULT_MAX_RETRY_INTERVAL;
			this.enable = enable;
		}

		public int getMaxRetries() {
			return maxRetries;
		}

		public long getMaxRetryInterval() {
			return maxRetryInterval;
		}

		public boolean isEnable() {
			return enable;
		}

	}

	public record TimeoutConfig(int connect, int read, int write, int maxConnectTimeout, int maxReadTimeout,
			int maxWriteTimeout) {

	}

	public enum BodyType {

		NONE, FORM_DATA, X_WWW_FORM_URLENCODED, RAW_TEXT, JSON, BINARY;

		public static BodyType from(String s) {
			return BodyType.valueOf(s.toUpperCase().replace("-", "_"));
		}

	}

	public static class BodyData {

		private String key;

		private BodyType type;

		private String value;

		private byte[] fileBytes;

		private String filename;

		private String mimeType;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public BodyType getType() {
			return type;
		}

		public void setType(BodyType type) {
			this.type = type;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public byte[] getFileBytes() {
			return fileBytes;
		}

		public void setFileBytes(byte[] fileBytes) {
			this.fileBytes = fileBytes;
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

	}

}
