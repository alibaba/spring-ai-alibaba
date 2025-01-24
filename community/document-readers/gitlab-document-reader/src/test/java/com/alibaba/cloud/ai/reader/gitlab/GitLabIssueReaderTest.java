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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for GitLabIssueReader. Using real issues from Spring AI project
 * (https://gitlab.com/spring-ai/spring-ai).
 *
 * @author brianxiadong
 */
class GitLabIssueReaderTest {

	private static final String TEST_HOST_URL = "https://gitlab.com";

	private static final String TEST_NAMESPACE = "";

	private static final String TEST_PROJECT_NAME = "";

	private GitLabIssueReader reader;

	@BeforeEach
	void setUp() throws GitLabApiException {
		// Create GitLabIssueReader instance for accessing public project
		reader = new GitLabIssueReader(TEST_HOST_URL, TEST_NAMESPACE, TEST_PROJECT_NAME);
	}

	@Test
	void testGetIssuesWithDefaultParameters() {
		// Get all open issues directly
		List<Document> documents = reader.get();

		// Verify results
		assertThat(documents).isNotEmpty();

		// Verify basic structure of first document
		Document doc = documents.get(0);
		assertThat(doc.getId()).isNotNull();
		assertThat(doc.getText()).isNotBlank();
		assertThat(doc.getMetadata()).containsKey("state").containsKey("url");

		// Verify default state is OPEN
		assertThat(doc.getMetadata().get("state")).isEqualTo("opened");
	}

	@Test
	void testLoadDataWithCustomParameters() throws GitLabApiException {
		// Create new reader instance with custom parameters
		GitLabIssueConfig config = GitLabIssueConfig.builder()
			.confidential(false)
			.createdAfter(LocalDateTime.now().minusDays(365))
			.issueType(GitLabIssueType.ISSUE)
			.labels(Arrays.asList("enhancement", "feature"))
			.nonArchived(true)
			.scope(GitLabScope.ALL)
			.state(GitLabIssueState.CLOSED)
			.build();

		reader = new GitLabIssueReader(TEST_HOST_URL, TEST_NAMESPACE, TEST_PROJECT_NAME, null, config);

		// Get issues
		List<Document> documents = reader.get();

		// Verify results
		assertThat(documents).isNotEmpty();

		// Verify all documents match our filter criteria
		for (Document doc : documents) {
			assertThat(doc.getMetadata()).containsEntry("state", "closed").hasEntrySatisfying("labels", labels -> {
				@SuppressWarnings("unchecked")
				List<String> labelList = (List<String>) labels;
				assertThat(labelList).containsAnyOf("enhancement", "feature");
			}).containsKey("url");

			// Verify document content
			assertThat(doc.getId()).isNotNull();
			assertThat(doc.getText()).isNotBlank();
		}
	}

	@Test
	void testLoadSpecificIssue() throws GitLabApiException {
		// Create configuration to get specific issue
		GitLabIssueConfig config = GitLabIssueConfig.builder().iids(Arrays.asList(1)).build();

		reader = new GitLabIssueReader(TEST_HOST_URL, TEST_NAMESPACE, TEST_PROJECT_NAME, null, config);

		// Get specific issue (#1)
		List<Document> documents = reader.get();

		// Verify results
		assertThat(documents).hasSize(1);
		Document doc = documents.get(0);
		assertThat(doc.getId()).isEqualTo("1");
		assertThat(doc.getMetadata()).containsKey("state").containsKey("url");
	}

}
