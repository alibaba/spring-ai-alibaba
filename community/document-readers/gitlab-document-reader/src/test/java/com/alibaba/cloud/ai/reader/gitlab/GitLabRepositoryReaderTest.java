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

    @Mock
    private GitLabApi gitLabApi;

    @Mock
    private RepositoryApi repositoryApi;

    @Mock
    private RepositoryFileApi repositoryFileApi;

    private GitLabRepositoryReader reader;

    @BeforeEach
    void setUp() {
        when(gitLabApi.getRepositoryApi()).thenReturn(repositoryApi);
        when(gitLabApi.getRepositoryFileApi()).thenReturn(repositoryFileApi);
        when(gitLabApi.getGitLabServerUrl()).thenReturn("https://gitlab.com/api/v4");
        
        reader = new GitLabRepositoryReader(gitLabApi, 123, false, true);
    }

    @Test
    void testLoadSingleFile() throws Exception {
        // Prepare test data
        RepositoryFile mockFile = new RepositoryFile();
        mockFile.setBlobId("abc123");
        mockFile.setFilePath("test/file.txt");
        mockFile.setFileName("file.txt");
        String content = "Test file content";
        mockFile.setContent(Base64.getEncoder().encodeToString(content.getBytes()));

        when(repositoryFileApi.getFile(eq(123), eq("test/file.txt"), eq("main")))
            .thenReturn(mockFile);

        // Execute test
        List<Document> documents = reader.loadData("main", "test/file.txt", null, false);

        // Verify results
        assertThat(documents).hasSize(1);
        Document doc = documents.get(0);
        assertThat(doc.getId()).isEqualTo("abc123");
        assertThat(doc.getContent()).isEqualTo(content);
        assertThat(doc.getMetadata())
            .containsEntry("file_path", "test/file.txt")
            .containsEntry("file_name", "file.txt")
            .containsEntry("size", content.length());
    }

    @Test
    void testLoadDirectoryContents() throws Exception {
        // Prepare test data
        TreeItem file1 = new TreeItem();
        file1.setType("blob");
        file1.setPath("dir/file1.txt");

        TreeItem file2 = new TreeItem();
        file2.setType("blob");
        file2.setPath("dir/file2.txt");

        TreeItem directory = new TreeItem();
        directory.setType("tree");
        directory.setPath("dir/subdir");

        when(repositoryApi.getTree(eq(123), eq("dir"), eq("main"), eq(true)))
            .thenReturn(Arrays.asList(file1, file2, directory));

        // Mock file contents
        RepositoryFile mockFile1 = new RepositoryFile();
        mockFile1.setBlobId("abc123");
        mockFile1.setFilePath("dir/file1.txt");
        mockFile1.setFileName("file1.txt");
        mockFile1.setContent(Base64.getEncoder().encodeToString("Content 1".getBytes()));

        RepositoryFile mockFile2 = new RepositoryFile();
        mockFile2.setBlobId("def456");
        mockFile2.setFilePath("dir/file2.txt");
        mockFile2.setFileName("file2.txt");
        mockFile2.setContent(Base64.getEncoder().encodeToString("Content 2".getBytes()));

        when(repositoryFileApi.getFile(eq(123), eq("dir/file1.txt"), eq("main")))
            .thenReturn(mockFile1);
        when(repositoryFileApi.getFile(eq(123), eq("dir/file2.txt"), eq("main")))
            .thenReturn(mockFile2);

        // Execute test
        List<Document> documents = reader.loadData("main", null, "dir", true);

        // Verify results
        assertThat(documents).hasSize(2);
        assertThat(documents).extracting(Document::getId)
            .containsExactlyInAnyOrder("abc123", "def456");
        assertThat(documents).extracting(Document::getContent)
            .containsExactlyInAnyOrder("Content 1", "Content 2");
    }

    @Test
    void testGetDefaultBehavior() throws Exception {
        // Prepare test data
        when(repositoryApi.getTree(eq(123), isNull(), eq("main"), eq(false)))
            .thenReturn(Collections.emptyList());

        // Execute test
        List<Document> documents = reader.get();

        // Verify results
        assertThat(documents).isEmpty();
    }
} 