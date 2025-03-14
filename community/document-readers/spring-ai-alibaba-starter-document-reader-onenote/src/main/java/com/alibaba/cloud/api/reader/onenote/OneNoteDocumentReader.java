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
package com.alibaba.cloud.api.reader.onenote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;

/**
 * @author sparkle6979l
 */
public class OneNoteDocumentReader implements DocumentReader {

	public static final String MICROSOFT_GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0";

	public static final String NOTEBOOK_ID_FILTER_PREFIX = "/me/onenote/pages/?$expand=parentNotebook&$filter=parentNotebook/id";

	public static final String SECTION_ID_FILTER_PREFIX = "/me/onenote/pages/?$expand=parentSection&$filter=parentSection/id";

	private static final Logger log = LoggerFactory.getLogger(OneNoteDocumentReader.class);

	private final OneNoteResource oneNoteResource;

	private final HttpClient client;

	private final String accessToken;

	public OneNoteDocumentReader(String accessToken, OneNoteResource oneNoteResource) {
		this.accessToken = accessToken;
		this.oneNoteResource = oneNoteResource;
		this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
	}

	/**
	 * Retrieves the content of a OneNote notebook by querying the Microsoft Graph API.
	 */
	private List<String> getNoteBookContent(String accessToken, String notebookId) {
		// Build the URI for fetching pages from the notebook
		String uri = MICROSOFT_GRAPH_BASE_URL + NOTEBOOK_ID_FILTER_PREFIX + "+eq+" + "'" + notebookId + "'";

		// Get the page IDs from the notebook by querying the API
		List<String> pageIdsFromNotebook = getOneNotePageIdsByURI(accessToken, uri);

		// Fetch the content for each page by its ID
		return pageIdsFromNotebook.stream().map(id -> getPageContent(accessToken, id)).toList();
	}

	/**
	 * Retrieves the content of a OneNote section by querying the Microsoft Graph API.
	 */
	private List<String> getSectionContent(String accessToken, String sectionId) {
		// Build the URI for fetching pages from the section
		String uri = MICROSOFT_GRAPH_BASE_URL + SECTION_ID_FILTER_PREFIX + "+eq+" + "'" + sectionId + "'";

		// Get the page IDs from the notebook by querying the API
		List<String> pageIdsBySection = getOneNotePageIdsByURI(accessToken, uri);

		// Fetch the content for each page by its ID
		return pageIdsBySection.stream().map(id -> getPageContent(accessToken, id)).toList();
	}

	private List<String> getOneNotePageIdsByURI(String accessToken, String uri) {
		HttpRequest request = HttpRequest.newBuilder()
			.header("Authorization", accessToken)
			.header("Content-Type", "application/json")
			.uri(URI.create(uri))
			.GET()
			.build();

		try {
			HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
			Assert.isTrue(response.statusCode() == 200, "Failed to fetch pages information");
			// Parse JSON response and extract page IDs
			return parsePageIdsFromJson(response.body());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get pages id", e);
		}
	}

	/**
	 * Parses the JSON response and extracts page IDs
	 */
	private List<String> parsePageIdsFromJson(String jsonResponse) {
		JsonObject rootObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
		JsonArray valueArray = rootObject.getAsJsonArray("value");

		return valueArray.asList()
			.stream()
			.map(jsonElement -> jsonElement.getAsJsonObject().get("id").getAsString())
			.toList();
	}

	/**
	 * Retrieves the content of a specific OneNote page by querying the Microsoft Graph
	 * API.
	 */
	private String getPageContent(String accessToken, String pageId) {
		URI uri = URI.create(MICROSOFT_GRAPH_BASE_URL + "/me/onenote/pages/" + pageId + "/content");
		HttpRequest request = HttpRequest.newBuilder().header("Authorization", accessToken).uri(uri).GET().build();
		try {
			HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
			Assert.isTrue(response.statusCode() == 200, "Failed to fetch page blocks");
			return parseHtmlContent(response.body());
		}
		catch (Exception e) {
			log.warn("Failed to get page content with token: {}, pageId: {}, {}", accessToken, pageId, e.getMessage(),
					e);
			throw new RuntimeException("Failed to get page content", e);
		}
	}

