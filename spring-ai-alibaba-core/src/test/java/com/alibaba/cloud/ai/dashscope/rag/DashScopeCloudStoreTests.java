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
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test cases for DashScopeCloudStore.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeCloudStoreTests {

    @Mock
    private DashScopeApi dashScopeApi;

    private DashScopeCloudStore cloudStore;
    private DashScopeStoreOptions options;

    private static final String TEST_INDEX_NAME = "test-index";
    private static final String TEST_PIPELINE_ID = "test-pipeline-id";
    private static final String TEST_QUERY = "test query";

    @BeforeEach
    void setUp() {
        // 初始化 Mockito 注解
        MockitoAnnotations.openMocks(this);

        // 设置基本配置
        options = new DashScopeStoreOptions(TEST_INDEX_NAME);
        cloudStore = new DashScopeCloudStore(dashScopeApi, options);

        // 设置基本的 mock 行为
        when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(TEST_PIPELINE_ID);
    }

    @Test
    void testAddDocumentsWithNullList() {
        // 测试添加空文档列表
        assertThrows(DashScopeException.class, () -> cloudStore.add(null));
    }

    @Test
    void testAddDocumentsWithEmptyList() {
        // 测试添加空文档列表
        assertThrows(DashScopeException.class, () -> cloudStore.add(new ArrayList<>()));
    }

    @Test
    void testAddDocumentsSuccessfully() {
        // 创建测试文档
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        List<Document> documents = Arrays.asList(
                new Document("id1", "content1", metadata),
                new Document("id2", "content2", metadata));

        // 执行添加操作
        cloudStore.add(documents);

        // 验证 API 调用
        verify(dashScopeApi).upsertPipeline(eq(documents), eq(options));
    }

    @Test
    void testDeleteDocumentsWithNonExistentIndex() {
        // 模拟索引不存在的情况
        when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(null);

        // 测试删除文档
        List<String> ids = Arrays.asList("id1", "id2");
        assertThrows(DashScopeException.class, () -> cloudStore.delete(ids));
    }

    @Test
    void testDeleteDocumentsSuccessfully() {
        // 准备测试数据
        List<String> ids = Arrays.asList("id1", "id2");

        // 执行删除操作
        cloudStore.delete(ids);

        // 验证 API 调用
        verify(dashScopeApi).deletePipelineDocument(TEST_PIPELINE_ID, ids);
    }

    @Test
    void testSimilaritySearchWithNonExistentIndex() {
        // 模拟索引不存在的情况
        when(dashScopeApi.getPipelineIdByName(TEST_INDEX_NAME)).thenReturn(null);

        // 测试相似度搜索
        assertThrows(DashScopeException.class, () -> cloudStore.similaritySearch(TEST_QUERY));
    }

    @Test
    void testSimilaritySearchSuccessfully() {
        // 准备测试数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        List<Document> expectedResults = Arrays.asList(
                new Document("id1", "result1", metadata),
                new Document("id2", "result2", metadata));
        when(dashScopeApi.retriever(anyString(), anyString(), any())).thenReturn(expectedResults);

        // 执行搜索
        List<Document> results = cloudStore.similaritySearch(TEST_QUERY);

        // 验证结果
        assertThat(results).isEqualTo(expectedResults);
        verify(dashScopeApi).retriever(eq(TEST_PIPELINE_ID), eq(TEST_QUERY), any());
    }

    @Test
    void testSimilaritySearchWithSearchRequest() {
        // 准备测试数据
        SearchRequest request = SearchRequest.builder()
                .query(TEST_QUERY)
                .topK(5)
                .build();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");

        List<Document> expectedResults = Arrays.asList(
                new Document("id1", "result1", metadata),
                new Document("id2", "result2", metadata));
        when(dashScopeApi.retriever(anyString(), anyString(), any())).thenReturn(expectedResults);

        // 执行搜索
        List<Document> results = cloudStore.similaritySearch(request);

        // 验证结果
        assertThat(results).isEqualTo(expectedResults);
        verify(dashScopeApi).retriever(eq(TEST_PIPELINE_ID), eq(TEST_QUERY), any());
    }

    @Test
    void testGetName() {
        // 测试获取名称
        String name = cloudStore.getName();
        assertThat(name).isEqualTo("DashScopeCloudStore");
    }
}