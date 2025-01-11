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
import org.gitlab4j.api.models.User;
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

    @Mock
    private GitLabApi gitLabApi;

    @Mock
    private IssuesApi issuesApi;

    private GitLabIssueReader reader;

    @BeforeEach
    void setUp() {
        when(gitLabApi.getIssuesApi()).thenReturn(issuesApi);
        reader = new GitLabIssueReader(gitLabApi, 123, null, true);
    }

    @Test
    void testGetIssuesWithDefaultParameters() throws Exception {
        // Prepare test data
        Issue mockIssue = new Issue();
        mockIssue.setIid(1);
        mockIssue.setTitle("Test Issue");
        mockIssue.setDescription("Test Description");
        mockIssue.setState("opened");
        mockIssue.setLabels(Arrays.asList("bug", "critical"));
        
        User author = new User();
        author.setUsername("testAuthor");
        mockIssue.setAuthor(author);

        User assignee = new User();
        assignee.setUsername("testAssignee");
        mockIssue.setAssignee(assignee);

        when(issuesApi.getIssues(eq(123), anyString(), anyList(), anyString(), 
                                isNull(), isNull(), isNull(), isNull(),
                                isNull(), isNull(), isNull(), isNull(), isNull()))
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
            .containsEntry("state", "opened")
            .containsEntry("author", "testAuthor")
            .containsEntry("assignee", "testAssignee");
    }

    @Test
    void testLoadDataWithCustomParameters() throws Exception {
        // Prepare test data
        Issue mockIssue = new Issue();
        mockIssue.setIid(2);
        mockIssue.setTitle("Custom Issue");
        mockIssue.setDescription("Custom Description");
        mockIssue.setState("closed");

        when(issuesApi.getIssues(eq(123), anyString(), anyList(), anyString(),
                                anyString(), anyString(), anyString(), anyString(),
                                anyBoolean(), any(), any(), any(), any()))
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
        assertThat(doc.getMetadata()).containsEntry("state", "closed");
    }
} 