	@Override
	public List<Document> get() {
		// Get the access token
		String accessToken = this.accessToken;
		// Get the resource type and resource ID for the OneNote resource
		OneNoteResource.ResourceType resourceType = this.oneNoteResource.getResourceType();
		String resourceId = this.oneNoteResource.getResourceId();

		// Parameters check
		Assert.notNull(accessToken, "token must not be null");
		Assert.notNull(resourceType, "resource type must not be null");
		Assert.notNull(resourceId, "resource id must not be null");

		// Fetch content based on the resource type (Notebook, Section, or Page)
		List<String> content = switch (resourceType) {
			case NOTEBOOK -> getNoteBookContent(accessToken, resourceId);
			case SECTION -> getSectionContent(accessToken, resourceId);
			case PAGE -> Collections.singletonList(getPageContent(accessToken, resourceId));
		};

		// Build metadata for the resource
		Map<String, Object> metaData = buildMetadata();

		// Construct a list of Document objects
		return content.stream().map(c -> new Document(c, metaData)).toList();
	}

	private String parseHtmlContent(String htmlContent) {
		// Parse the HTML content
		org.jsoup.nodes.Document parseDoc = Jsoup.parse(htmlContent);

		// Get title and text content, ensuring title is not empty
		String title = parseDoc.title();
		String text = parseDoc.text();

		// Return title and content in a readable format
		return (StringUtils.hasText(title) ? title : "") + "\n" + text;
	}

	/**
	 * Builds metadata for a given OneNote resource (Notebook, Section, or Page) by
	 * querying the Microsoft Graph API.
	 */
	private Map<String, Object> buildMetadata() {
		Map<String, Object> metadata = new HashMap<>();
		String accessToken = this.accessToken;
		String resourceId = this.oneNoteResource.getResourceId();
		OneNoteResource.ResourceType resourceType = this.oneNoteResource.getResourceType();
		String endpoint = switch (resourceType) {
			case NOTEBOOK -> "/notebooks/";
			case SECTION -> "/sections/";
			case PAGE -> "/pages/";
		};
		String uriPath = MICROSOFT_GRAPH_BASE_URL + "/me/onenote" + endpoint + resourceId;
		URI uri = URI.create(uriPath);

		// Add basic metadata to the map (resource URI, type, and ID)
		metadata.put(OneNoteResource.SOURCE, uriPath);
		metadata.put("resourceType", resourceType.name());
		metadata.put("resourceId", resourceId);

		try {
			HttpRequest request = HttpRequest.newBuilder()
				.header("Authorization", accessToken)
				.header("Content-Type", "application/json")
				.uri(uri)
				.GET()
				.build();

			HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
			Assert.isTrue(response.statusCode() == 200, "Failed to fetch page blocks");

			// Parse the JSON response to extract relevant metadata fields
			JsonObject jsonMetaData = JsonParser.parseString(response.body()).getAsJsonObject();

			// Extract creation date and add to metadata if available
			String createDateTime = Optional.ofNullable(jsonMetaData.get("createdDateTime"))
				.map(JsonElement::getAsString)
				.orElse(null);
			if (StringUtils.hasText(createDateTime)) {
				metadata.put("createdTime", Instant.parse(createDateTime).toEpochMilli());
			}

			// Extract last modified date and add to metadata if available
			String lastModifiedDateTime = Optional.ofNullable(jsonMetaData.get("lastModifiedDateTime"))
				.map(JsonElement::getAsString)
				.orElse(null);
			if (StringUtils.hasText(lastModifiedDateTime)) {
				metadata.put("lastModifiedTime", Instant.parse(lastModifiedDateTime).toEpochMilli());
			}

			// Extract content URL and add to metadata if available
			String contentURL = Optional.ofNullable(jsonMetaData.get("contentUrl"))
				.map(JsonElement::getAsString)
				.orElse(null);
			if (StringUtils.hasText(contentURL)) {
				metadata.put("contentURL", contentURL);
			}

		}
		catch (Exception e) {
			log.warn("Failed to get page content with token: {}, resourceId: {}, resourceType: {}, {}", accessToken,
					resourceId, resourceType, e.getMessage(), e);
			throw new RuntimeException("Failed to get page content", e);
		}
		return metadata;
	}

}
