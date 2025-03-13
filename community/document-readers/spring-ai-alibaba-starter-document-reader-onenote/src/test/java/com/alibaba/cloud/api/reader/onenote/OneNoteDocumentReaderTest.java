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

import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sparkle6979l
 * @author brianxiadong
 * @version 1.0
 * @data 2025/1/27 19:59
 */
@EnabledIfEnvironmentVariable(named = "ONENOTE_ACCESS_TOKEN", matches = ".+")
public class OneNoteDocumentReaderTest {

	private static final String TEST_ACCESS_TOKEN = System.getenv("ONENOTE_ACCESS_TOKEN");

	private static final String TEST_NOTEBOOK_ID = "${notebookId}";

	private static final String TEST_SECTION_ID = "${sectionId}";

	private static final String TEST_PAGE_ID = "${pageId}";

	private OneNoteDocumentReader oneNoteDocumentReader;

	static {
		if (TEST_ACCESS_TOKEN == null || TEST_ACCESS_TOKEN.isEmpty()) {
			System.out.println("ONENOTE_ACCESS_TOKEN environment variable is not set. Tests will be skipped.");
		}
	}

	@Test
	public void test_load_page() {
		// Ensure TEST_ACCESS_TOKEN is not null, otherwise skip the test
		Assumptions.assumeTrue(TEST_ACCESS_TOKEN != null && !TEST_ACCESS_TOKEN.isEmpty(),
				"Skipping test because ONENOTE_ACCESS_TOKEN is not set");

		// Create page reader
		OneNoteResource oneNoteResource = OneNoteResource.builder()
			.resourceId(TEST_PAGE_ID)
			.resourceType(OneNoteResource.ResourceType.PAGE)
			.build();
		OneNoteDocumentReader oneNoteDocumentReader = new OneNoteDocumentReader(TEST_ACCESS_TOKEN, oneNoteResource);

		List<Document> documents = oneNoteDocumentReader.get();
		// then
		assertThat(documents).isNotEmpty();
		Document document = documents.get(0);

		// Verify metadata
		assertThat(document.getMetadata()).containsKey(OneNoteResource.SOURCE);
		assertThat(document.getMetadata().get("resourceType")).isEqualTo(OneNoteResource.ResourceType.PAGE.name());
		assertThat(document.getMetadata().get("resourceId")).isEqualTo(TEST_PAGE_ID);

		// Verify content
		String content = document.getText();
		assertThat(content).isNotEmpty();
	}

	@Test
	public void test_load_section() {
		// Ensure TEST_ACCESS_TOKEN is not null, otherwise skip the test
		Assumptions.assumeTrue(TEST_ACCESS_TOKEN != null && !TEST_ACCESS_TOKEN.isEmpty(),
				"Skipping test because ONENOTE_ACCESS_TOKEN is not set");

		// Create page reader
		OneNoteResource oneNoteResource = OneNoteResource.builder()
			.resourceId(TEST_SECTION_ID)
			.resourceType(OneNoteResource.ResourceType.SECTION)
			.build();
		OneNoteDocumentReader oneNoteDocumentReader = new OneNoteDocumentReader(TEST_ACCESS_TOKEN, oneNoteResource);

		List<Document> documents = oneNoteDocumentReader.get();
		// then
		assertThat(documents).isNotEmpty();
		Document document = documents.get(0);

		// Verify metadata
		assertThat(document.getMetadata()).containsKey(OneNoteResource.SOURCE);
		assertThat(document.getMetadata().get("resourceType")).isEqualTo(OneNoteResource.ResourceType.SECTION.name());
		assertThat(document.getMetadata().get("resourceId")).isEqualTo(TEST_SECTION_ID);

		// Verify content
		String content = document.getText();
		assertThat(content).isNotEmpty();
	}

	@Test
	public void test_load_notebook() {
		// Ensure TEST_ACCESS_TOKEN is not null, otherwise skip the test
		Assumptions.assumeTrue(TEST_ACCESS_TOKEN != null && !TEST_ACCESS_TOKEN.isEmpty(),
				"Skipping test because ONENOTE_ACCESS_TOKEN is not set");

		// Create page reader
		OneNoteResource oneNoteResource = OneNoteResource.builder()
			.resourceId(TEST_NOTEBOOK_ID)
			.resourceType(OneNoteResource.ResourceType.NOTEBOOK)
			.build();
		OneNoteDocumentReader oneNoteDocumentReader = new OneNoteDocumentReader(TEST_ACCESS_TOKEN, oneNoteResource);

		List<Document> documents = oneNoteDocumentReader.get();
		// then
		assertThat(documents).isNotEmpty();
		Document document = documents.get(0);

		// Verify metadata
		assertThat(document.getMetadata()).containsKey(OneNoteResource.SOURCE);
		assertThat(document.getMetadata().get("resourceType")).isEqualTo(OneNoteResource.ResourceType.NOTEBOOK.name());
		assertThat(document.getMetadata().get("resourceId")).isEqualTo(TEST_NOTEBOOK_ID);

		// Verify content
		String content = document.getText();
		assertThat(content).isNotEmpty();
	}

}
