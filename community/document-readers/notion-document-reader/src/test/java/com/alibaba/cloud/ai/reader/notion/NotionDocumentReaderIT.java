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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Notion Document Reader
 *
 * @author xiadong
 * @since 2024-01-06
 */
class NotionDocumentReaderIT {

	private static final String NOTION_TOKEN = System.getenv("NOTION_TOKEN");

	// Test page ID
	private static final String TEST_PAGE_ID = "${pageId}";

	// Test database ID
	private static final String TEST_DATABASE_ID = "${databaseId}";

	NotionDocumentReader pageReader;

	NotionDocumentReader databaseReader;

	@BeforeEach
	public void beforeEach() {
		// Create page reader
		NotionResource pageResource = NotionResource.builder()
			.notionToken(NOTION_TOKEN)
			.resourceId(TEST_PAGE_ID)
			.resourceType(NotionResource.ResourceType.PAGE)
			.build();
		pageReader = new NotionDocumentReader(pageResource);

		// Create database reader
		NotionResource databaseResource = NotionResource.builder()
			.notionToken(NOTION_TOKEN)
			.resourceId(TEST_DATABASE_ID)
			.resourceType(NotionResource.ResourceType.DATABASE)
			.build();
		databaseReader = new NotionDocumentReader(databaseResource);
	}

	@Test
	void should_load_page() {
		// when
		List<Document> documents = pageReader.get();

		// then
		assertThat(documents).isNotEmpty();
		Document document = documents.get(0);

		// Verify metadata
		assertThat(document.getMetadata()).containsKey(NotionResource.SOURCE);
		assertThat(document.getMetadata().get(NotionResource.SOURCE)).isEqualTo("notion://page/" + TEST_PAGE_ID);
		assertThat(document.getMetadata().get("resourceType")).isEqualTo(NotionResource.ResourceType.PAGE.name());
		assertThat(document.getMetadata().get("resourceId")).isEqualTo(TEST_PAGE_ID);

		// Verify content
		String content = document.getContent();
		assertThat(content).isNotEmpty();
		System.out.println("Page content: " + content);
	}

	@Test
	void should_load_database() {
		// when
		List<Document> documents = databaseReader.get();

		// then
		assertThat(documents).isNotEmpty();
		Document document = documents.get(0);

		// Verify metadata
		assertThat(document.getMetadata()).containsKey(NotionResource.SOURCE);
		assertThat(document.getMetadata().get(NotionResource.SOURCE))
			.isEqualTo("notion://database/" + TEST_DATABASE_ID);
		assertThat(document.getMetadata().get("resourceType")).isEqualTo(NotionResource.ResourceType.DATABASE.name());
		assertThat(document.getMetadata().get("resourceId")).isEqualTo(TEST_DATABASE_ID);

		// Verify content
		String content = document.getContent();
		assertThat(content).isNotEmpty();
		System.out.println("Database content: " + content);
	}

}