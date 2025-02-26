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
package com.alibaba.cloud.ai.dashscope.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.QueryFileResponseData;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.UploadLeaseResponse;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeDocumentCloudReader
 *
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeDocumentCloudReaderTests {

    private static final String TEST_CATEGORY_ID = "test-category";
    private static final String TEST_FILE_ID = "test-file-id";
    private static final String TEST_CONTENT = "Test content";
    private static final String TEST_FILE_NAME = "test.txt";
    private static final String TEST_FILE_TYPE = "txt";
    private static final long TEST_FILE_SIZE = 1024L;
    private static final String TEST_UPLOAD_TIME = "2024-01-01 00:00:00";

    @Mock
    private DashScopeApi dashScopeApi;

    @TempDir
    Path tempDir;

    private DashScopeDocumentCloudReader reader;
    private File testFile;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        // 创建测试文件
        testFile = tempDir.resolve("test.txt").toFile();
        Files.writeString(testFile.toPath(), TEST_CONTENT);

        DashScopeDocumentCloudReaderOptions options = new DashScopeDocumentCloudReaderOptions(TEST_CATEGORY_ID);
        reader = new DashScopeDocumentCloudReader(dashScopeApi, options);
    }

    @Test
    void testConstructorWithNonExistentFile() {
        // 测试使用不存在的文件路径创建实例
        String nonExistentPath = tempDir.resolve("non-existent.txt").toString();
        DashScopeDocumentCloudReaderOptions options = new DashScopeDocumentCloudReaderOptions(TEST_CATEGORY_ID);

        assertThatThrownBy(() -> reader.get(nonExistentPath))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not Exist");
    }

    @Test
    void testSuccessfulDocumentParsing() throws IOException {
        // 设置模拟行为
        mockSuccessfulUpload();
        mockSuccessfulParsing();

        // 执行测试
        List<Document> documents = reader.get(testFile.getAbsolutePath());

        // 验证结果
        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getText()).isEqualTo(TEST_CONTENT);
        assertThat(document.getMetadata()).containsEntry("file_id", TEST_FILE_ID);
    }

    @Test
    void testParseFailure() throws IOException {
        // 设置模拟行为
        mockSuccessfulUpload();
        mockFailedParsing();

        // 验证异常
        assertThatThrownBy(() -> reader.get(testFile.getAbsolutePath()))
                .isInstanceOf(DashScopeException.class)
                .hasMessageContaining("READER_PARSE_FILE_ERROR");
    }

    @Test
    void testPollingTimeout() throws IOException {
        // 设置模拟行为
        mockSuccessfulUpload();
        mockPollingTimeout();

        // 执行测试
        List<Document> documents = reader.get(testFile.getAbsolutePath());

        // 验证结果为空
        assertThat(documents).isEmpty();
    }

    private void mockSuccessfulUpload() {
        UploadLeaseResponse leaseResponse = new UploadLeaseResponse();
        when(dashScopeApi.uploadFile(any(), any())).thenReturn(TEST_FILE_ID);
    }

    private void mockSuccessfulParsing() {
        QueryFileResponseData successResponse = new QueryFileResponseData(
                TEST_CATEGORY_ID,
                TEST_FILE_ID,
                TEST_FILE_NAME,
                TEST_FILE_TYPE,
                TEST_FILE_SIZE,
                "PARSE_SUCCESS",
                TEST_UPLOAD_TIME);
        when(dashScopeApi.queryFileStatus(TEST_FILE_ID)).thenReturn(successResponse);
    }

    private void mockFailedParsing() {
        QueryFileResponseData failedResponse = new QueryFileResponseData(
                TEST_CATEGORY_ID,
                TEST_FILE_ID,
                TEST_FILE_NAME,
                TEST_FILE_TYPE,
                TEST_FILE_SIZE,
                "PARSE_FAILED",
                TEST_UPLOAD_TIME);
        when(dashScopeApi.queryFileStatus(TEST_FILE_ID)).thenReturn(failedResponse);
    }

    private void mockPollingTimeout() {
        QueryFileResponseData processingResponse = new QueryFileResponseData(
                TEST_CATEGORY_ID,
                TEST_FILE_ID,
                TEST_FILE_NAME,
                TEST_FILE_TYPE,
                TEST_FILE_SIZE,
                "PROCESSING",
                TEST_UPLOAD_TIME);
        when(dashScopeApi.queryFileStatus(TEST_FILE_ID)).thenReturn(processingResponse);
    }
}
