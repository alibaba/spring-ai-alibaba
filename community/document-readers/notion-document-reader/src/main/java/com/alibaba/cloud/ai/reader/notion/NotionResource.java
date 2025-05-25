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
package com.alibaba.cloud.ai.reader.notion;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

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

/**
 * Notion Resource class Supports accessing Notion pages and databases
 *
 * @author xiadong
 * @since 2024-01-06
 */
public class NotionResource implements Resource {

	public static final String SOURCE = "source";

	private static final String BASE_URL = "https://api.notion.com/v1";

	private static final String API_VERSION = "2022-06-28";

	// Resource types
	public enum ResourceType {

		PAGE, DATABASE

	}

	private final HttpClient httpClient;

	private final InputStream inputStream;

	private final URI uri;

	private final ResourceType resourceType;

	private final String resourceId;

	private JSONObject metadata;

	/**
	 * Constructor
	 * @param notionToken Notion API Token
	 * @param resourceId Notion resource ID
	 * @param resourceType Resource type (PAGE or DATABASE)
	 */
	public NotionResource(String notionToken, String resourceId, ResourceType resourceType) {
		Assert.hasText(resourceId, "ResourceId must not be empty");
		Assert.notNull(resourceType, "ResourceType must not be null");

		this.resourceId = resourceId;
		this.resourceType = resourceType;
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();

		validateToken(notionToken);

		// Get resource metadata
		this.metadata = getResourceMetadata(notionToken, resourceId, resourceType);

		// Get content based on resource type
		String content = switch (resourceType) {
			case PAGE -> getPageContent(notionToken, resourceId);
			case DATABASE -> getDatabaseContent(notionToken, resourceId);
		};

		this.inputStream = new ByteArrayInputStream(content.getBytes());
		this.uri = URI.create(String.format("notion://%s/%s", resourceType.name().toLowerCase(), resourceId));
	}

	/**
	 * Validate Notion API token
	 */
	private void validateToken(String notionToken) {
		URI uri = URI.create(BASE_URL + "/users/me");
		HttpRequest request = HttpRequest.newBuilder()
			.header("Authorization", "Bearer " + notionToken)
			.header("Notion-Version", API_VERSION)
			.uri(uri)
			.GET()
			.build();

		try {
			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			Assert.isTrue(response.statusCode() == 200, "Failed to authenticate Notion token");
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to validate Notion token", e);
		}
	}

	/**
	 * Get page content
	 */
	private String getPageContent(String notionToken, String pageId) {
		try {
			// 1. Get page content
			URI pageUri = URI.create(BASE_URL + "/pages/" + pageId);
			HttpRequest pageRequest = HttpRequest.newBuilder()
				.header("Authorization", "Bearer " + notionToken)
				.header("Notion-Version", API_VERSION)
				.uri(pageUri)
				.GET()
				.build();

			HttpResponse<String> pageResponse = this.httpClient.send(pageRequest, HttpResponse.BodyHandlers.ofString());
			Assert.isTrue(pageResponse.statusCode() == 200, "Failed to fetch page content");

			// 2. Parse page content
			JSONObject pageJson = JSON.parseObject(pageResponse.body());
			StringBuilder content = new StringBuilder();

			// Extract page title
			JSONObject properties = pageJson.getJSONObject("properties");
			if (properties != null && properties.containsKey("title")) {
				JSONObject titleProp = properties.getJSONObject("title");
				JSONArray titleArray = titleProp.getJSONArray("title");
				if (titleArray != null && !titleArray.isEmpty()) {
					for (int i = 0; i < titleArray.size(); i++) {
						content.append(titleArray.getJSONObject(i).getString("plain_text"));
					}
					content.append("\n\n");
				}
			}

			// 3. Get page blocks
			URI blocksUri = URI.create(BASE_URL + "/blocks/" + pageId + "/children");
			HttpRequest blocksRequest = HttpRequest.newBuilder()
				.header("Authorization", "Bearer " + notionToken)
				.header("Notion-Version", API_VERSION)
				.uri(blocksUri)
				.GET()
				.build();

			HttpResponse<String> blocksResponse = this.httpClient.send(blocksRequest,
					HttpResponse.BodyHandlers.ofString());
			Assert.isTrue(blocksResponse.statusCode() == 200, "Failed to fetch page blocks");

			// 4. Parse block content
			JSONObject blocksJson = JSON.parseObject(blocksResponse.body());
			JSONArray blocks = blocksJson.getJSONArray("results");

			// 5. Extract text content
			for (int i = 0; i < blocks.size(); i++) {
				JSONObject block = blocks.getJSONObject(i);
				String type = block.getString("type");
				if (block.containsKey(type)) {
					JSONObject typeObj = block.getJSONObject(type);
					if (typeObj.containsKey("rich_text")) {
						JSONArray richText = typeObj.getJSONArray("rich_text");
						for (int j = 0; j < richText.size(); j++) {
							content.append(richText.getJSONObject(j).getString("plain_text"));
						}
						content.append("\n");
					}
				}
			}
			return content.toString();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get page content", e);
		}
	}

