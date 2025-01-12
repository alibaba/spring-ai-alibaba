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
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.RepositoryFileApi;
import org.gitlab4j.api.models.RepositoryFile;
import org.gitlab4j.api.models.TreeItem;
import org.gitlab4j.api.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test cases for GitLabRepositoryReader.
 *
 * @author brianxiadong
 */
@ExtendWith(MockitoExtension.class)
class GitLabRepositoryReaderTest {

    private static final String TEST_HOST_URL = "https://gitlab.com";
    private static final String TEST_TOKEN = "test-token";
    private static final Integer TEST_PROJECT_ID = 123;
    private static final String TEST_REF = "main";

    @Mock
    private GitLabApi gitLabApi;

    @Mock
    private RepositoryApi repositoryApi;

    @Mock
    private RepositoryFileApi repositoryFileApi;

    private GitLabReaderFactory factory;
    private GitLabRepositoryReader reader;

    @BeforeEach
    void setUp() throws Exception {
        // Mock GitLabApi creation
        when(gitLabApi.getUserApi().getCurrentUser()).thenReturn(new User());
        when(gitLabApi.getRepositoryApi()).thenReturn(repositoryApi);
        when(gitLabApi.getRepositoryFileApi()).thenReturn(repositoryFileApi);
        when(gitLabApi.getGitLabServerUrl()).thenReturn(TEST_HOST_URL);

        // Create factory with mocked GitLabApi
        factory = new GitLabReaderFactory(TEST_HOST_URL, TEST_TOKEN) {
            @Override
            public GitLabRepositoryReader createRepositoryReader(Integer projectId, boolean useParser, boolean verbose) {
                return new GitLabRepositoryReader(gitLabApi, projectId, useParser, verbose);
            }
        };

        // Create reader using factory
        reader = factory.createRepositoryReader(TEST_PROJECT_ID, true, true);
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void testLoadSingleFile() throws Exception {
        // Prepare test data
        String filePath = "test.txt";
        String content = "Test content";
        String base64Content = Base64.getEncoder().encodeToString(content.getBytes());

        RepositoryFile mockFile = new RepositoryFile();
        mockFile.setFilePath(filePath);
        mockFile.setFileName("test.txt");
        mockFile.setBlobId("123");
        mockFile.setContent(base64Content);

        when(repositoryFileApi.getFile(eq(TEST_PROJECT_ID), eq(filePath), eq(TEST_REF)))
            .thenReturn(mockFile);

        // Execute test
        List<Document> documents = reader.loadData(TEST_REF, filePath, null, false);

        // Verify results
        assertThat(documents).hasSize(1);
        Document doc = documents.get(0);
        assertThat(doc.getId()).isEqualTo("123");
        assertThat(doc.getContent()).isEqualTo(content);
        assertThat(doc.getMetadata())
            .containsEntry("file_path", filePath)
            .containsEntry("file_name", "test.txt");
    }

    @Test
    void testLoadDirectory() throws Exception {
        // Prepare test data
        TreeItem file1 = new TreeItem();
        file1.setType("blob");
        file1.setPath("file1.txt");

        TreeItem file2 = new TreeItem();
        file2.setType("blob");
        file2.setPath("file2.txt");

        TreeItem directory = new TreeItem();
        directory.setType("tree");
        directory.setPath("dir");

        when(repositoryApi.getTree(eq(TEST_PROJECT_ID), isNull(), eq(TEST_REF), eq(false)))
            .thenReturn(Arrays.asList(file1, file2, directory));

        // Mock file contents
        RepositoryFile mockFile1 = new RepositoryFile();
        mockFile1.setFilePath("file1.txt");
        mockFile1.setFileName("file1.txt");
        mockFile1.setBlobId("123");
        mockFile1.setContent(Base64.getEncoder().encodeToString("Content 1".getBytes()));

        RepositoryFile mockFile2 = new RepositoryFile();
        mockFile2.setFilePath("file2.txt");
        mockFile2.setFileName("file2.txt");
        mockFile2.setBlobId("456");
        mockFile2.setContent(Base64.getEncoder().encodeToString("Content 2".getBytes()));

        when(repositoryFileApi.getFile(eq(TEST_PROJECT_ID), eq("file1.txt"), eq(TEST_REF)))
            .thenReturn(mockFile1);
        when(repositoryFileApi.getFile(eq(TEST_PROJECT_ID), eq("file2.txt"), eq(TEST_REF)))
            .thenReturn(mockFile2);

        // Execute test
        List<Document> documents = reader.loadData(TEST_REF, null, null, false);

        // Verify results
        assertThat(documents).hasSize(2);
        assertThat(documents).extracting(Document::getId)
            .containsExactlyInAnyOrder("123", "456");
        assertThat(documents).extracting(Document::getContent)
            .containsExactlyInAnyOrder("Content 1", "Content 2");
    }
} 