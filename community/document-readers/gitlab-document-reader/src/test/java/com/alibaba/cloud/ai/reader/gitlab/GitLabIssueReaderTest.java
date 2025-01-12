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

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.IssuesApi;
import org.gitlab4j.api.models.Issue;
import org.gitlab4j.api.models.IssueFilter;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.Constants.IssueState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test cases for GitLabIssueReader.
 *
 * @author YunLong
 */
@ExtendWith(MockitoExtension.class)
class GitLabIssueReaderTest {

    private static final String TEST_HOST_URL = "https://gitlab.com";
    private static final String TEST_TOKEN = "test-token";
    private static final Integer TEST_PROJECT_ID = 123;

    @Mock
    private GitLabApi gitLabApi;

    @Mock
    private IssuesApi issuesApi;

    private GitLabReaderFactory factory;
    private GitLabIssueReader reader;

    @BeforeEach
    void setUp() throws Exception {
        // Mock GitLabApi creation
        when(gitLabApi.getUserApi().getCurrentUser()).thenReturn(new User());
        when(gitLabApi.getIssuesApi()).thenReturn(issuesApi);

        // Create factory with mocked GitLabApi
        factory = new GitLabReaderFactory(TEST_HOST_URL, TEST_TOKEN) {
            @Override
            public GitLabIssueReader createIssueReader(Integer projectId, Integer groupId, boolean verbose) {
                return new GitLabIssueReader(gitLabApi, projectId, groupId, verbose);
            }
        };

        // Create reader using factory
        reader = factory.createIssueReader(TEST_PROJECT_ID, null, true);
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void testGetIssuesWithDefaultParameters() throws Exception {
        // Prepare test data
        Issue mockIssue = new Issue();
        mockIssue.setIid(1L);
        mockIssue.setTitle("Test Issue");
        mockIssue.setDescription("Test Description");
        mockIssue.setState(IssueState.OPENED);
        mockIssue.setLabels(Arrays.asList("bug", "critical"));
        
        User author = new User();
        author.setUsername("testAuthor");
        mockIssue.setAuthor(author);

        User assignee = new User();
        assignee.setUsername("testAssignee");
        mockIssue.setAssignee(assignee);

        when(issuesApi.getIssues(eq(TEST_PROJECT_ID), any(IssueFilter.class)))
            .thenReturn(Collections.singletonList(mockIssue));

        // Execute test
        List<Document> documents = reader.get();

        // Verify results
        assertThat(documents).hasSize(1);
        Document doc = documents.get(0);
        assertThat(doc.getId()).isEqualTo("1");
        assertThat(doc.getContent()).contains("Test Issue");
        assertThat(doc.getContent()).contains("Test Description");
        assertThat(doc.getMetadata())
            .containsEntry("state", IssueState.OPENED.toString())
            .containsEntry("author", "testAuthor")
            .containsEntry("assignee", "testAssignee");
    }

    @Test
    void testLoadDataWithCustomParameters() throws Exception {
        // Prepare test data
        Issue mockIssue = new Issue();
        mockIssue.setIid(2L);
        mockIssue.setTitle("Custom Issue");
        mockIssue.setDescription("Custom Description");
        mockIssue.setState(IssueState.CLOSED);

        when(issuesApi.getIssues(eq(TEST_PROJECT_ID), any(IssueFilter.class)))
            .thenReturn(Collections.singletonList(mockIssue));

        // Execute test with custom parameters
        List<Document> documents = reader.loadData(
            "assignee1",
            "author1",
            true,
            LocalDateTime.now().minusDays(7),
            LocalDateTime.now(),
            Arrays.asList(1, 2),
            GitLabIssueType.ISSUE,
            Arrays.asList("label1", "label2"),
            "milestone1",
            true,
            GitLabScope.ALL,
            "searchTerm",
            GitLabIssueState.CLOSED,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        );

        // Verify results
        assertThat(documents).hasSize(1);
        Document doc = documents.get(0);
        assertThat(doc.getId()).isEqualTo("2");
        assertThat(doc.getContent()).contains("Custom Issue");
        assertThat(doc.getContent()).contains("Custom Description");
        assertThat(doc.getMetadata()).containsEntry("state", IssueState.CLOSED.toString());
    }
} 