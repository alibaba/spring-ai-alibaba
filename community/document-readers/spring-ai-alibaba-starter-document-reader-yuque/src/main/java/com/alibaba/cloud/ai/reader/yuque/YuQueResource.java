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
package com.alibaba.cloud.ai.reader.yuque;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author YunLong
 */
public class YuQueResource implements Resource {

	private static final String BASE_URL = "https://www.yuque.com";

	private static final String INFO_PATH = "/api/v2/hello";

	private static final String DOC_DETAIL_PATH = "/api/v2/repos/%s/%s/docs/%s";

	public static final String SOURCE = "source";

	public static final String SUPPORT_TYPE = "Doc";

	private final HttpClient httpClient;

	private final InputStream inputStream;

	private final URI uri;

	private final String resourcePath;

	private String groupLogin;

	private String bookSlug;

	private String id;

	public YuQueResource(String yuQueToken, String resourcePath) {

		this.resourcePath = resourcePath;

		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

		judgePathRule(resourcePath);
		judgeToken(yuQueToken);

		URI baseUri = URI.create(BASE_URL + DOC_DETAIL_PATH.formatted(groupLogin, bookSlug, id));

		HttpRequest httpRequest = HttpRequest.newBuilder()
			.header("X-Auth-Token", yuQueToken)
			.uri(baseUri)
			.GET()
			.build();

		try {
			HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			String body = response.body();
			// Parse the JSON response using Jackson
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonObject = objectMapper.readTree(body);
			JsonNode dataObject = jsonObject.get("data");

			if (dataObject == null || !dataObject.isObject()) {
				throw new RuntimeException("Invalid response format: 'data' is not an object");
			}

			if (!Objects.equals(dataObject.get("type").asText(), SUPPORT_TYPE)) {
				throw new RuntimeException("Unsupported resource type, only support " + SUPPORT_TYPE);
			}

			inputStream = new ByteArrayInputStream(dataObject.get("body_html").asText().getBytes());
			uri = URI.create(resourcePath);

		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Judge resource path rule Official online doc
	 * https://www.yuque.com/yuque/developer/openapi
	 * @param resourcePath
	 */
	private void judgePathRule(String resourcePath) {

		// Determine if the path conforms to this format： https://xx.xxx.com/aa/bb/cc
		String regex = "^https://[a-zA-Z0-9.-]+/([a-zA-Z0-9.-]+)/([a-zA-Z0-9.-]+)/([a-zA-Z0-9.-]+)$";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(resourcePath);
		Assert.isTrue(matcher.matches(), "Invalid resource path");

		// Extract the captured groups
		this.groupLogin = matcher.group(1);
		this.bookSlug = matcher.group(2);
		this.id = matcher.group(3);
		Assert.isTrue(StringUtils.hasText(this.groupLogin), "Invalid resource path");
		Assert.isTrue(StringUtils.hasText(this.bookSlug), "Invalid resource path");
		Assert.isTrue(StringUtils.hasText(this.id), "Invalid resource path");
	}

	/**
	 * judge yuQue token
	 * @param yuQueToken User/Team token
	 */
	private void judgeToken(String yuQueToken) {
		URI uri = URI.create(BASE_URL + INFO_PATH);

		HttpRequest httpRequest = HttpRequest.newBuilder().header("X-Auth-Token", yuQueToken).uri(uri).GET().build();

		try {
			HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			int statusCode = response.statusCode();
			Assert.isTrue(statusCode == 200, "Failed to auth YuQueToken");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String yuQueToken;

		private String resourcePath;

		public Builder yuQueToken(String yuQueToken) {
			this.yuQueToken = yuQueToken;
			return this;
		}

		public Builder resourcePath(String resourcePath) {
			this.resourcePath = resourcePath;
			return this;
		}

		public YuQueResource build() {
			Assert.notNull(yuQueToken, "YuQueToken must not be null");
			Assert.notNull(resourcePath, "ResourcePath must not be null");
			return new YuQueResource(yuQueToken, resourcePath);
		}

	}

	public String getResourcePath() {
		return resourcePath;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public URL getURL() throws IOException {
		return null;
	}

	@Override
	public URI getURI() throws IOException {
		return uri;
	}

	@Override
	public File getFile() throws IOException {
		return null;
	}

	@Override
	public long contentLength() throws IOException {
		return 0;
	}

	@Override
	public long lastModified() throws IOException {
		return 0;
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return null;
	}

	@Override
	public String getFilename() {
		return "";
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inputStream;
	}

}
