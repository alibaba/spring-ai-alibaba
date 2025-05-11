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
package com.alibaba.cloud.ai.reader.obsidian;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Obsidian Document Reader
 *
 * Tests are only run if OBSIDIAN_VAULT_PATH environment variable is set.
 *
 * @author xiadong
 * @since 2024-01-06
 */
@EnabledIfEnvironmentVariable(named = "OBSIDIAN_VAULT_PATH", matches = ".+")
class ObsidianDocumentReaderIT {

	private static final String VAULT_PATH = System.getenv("OBSIDIAN_VAULT_PATH");

	// Static initializer to log a message if environment variable is not set
	static {
		if (VAULT_PATH == null || VAULT_PATH.isEmpty()) {
			System.out.println("Skipping Obsidian tests because OBSIDIAN_VAULT_PATH environment variable is not set.");
		}
	}

	ObsidianDocumentReader reader;

	@BeforeEach
	void setUp() {
		// Only initialize if VAULT_PATH is set
		if (VAULT_PATH != null && !VAULT_PATH.isEmpty()) {
			reader = ObsidianDocumentReader.builder().vaultPath(Path.of(VAULT_PATH)).build();
		}
	}

	@Test
	void should_read_markdown_files() {
		// Skip test if reader is null
		Assumptions.assumeTrue(reader != null, "Skipping test because ObsidianDocumentReader could not be initialized");

		// when
		List<Document> documents = reader.get();

		// then
		assertThat(documents).isNotEmpty();

		// Verify document content and metadata
		for (Document doc : documents) {
			// Verify source metadata
			assertThat(doc.getMetadata()).containsKey(ObsidianResource.SOURCE);
			String source = doc.getMetadata().get(ObsidianResource.SOURCE).toString();
			assertThat(source).isNotEmpty().endsWith(ObsidianResource.MARKDOWN_EXTENSION);

			// Verify content
			assertThat(doc.getText()).isNotEmpty();

			// Print for debugging
			System.out.println("Document source: " + source);
			if (doc.getMetadata().containsKey("category")) {
				System.out.println("Document category: " + doc.getMetadata().get("category"));
			}
			System.out.println("Document content: " + doc.getText());
			System.out.println("---");
		}
	}

}
