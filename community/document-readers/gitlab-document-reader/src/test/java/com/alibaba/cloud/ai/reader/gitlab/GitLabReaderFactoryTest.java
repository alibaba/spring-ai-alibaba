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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test cases for GitLabReaderFactory.
 *
 * @author brianxiadong
 */
@ExtendWith(MockitoExtension.class)
class GitLabReaderFactoryTest {

    private static final String TEST_HOST_URL = "https://gitlab.com";
    private static final String TEST_TOKEN = "test-token";
    private static final Integer TEST_PROJECT_ID = 123;

    @Test
    void testCreateFactoryForPublicRepositories() {
        GitLabReaderFactory factory = new GitLabReaderFactory(TEST_HOST_URL);
        try {
            // Create readers
            GitLabRepositoryReader repoReader = factory.createRepositoryReader(TEST_PROJECT_ID, true, true);
            GitLabIssueReader issueReader = factory.createIssueReader(TEST_PROJECT_ID, null, true);

            // Verify readers are created
            assertThat(repoReader).isNotNull();
            assertThat(issueReader).isNotNull();
        } finally {
            factory.close();
        }
    }

    @Test
    void testCreateFactoryWithInvalidToken() {
        assertThatThrownBy(() -> new GitLabReaderFactory(TEST_HOST_URL, "invalid-token"))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("Failed to authenticate with GitLab");
    }

    @Test
    void testFactoryResourceManagement() {
        GitLabReaderFactory factory = new GitLabReaderFactory(TEST_HOST_URL);
        
        // Create and use readers
        GitLabRepositoryReader repoReader = factory.createRepositoryReader(TEST_PROJECT_ID, true, true);
        GitLabIssueReader issueReader = factory.createIssueReader(TEST_PROJECT_ID, null, true);
        
        assertThat(repoReader).isNotNull();
        assertThat(issueReader).isNotNull();

        // Close factory
        factory.close();
        
        // Verify factory is closed by trying to create new readers
        assertThatThrownBy(() -> factory.createRepositoryReader(TEST_PROJECT_ID, true, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GitLabApi has been closed");
    }
} 