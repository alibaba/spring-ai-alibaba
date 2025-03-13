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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.StringUtils;

/**
 * Notion Document Reader Implements DocumentReader interface to read content from Notion
 *
 * @author xiadong
 * @since 2024-01-06
 */

public class NotionDocumentReader implements DocumentReader {

	private final NotionResource notionResource;

	private final JsonNode pageMetadata;

	/**
	 * Constructor
	 * @param notionResource Notion resource
	 */
	public NotionDocumentReader(NotionResource notionResource) {
		this.notionResource = notionResource;
		this.pageMetadata = notionResource.getMetadata();
	}

	@Override
	public List<Document> get() {
		try {
			// Read content from input stream
			String content = readContent();

			// Create metadata map
			Map<String, Object> metadata = buildMetadata();

			// Create and return document
			return Collections.singletonList(new Document(content, metadata));
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load document from Notion: " + e.getMessage(), e);
		}
	}

	/**
	 * Build metadata map from Notion API response
	 */
	private Map<String, Object> buildMetadata() {
		Map<String, Object> metadata = new HashMap<>();

		// Add basic metadata
		metadata.put(NotionResource.SOURCE, notionResource.getSource());
		metadata.put("resourceType", notionResource.getResourceType().name());
		metadata.put("resourceId", notionResource.getResourceId());

		// Add metadata from Notion API
		if (pageMetadata != null) {
			// Creation and update times
			String createdTime = pageMetadata.get("created_time").asText();
			if (StringUtils.hasText(createdTime)) {
				metadata.put("createdTime", Instant.parse(createdTime).toEpochMilli());
			}

			String lastEditedTime = pageMetadata.get("last_edited_time").asText();
			if (StringUtils.hasText(lastEditedTime)) {
				metadata.put("lastEditedTime", Instant.parse(lastEditedTime).toEpochMilli());
			}

			// Creator and last editor
			JsonNode createdBy = pageMetadata.get("created_by");
			if (createdBy != null) {
				String creatorName = createdBy.get("name").asText();
				String creatorId = createdBy.get("id").asText();
				if (StringUtils.hasText(creatorName)) {
					metadata.put("createdBy", creatorName);
				}
				if (StringUtils.hasText(creatorId)) {
					metadata.put("createdById", creatorId);
				}
			}

			JsonNode lastEditedBy = pageMetadata.get("last_edited_by");
			if (lastEditedBy != null) {
				String editorName = lastEditedBy.get("name").asText();
				String editorId = lastEditedBy.get("id").asText();
				if (StringUtils.hasText(editorName)) {
					metadata.put("lastEditedBy", editorName);
				}
				if (StringUtils.hasText(editorId)) {
					metadata.put("lastEditedById", editorId);
				}
			}

			// URL
			String url = pageMetadata.get("url").asText();
			if (StringUtils.hasText(url)) {
				metadata.put("url", url);
			}

			// Parent information
			JsonNode parent = pageMetadata.get("parent");
			if (parent != null) {
				String parentType = parent.get("type").asText();
				if (StringUtils.hasText(parentType)) {
					metadata.put("parentType", parentType);
					String parentId = parent.get(parentType + "_id").asText();
					if (StringUtils.hasText(parentId)) {
						metadata.put("parentId", parentId);
					}
				}
			}

			// Icon
			JsonNode icon = pageMetadata.get("icon");
			if (icon != null) {
				String iconType = icon.get("type").asText();
				String iconUrl = icon.get("url").asText();
				if (StringUtils.hasText(iconType)) {
					metadata.put("iconType", iconType);
				}
				if (StringUtils.hasText(iconUrl)) {
					metadata.put("iconUrl", iconUrl);
				}
			}

			// Cover
			JsonNode cover = pageMetadata.get("cover");
			if (cover != null) {
				String coverType = cover.get("type").asText();
				String coverUrl = cover.get("url").asText();
				if (StringUtils.hasText(coverType)) {
					metadata.put("coverType", coverType);
				}
				if (StringUtils.hasText(coverUrl)) {
					metadata.put("coverUrl", coverUrl);
				}
			}
		}

		return metadata;
	}

	/**
	 * Read content from input stream
	 */
	private String readContent() throws IOException {
		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(notionResource.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
		}
		return content.toString();
	}

}
