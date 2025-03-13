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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for GitLabIssueReader. Using real issues from Spring AI project
 * (https://gitlab.com/spring-ai/spring-ai).
 *
 * Tests are only run if GITLAB_NAMESPACE and GITLAB_PROJECT_NAME environment variables
 * are set.
 *
 * @author brianxiadong
 */
@EnabledIfEnvironmentVariable(named = "GITLAB_NAMESPACE", matches = ".+")
@EnabledIfEnvironmentVariable(named = "GITLAB_PROJECT_NAME", matches = ".+")
class GitLabIssueReaderTest {

	private static final String TEST_HOST_URL = "https://gitlab.com";

	private static final String TEST_NAMESPACE = System.getenv("GITLAB_NAMESPACE") != null
			? System.getenv("GITLAB_NAMESPACE") : "";

	private static final String TEST_PROJECT_NAME = System.getenv("GITLAB_PROJECT_NAME") != null
			? System.getenv("GITLAB_PROJECT_NAME") : "";

	// Static initializer to log a message if environment variables are not set
	static {
		if (System.getenv("GITLAB_NAMESPACE") == null || System.getenv("GITLAB_PROJECT_NAME") == null) {
			System.out.println(
					"Skipping GitLab tests because GITLAB_NAMESPACE and/or GITLAB_PROJECT_NAME environment variables are not set.");
		}
	}

	private GitLabIssueReader reader;

	@BeforeEach
	void setUp() throws GitLabApiException {
		// Create GitLabIssueReader instance only if environment variables are set
		if (System.getenv("GITLAB_NAMESPACE") != null && System.getenv("GITLAB_PROJECT_NAME") != null) {
			// Create GitLabIssueReader instance for accessing public project
			reader = new GitLabIssueReader(TEST_HOST_URL, TEST_NAMESPACE, TEST_PROJECT_NAME);
		}
		// If environment variables are not set, reader will remain null and tests will
		// be skipped
	}

	@Test
	void testGetIssuesWithDefaultParameters() {
		// Skip test if reader is null (environment variables not set)
		Assumptions.assumeTrue(reader != null, "Skipping test because GitLabIssueReader could not be initialized");

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
		// Skip test if environment variables are not set
		Assumptions.assumeTrue(
				System.getenv("GITLAB_NAMESPACE") != null && System.getenv("GITLAB_PROJECT_NAME") != null,
				"Skipping test because GITLAB_NAMESPACE and/or GITLAB_PROJECT_NAME environment variables are not set");

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
		// Skip test if environment variables are not set
		Assumptions.assumeTrue(
				System.getenv("GITLAB_NAMESPACE") != null && System.getenv("GITLAB_PROJECT_NAME") != null,
				"Skipping test because GITLAB_NAMESPACE and/or GITLAB_PROJECT_NAME environment variables are not set");

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
