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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

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

	private JsonNode metadata;

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
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode pageJson = objectMapper.readTree(pageResponse.body());
			StringBuilder content = new StringBuilder();

			// Extract page title
			JsonNode properties = pageJson.get("properties");
			if (properties != null && properties.has("title")) {
				JsonNode titleProp = properties.get("title");
				JsonNode titleArray = titleProp.get("title");
				if (titleArray != null && titleArray.isArray()) {
					for (JsonNode titleNode : titleArray) {
						content.append(titleNode.get("plain_text").asText());
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
			JsonNode blocksJson = objectMapper.readTree(blocksResponse.body());
			JsonNode blocks = blocksJson.get("results");

			// 5. Extract text content
			if (blocks != null && blocks.isArray()) {
				for (JsonNode block : blocks) {
					String type = block.get("type").asText();
					if (block.has(type)) {
						JsonNode typeObj = block.get(type);
						if (typeObj.has("rich_text")) {
							JsonNode richText = typeObj.get("rich_text");
							for (JsonNode textNode : richText) {
								content.append(textNode.get("plain_text").asText());
							}
							content.append("\n");
						}
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
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonResponse = objectMapper.readTree(response.body());
			JsonNode results = jsonResponse.get("results");

			// 3. Extract property values
			StringBuilder content = new StringBuilder();
			if (results != null && results.isArray()) {
				for (JsonNode row : results) {
					JsonNode properties = row.get("properties");

					for (Iterator<String> it = properties.fieldNames(); it.hasNext();) {
						String propertyName = it.next();
						JsonNode property = properties.get(propertyName);
						String type = property.get("type").asText();

						if (property.has(type)) {
							JsonNode value = property.get(type);
							if (value.isArray()) {
								for (JsonNode item : value) {
									if (item.has("plain_text")) {
										content.append(propertyName)
											.append(": ")
											.append(item.get("plain_text").asText())
											.append("\n");
									}
								}
							}
						}
					}
					content.append("---\n");
				}
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
	private JsonNode getResourceMetadata(String notionToken, String resourceId, ResourceType resourceType) {
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

			ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.readTree(response.body());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get resource metadata", e);
		}
	}

	/**
	 * Get resource metadata
	 */
	public JsonNode getMetadata() {
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
