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
package com.alibaba.cloud.ai.reader.gitbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link GitbookDocumentReader}.
 *
 * @author brianxiadong
 */
@ExtendWith(MockitoExtension.class)
class GitbookDocumentReaderTest {

	// Get API token from environment variable, use test value if not set
	private static final String API_TOKEN = System.getenv("GITBOOK_API_TOKEN") != null
			? System.getenv("GITBOOK_API_TOKEN") : "test-token";

	// Get space ID from environment variable, use test value if not set
	private static final String SPACE_ID = System.getenv("GITBOOK_SPACE_ID") != null ? System.getenv("GITBOOK_SPACE_ID")
			: "test-space";

	private static final String CUSTOM_API_URL = "https://api.custom-gitbook.com";

	// Flag to indicate if Gitbook credentials are available
	private static final boolean gitbookCredentialsAvailable = isGitbookCredentialsAvailable();

	@Mock
	private GitbookClient mockGitbookClient;

	private GitbookDocumentReader reader;

	/**
	 * Check if Gitbook credentials are available
	 * @return true if both API token and space ID are available, false otherwise
	 */
	public static boolean isGitbookCredentialsAvailable() {
		String apiToken = System.getenv("GITBOOK_API_TOKEN");
		String spaceId = System.getenv("GITBOOK_SPACE_ID");

		boolean available = apiToken != null && !apiToken.isEmpty() && spaceId != null && !spaceId.isEmpty();

		if (!available) {
			System.out.println(
					"Gitbook credentials not available. Set GITBOOK_API_TOKEN and GITBOOK_SPACE_ID environment variables to run these tests.");
		}

		return available;
	}

	@BeforeEach
	void setUp() {
		reader = new GitbookDocumentReader(API_TOKEN, SPACE_ID);
	}

	@Test
	@EnabledIf("isGitbookCredentialsAvailable")
	void constructorWithMinimalParameters() {
		GitbookDocumentReader reader = new GitbookDocumentReader(API_TOKEN, SPACE_ID);
		assertThat(reader).isNotNull();
	}

	@Test
	@EnabledIf("isGitbookCredentialsAvailable")
	void constructorWithAllParameters() {
		List<String> metadataFields = Arrays.asList("title", "description");
		GitbookDocumentReader reader = new GitbookDocumentReader(API_TOKEN, SPACE_ID, CUSTOM_API_URL, metadataFields);
		assertThat(reader).isNotNull();
	}

	@Test
	void constructorShouldThrowExceptionWhenApiTokenIsEmpty() {
		assertThatThrownBy(() -> new GitbookDocumentReader("", SPACE_ID)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("API Token must not be empty");
	}

	@Test
	void constructorShouldThrowExceptionWhenSpaceIdIsEmpty() {
		assertThatThrownBy(() -> new GitbookDocumentReader(API_TOKEN, "")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Space ID must not be empty");
	}

	@Test
	@EnabledIf("isGitbookCredentialsAvailable")
	void getShouldReturnEmptyListWhenNoPages() {
		GitbookDocumentReader reader = new GitbookDocumentReader(API_TOKEN, SPACE_ID);
		List<Document> documents = reader.get();
		assertThat(documents).isNotEmpty();
	}

}
