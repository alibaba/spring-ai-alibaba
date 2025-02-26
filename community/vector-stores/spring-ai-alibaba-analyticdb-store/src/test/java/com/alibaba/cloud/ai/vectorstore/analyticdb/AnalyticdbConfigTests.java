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
package com.alibaba.cloud.ai.vectorstore.analyticdb;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for AnalyticdbConfig. Tests cover constructors, getters/setters,
 * and client parameter generation.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */
class AnalyticdbConfigTests {

    // 测试常量
    private static final String TEST_ACCESS_KEY_ID = "test-access-key-id";
    private static final String TEST_ACCESS_KEY_SECRET = "test-access-key-secret";
    private static final String TEST_REGION_ID = "test-region";
    private static final String TEST_DB_INSTANCE_ID = "test-db-instance";
    private static final String TEST_MANAGER_ACCOUNT = "test-manager";
    private static final String TEST_MANAGER_PASSWORD = "test-manager-password";
    private static final String TEST_NAMESPACE = "test-namespace";
    private static final String TEST_NAMESPACE_PASSWORD = "test-namespace-password";
    private static final String TEST_METRICS = "euclidean";
    private static final Integer TEST_READ_TIMEOUT = 30000;
    private static final Long TEST_EMBEDDING_DIMENSION = 768L;
    private static final String TEST_USER_AGENT = "test-agent";

    @Test
    void testDefaultConstructor() {
        // 测试默认构造函数和默认值
        AnalyticdbConfig config = new AnalyticdbConfig();

        // 验证默认值
        assertThat(config.getMetrics()).isEqualTo("cosine");
        assertThat(config.getReadTimeout()).isEqualTo(60000);
        assertThat(config.getEmbeddingDimension()).isEqualTo(1536L);
        assertThat(config.getUserAgent()).isEqualTo("index");
    }

    @Test
    void testParameterizedConstructor() {
        // 测试带参数的构造函数
        AnalyticdbConfig config = new AnalyticdbConfig(
                TEST_ACCESS_KEY_ID,
                TEST_ACCESS_KEY_SECRET,
                TEST_REGION_ID,
                TEST_DB_INSTANCE_ID,
                TEST_MANAGER_ACCOUNT,
                TEST_MANAGER_PASSWORD,
                TEST_NAMESPACE,
                TEST_NAMESPACE_PASSWORD,
                TEST_METRICS,
                TEST_READ_TIMEOUT,
                TEST_EMBEDDING_DIMENSION,
                TEST_USER_AGENT);

        // 验证所有字段都被正确设置
        assertThat(config.getAccessKeyId()).isEqualTo(TEST_ACCESS_KEY_ID);
        assertThat(config.getAccessKeySecret()).isEqualTo(TEST_ACCESS_KEY_SECRET);
        assertThat(config.getRegionId()).isEqualTo(TEST_REGION_ID);
        assertThat(config.getDBInstanceId()).isEqualTo(TEST_DB_INSTANCE_ID);
        assertThat(config.getManagerAccount()).isEqualTo(TEST_MANAGER_ACCOUNT);
        assertThat(config.getManagerAccountPassword()).isEqualTo(TEST_MANAGER_PASSWORD);
        assertThat(config.getNamespace()).isEqualTo(TEST_NAMESPACE);
        assertThat(config.getNamespacePassword()).isEqualTo(TEST_NAMESPACE_PASSWORD);
        assertThat(config.getMetrics()).isEqualTo(TEST_METRICS);
        assertThat(config.getReadTimeout()).isEqualTo(TEST_READ_TIMEOUT);
        assertThat(config.getEmbeddingDimension()).isEqualTo(TEST_EMBEDDING_DIMENSION);
        assertThat(config.getUserAgent()).isEqualTo(TEST_USER_AGENT);
    }

