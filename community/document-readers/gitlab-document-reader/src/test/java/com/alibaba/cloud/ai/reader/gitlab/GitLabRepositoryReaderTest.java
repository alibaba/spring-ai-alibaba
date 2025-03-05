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
package com.alibaba.cloud.ai.reader.gitlab;

import org.gitlab4j.api.GitLabApiException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for GitLabRepositoryReader. Using real repository from Spring AI project
 * (https://gitlab.com/spring-ai/spring-ai).
 *
 * Tests are only run if GITLAB_NAMESPACE and GITLAB_PROJECT_NAME environment variables
 * are set.
 *
 * @author brianxiadong
 */
@EnabledIfEnvironmentVariable(named = "GITLAB_NAMESPACE", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GITLAB_PROJECT_NAME", matches = ".+")
class GitLabRepositoryReaderTest {

	private static final String TEST_HOST_URL = "https://gitlab.com";

	private static final String TEST_NAMESPACE = System.getenv("GITLAB_NAMESPACE") != null
			? System.getenv("GITLAB_NAMESPACE") : "";

	private static final String TEST_PROJECT_NAME = System.getenv("GITLAB_PROJECT_NAME") != null
			? System.getenv("GITLAB_PROJECT_NAME") : "";

	private static final String TEST_REF = "master";

	private static final String TEST_FILE_PATH = "README.md";

	// Static initializer to log a message if environment variables are not set
	static {
		if (System.getenv("GITLAB_NAMESPACE") == null || System.getenv("GITLAB_PROJECT_NAME") == null) {
			System.out.println(
					"Skipping GitLab tests because GITLAB_NAMESPACE and/or GITLAB_PROJECT_NAME environment variables are not set.");
		}
	}

	private GitLabRepositoryReader reader;

	@BeforeEach
	void setUp() throws GitLabApiException {
		// Create GitLabRepositoryReader instance only if environment variables are set
		if (System.getenv("GITLAB_NAMESPACE") != null && System.getenv("GITLAB_PROJECT_NAME") != null) {
			// Create GitLabRepositoryReader instance for accessing public project
			reader = new GitLabRepositoryReader(TEST_HOST_URL, TEST_NAMESPACE, TEST_PROJECT_NAME);
		}
		// If environment variables are not set, reader will remain null and tests will
		// be skipped
	}

	@Test
	void testGetSingleFile() {
		// Skip test if reader is null (environment variables not set)
		Assumptions.assumeTrue(reader != null, "Skipping test because GitLabRepositoryReader could not be initialized");

		// Configure reader to load a single file
		List<Document> documents = reader.setRef(TEST_REF).setFilePath(TEST_FILE_PATH).get();

		// Verify results
		assertThat(documents).hasSize(1);
		Document doc = documents.get(0);
		assertThat(doc.getId()).isNotNull();
		assertThat(doc.getText()).isNotBlank();
		assertThat(doc.getMetadata()).containsKey("file_path")
			.containsKey("file_name")
			.containsKey("url")
			.containsKey("ref");
	}

	@Test
	void testGetAllFiles() {
		// Skip test if reader is null (environment variables not set)
		Assumptions.assumeTrue(reader != null, "Skipping test because GitLabRepositoryReader could not be initialized");

		// Configure reader to load all files from root directory
		List<Document> documents = reader.setRef(TEST_REF).setRecursive(true).get();

		// Verify results
		assertThat(documents).isNotEmpty();

		// Verify each document has required metadata
		for (Document doc : documents) {
			assertThat(doc.getId()).isNotNull();
			assertThat(doc.getText()).isNotBlank();
			assertThat(doc.getMetadata()).containsKey("file_path")
				.containsKey("file_name")
				.containsKey("url")
				.containsKey("ref");
		}
	}

	@Test
	void testGetMarkdownFiles() {
		// Skip test if reader is null (environment variables not set)
		Assumptions.assumeTrue(reader != null, "Skipping test because GitLabRepositoryReader could not be initialized");

		// Configure reader to load only markdown files
		List<Document> documents = reader.setRef(TEST_REF).setPattern("*.md").setRecursive(true).get();

		// Verify results
		assertThat(documents).isNotEmpty();

		// Verify all documents are markdown files
		for (Document doc : documents) {
			assertThat(doc.getId()).isNotNull();
			assertThat(doc.getText()).isNotBlank();
			assertThat(doc.getMetadata()).containsKey("file_path")
				.containsKey("file_name")
				.containsKey("url")
				.containsKey("ref");

			String filePath = (String) doc.getMetadata().get("file_path");
			assertThat(filePath).endsWith(".md");
		}
	}

	@Test
	void testGetFilesInDirectory() {
		// Skip test if reader is null (environment variables not set)
		Assumptions.assumeTrue(reader != null, "Skipping test because GitLabRepositoryReader could not be initialized");

		// Configure reader to load files from a specific directory
		List<Document> documents = reader.setRef(TEST_REF).setFilePath("docs").setRecursive(true).get();

		// Verify results
		assertThat(documents).isNotEmpty();

		// Verify all files are from the docs directory
		for (Document doc : documents) {
			String filePath = (String) doc.getMetadata().get("file_path");
			assertThat(filePath).startsWith("docs/");
		}
	}

	@Test
	void testGetFilesWithComplexPattern() {
		// Skip test if reader is null (environment variables not set)
		Assumptions.assumeTrue(reader != null, "Skipping test because GitLabRepositoryReader could not be initialized");

		// Configure reader to load Java files from src directory and its subdirectories
		List<Document> documents = reader.setRef(TEST_REF).setPattern("src/**/*.java").setRecursive(true).get();

		// Verify results
		assertThat(documents).isNotEmpty();

		// Verify all files match the pattern
		for (Document doc : documents) {
			String filePath = (String) doc.getMetadata().get("file_path");
			assertThat(filePath).startsWith("src/").endsWith(".java");
		}
	}

	@Test
	void testGetFilesWithMetadata() {
		// Skip test if reader is null (environment variables not set)
		Assumptions.assumeTrue(reader != null, "Skipping test because GitLabRepositoryReader could not be initialized");

		// Configure reader to load a single file and check metadata
		List<Document> documents = reader.setRef(TEST_REF).setFilePath(TEST_FILE_PATH).get();

		// Verify results
		assertThat(documents).hasSize(1);
		Document doc = documents.get(0);

		// Verify required metadata fields
		assertThat(doc.getMetadata()).containsKey("file_path")
			.containsKey("file_name")
			.containsKey("size")
			.containsKey("url")
			.containsKey("ref");

		// Verify metadata values
		assertThat(doc.getMetadata().get("file_path")).isEqualTo(TEST_FILE_PATH);
		assertThat(doc.getMetadata().get("file_name")).isEqualTo("README.md");
		assertThat(doc.getMetadata().get("size")).isInstanceOf(Integer.class);
		assertThat(doc.getMetadata().get("url")).asString().contains(TEST_FILE_PATH);
	}

}
