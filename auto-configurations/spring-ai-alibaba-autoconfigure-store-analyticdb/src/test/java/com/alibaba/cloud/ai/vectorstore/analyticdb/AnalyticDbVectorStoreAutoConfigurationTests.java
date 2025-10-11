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

import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.DescribeCollectionResponse;
import com.aliyun.gpdb20160503.models.DescribeNamespaceResponse;
import com.aliyun.gpdb20160503.models.InitVectorDatabaseResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AnalyticDbVectorStoreAutoConfiguration}.
 *
 * @author Alibaba Cloud
 */
class AnalyticDbVectorStoreAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withBean(EmbeddingModel.class, AnalyticDbVectorStoreAutoConfigurationTests::mockEmbeddingModel)
		.withBean(Client.class, AnalyticDbVectorStoreAutoConfigurationTests::mockClient)
		.withConfiguration(AutoConfigurations.of(AnalyticDbVectorStoreAutoConfiguration.class));

	@Test
	void analyticDbBeansNotCreatedWhenTypeMissing() {
		this.contextRunner.withPropertyValues(basicProperties()).run((context) -> {
			assertThat(context).doesNotHaveBean(AnalyticDbVectorStore.class);
		});
	}

	@Test
	void analyticDbBeansCreatedWhenTypeMatches() {
		this.contextRunner.withPropertyValues(concat(basicProperties(), "spring.ai.vectorstore.type=analyticdb"))
			.run((context) -> {
				assertThat(context).hasSingleBean(AnalyticDbVectorStore.class);
				Client client = context.getBean(Client.class);
				verify(client, times(1)).initVectorDatabase(any());
				verify(client, times(1)).describeNamespace(any());
				verify(client, times(1)).describeCollection(any());
			});
	}

	@Test
	void legacyEnabledPropertyStillActivatesAutoConfiguration() {
		this.contextRunner
			.withPropertyValues(
					concat(basicProperties(), "spring.ai.vectorstore.analytic.enabled=true"))
			.run((context) -> assertThat(context).hasSingleBean(AnalyticDbVectorStore.class));
	}

	private static String[] basicProperties() {
		return new String[] { "spring.ai.vectorstore.analytic.collect-name=test",
				"spring.ai.vectorstore.analytic.access-key-id=ak", "spring.ai.vectorstore.analytic.access-key-secret=sk",
				"spring.ai.vectorstore.analytic.region-id=cn-test-1",
				"spring.ai.vectorstore.analytic.db-instance-id=db-123",
				"spring.ai.vectorstore.analytic.manager-account=manager",
				"spring.ai.vectorstore.analytic.manager-account-password=manager-pass",
				"spring.ai.vectorstore.analytic.namespace=default",
				"spring.ai.vectorstore.analytic.namespace-password=ns-pass" };
	}

	private static Client mockClient() {
		Client client = Mockito.mock(Client.class);
		try {
			when(client.initVectorDatabase(any())).thenReturn(new InitVectorDatabaseResponse());
			when(client.describeNamespace(any())).thenReturn(new DescribeNamespaceResponse());
			when(client.describeCollection(any())).thenReturn(new DescribeCollectionResponse());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return client;
	}

	private static EmbeddingModel mockEmbeddingModel() {
		EmbeddingModel embeddingModel = Mockito.mock(EmbeddingModel.class);
		when(embeddingModel.dimensions()).thenReturn(1536);
		return embeddingModel;
	}

	private static String[] concat(String[] source, String... extra) {
		String[] result = new String[source.length + extra.length];
		System.arraycopy(source, 0, result, 0, source.length);
		System.arraycopy(extra, 0, result, source.length, extra.length);
		return result;
	}

}