    @Test
    void testSettersAndGetters() {
        // 测试 setter 和 getter 方法
        AnalyticdbConfig config = new AnalyticdbConfig();

        // 使用 setter 方法设置值
        config.setAccessKeyId(TEST_ACCESS_KEY_ID)
                .setAccessKeySecret(TEST_ACCESS_KEY_SECRET)
                .setRegionId(TEST_REGION_ID)
                .setDBInstanceId(TEST_DB_INSTANCE_ID)
                .setManagerAccount(TEST_MANAGER_ACCOUNT)
                .setManagerAccountPassword(TEST_MANAGER_PASSWORD)
                .setNamespace(TEST_NAMESPACE)
                .setNamespacePassword(TEST_NAMESPACE_PASSWORD)
                .setMetrics(TEST_METRICS)
                .setReadTimeout(TEST_READ_TIMEOUT)
                .setEmbeddingDimension(TEST_EMBEDDING_DIMENSION)
                .setUserAgent(TEST_USER_AGENT);

        // 验证所有字段都被正确设置
        assertThat(config.getAccessKeyId()).isEqualTo(TEST_ACCESS_KEY_ID);
        assertThat(config.getAccessKeySecret()).isEqualTo(TEST_ACCESS_KEY_SECRET);
        assertThat(config.getRegionId()).isEqualTo(TEST_REGION_ID);
        assertThat(config.getDBInstanceId()).isEqualTo(TEST_DB_INSTANCE_ID);
        assertThat(config.getManagerAccount()).isEqualTo(TEST_MANAGER_ACCOUNT);
        assertThat(config.getManagerAccountPassword()).isEqualTo(TEST_MANAGER_PASSWORD);
        assertThat(config.getNamespace()).isEqualTo(TEST_NAMESPACE);
        assertThat(config.getNamespacePassword()).isEqualTo(TEST_NAMESPACE_PASSWORD);
        assertThat(config.getMetrics()).isEqualTo(TEST_METRICS);
        assertThat(config.getReadTimeout()).isEqualTo(TEST_READ_TIMEOUT);
        assertThat(config.getEmbeddingDimension()).isEqualTo(TEST_EMBEDDING_DIMENSION);
        assertThat(config.getUserAgent()).isEqualTo(TEST_USER_AGENT);
    }

    @Test
    void testToAnalyticdbClientParams() {
        // 测试生成客户端参数
        AnalyticdbConfig config = new AnalyticdbConfig();
        config.setAccessKeyId(TEST_ACCESS_KEY_ID)
                .setAccessKeySecret(TEST_ACCESS_KEY_SECRET)
                .setRegionId(TEST_REGION_ID)
                .setReadTimeout(TEST_READ_TIMEOUT)
                .setUserAgent(TEST_USER_AGENT);

        Map<String, Object> params = config.toAnalyticdbClientParams();

        // 验证参数映射
        assertThat(params).containsEntry("accessKeyId", TEST_ACCESS_KEY_ID);
        assertThat(params).containsEntry("accessKeySecret", TEST_ACCESS_KEY_SECRET);
        assertThat(params).containsEntry("regionId", TEST_REGION_ID);
        assertThat(params).containsEntry("readTimeout", TEST_READ_TIMEOUT);
        assertThat(params).containsEntry("userAgent", TEST_USER_AGENT);
        assertThat(params).hasSize(5); // 确保只包含这5个参数
    }

    @Test
    void testChainedSetters() {
        // 测试链式调用 setter 方法
        AnalyticdbConfig config = new AnalyticdbConfig()
                .setAccessKeyId(TEST_ACCESS_KEY_ID)
                .setAccessKeySecret(TEST_ACCESS_KEY_SECRET)
                .setRegionId(TEST_REGION_ID);

        // 验证链式调用结果
        assertThat(config.getAccessKeyId()).isEqualTo(TEST_ACCESS_KEY_ID);
        assertThat(config.getAccessKeySecret()).isEqualTo(TEST_ACCESS_KEY_SECRET);
        assertThat(config.getRegionId()).isEqualTo(TEST_REGION_ID);
    }
}