	/**
	 * Get database content
	 */
	private String getDatabaseContent(String notionToken, String databaseId) {
		try {
			// 1. Query database
			URI uri = URI.create(BASE_URL + "/databases/" + databaseId + "/query");
			HttpRequest request = HttpRequest.newBuilder()
				.header("Authorization", "Bearer " + notionToken)
				.header("Notion-Version", API_VERSION)
				.header("Content-Type", "application/json")
				.uri(uri)
				.POST(HttpRequest.BodyPublishers.ofString("{}"))
				.build();

			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			Assert.isTrue(response.statusCode() == 200, "Failed to fetch database content");

			// 2. Parse database content
			JSONObject jsonResponse = JSON.parseObject(response.body());
			JSONArray results = jsonResponse.getJSONArray("results");

			// 3. Extract property values
			StringBuilder content = new StringBuilder();
			for (int i = 0; i < results.size(); i++) {
				JSONObject row = results.getJSONObject(i);
				JSONObject properties = row.getJSONObject("properties");

				for (String propertyName : properties.keySet()) {
					JSONObject property = properties.getJSONObject(propertyName);
					String type = property.getString("type");

					if (property.containsKey(type)) {
						Object value = property.get(type);
						if (value instanceof JSONArray) {
							JSONArray array = (JSONArray) value;
							for (int j = 0; j < array.size(); j++) {
								JSONObject item = array.getJSONObject(j);
								if (item.containsKey("plain_text")) {
									content.append(propertyName)
										.append(": ")
										.append(item.getString("plain_text"))
										.append("\n");
								}
							}
						}
					}
				}
				content.append("---\n");
			}
			return content.toString();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get database content", e);
		}
	}

	/**
	 * Get resource metadata
	 */
	private JSONObject getResourceMetadata(String notionToken, String resourceId, ResourceType resourceType) {
		try {
			String endpoint = switch (resourceType) {
				case PAGE -> "/pages/";
				case DATABASE -> "/databases/";
			};

			URI uri = URI.create(BASE_URL + endpoint + resourceId);
			HttpRequest request = HttpRequest.newBuilder()
				.header("Authorization", "Bearer " + notionToken)
				.header("Notion-Version", API_VERSION)
				.uri(uri)
				.GET()
				.build();

			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			Assert.isTrue(response.statusCode() == 200, "Failed to fetch resource metadata");

			return JSON.parseObject(response.body());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get resource metadata", e);
		}
	}

	/**
	 * Get resource metadata
	 */
	public JSONObject getMetadata() {
		return metadata;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String notionToken;

		private String resourceId;

		private ResourceType resourceType;

		public Builder notionToken(String notionToken) {
			this.notionToken = notionToken;
			return this;
		}

		public Builder resourceId(String resourceId) {
			this.resourceId = resourceId;
			return this;
		}

		public Builder resourceType(ResourceType resourceType) {
			this.resourceType = resourceType;
			return this;
		}

		public NotionResource build() {
			Assert.notNull(notionToken, "NotionToken must not be null");
			Assert.notNull(resourceId, "ResourceId must not be null");
			Assert.notNull(resourceType, "ResourceType must not be null");
			return new NotionResource(notionToken, resourceId, resourceType);
		}

	}

	@Override
	public boolean exists() {
		return true;
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
		return resourceId;
	}

	@Override
	public String getDescription() {
		return String.format("Notion %s resource [id=%s]", resourceType, resourceId);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inputStream;
	}

	public ResourceType getResourceType() {
		return resourceType;
	}

	public String getResourceId() {
		return resourceId;
	}

	public String getSource() {
		return uri.toString();
	}

}