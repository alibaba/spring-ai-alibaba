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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.DeleteCollectionDataRequest;
import com.aliyun.gpdb20160503.models.DeleteCollectionDataResponse;
import com.aliyun.gpdb20160503.models.DescribeCollectionResponse;
import com.aliyun.gpdb20160503.models.DescribeNamespaceResponse;
import com.aliyun.gpdb20160503.models.InitVectorDatabaseResponse;
import com.aliyun.gpdb20160503.models.QueryCollectionDataRequest;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponse;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponseBody;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatches;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatchesMatch;
import com.aliyun.gpdb20160503.models.UpsertCollectionDataRequest;
import com.aliyun.gpdb20160503.models.UpsertCollectionDataRequest.UpsertCollectionDataRequestRows;
import com.aliyun.gpdb20160503.models.UpsertCollectionDataResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AnalyticDbVectorStoreAutoConfiguration}.
 *
 * @author saladday
 */
class AnalyticDbVectorStoreAutoConfigurationTests {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

	@Test
	void doAddEmbedsDocumentsAndCallsUpsert() throws Exception {
		Client client = baseClientMock();
		EmbeddingModel embeddingModel = mockEmbeddingModel();
		when(embeddingModel.embed(any(), any(), any())).thenReturn(List.of(new float[] { 1.0f, 2.0f }));
		when(client.upsertCollectionData(any())).thenReturn(new UpsertCollectionDataResponse());

		AnalyticDbVectorStore vectorStore = createVectorStore(client, embeddingModel);
		vectorStore.afterPropertiesSet();

		Document document = new Document("text body", Map.of("docId", "doc-123", "topic", "news"));

		vectorStore.doAdd(List.of(document));

		verify(embeddingModel).embed(any(), any(), any());

		ArgumentCaptor<UpsertCollectionDataRequest> captor = ArgumentCaptor.forClass(UpsertCollectionDataRequest.class);
		verify(client).upsertCollectionData(captor.capture());
		UpsertCollectionDataRequest request = captor.getValue();
		assertThat(request.getRows()).hasSize(1);
		UpsertCollectionDataRequestRows row = request.getRows().get(0);
		assertThat(row.getMetadata()).containsEntry("refDocId", "doc-123").containsEntry("content", "text body");
		Map<String, Object> storedMetadata = OBJECT_MAPPER.readValue(row.getMetadata().get("metadata"),
				new TypeReference<Map<String, Object>>() {
				});
		assertThat(storedMetadata).containsEntry("topic", "news");
		assertThat(row.getVector().stream().map(Number::doubleValue).collect(Collectors.toList()))
			.containsExactly(1.0, 2.0);
	}

	@Test
	void doSimilaritySearchReturnsDocumentsFromClientResponse() throws Exception {
		Client client = baseClientMock();
		EmbeddingModel embeddingModel = mockEmbeddingModel();
		AnalyticDbVectorStore vectorStore = createVectorStore(client, embeddingModel);
		vectorStore.afterPropertiesSet();

		QueryCollectionDataResponse response = new QueryCollectionDataResponse().setBody(
				new QueryCollectionDataResponseBody()
					.setMatches(new QueryCollectionDataResponseBodyMatches()
						.setMatch(List.of(new QueryCollectionDataResponseBodyMatchesMatch()
							.setScore(0.9)
							.setMetadata(Map.of("content", "sample content", "metadata", "{\"topic\":\"demo\"}"))))));
		when(client.queryCollectionData(any())).thenReturn(response);

		SearchRequest request = SearchRequest.builder().query("question").topK(5).similarityThreshold(0.5).build();

		List<Document> documents = vectorStore.doSimilaritySearch(request);

		assertThat(documents).hasSize(1);
		Document document = documents.get(0);
		assertThat(document.getText()).isEqualTo("sample content");
		assertThat(document.getMetadata()).containsEntry("topic", "demo");

		ArgumentCaptor<QueryCollectionDataRequest> captor = ArgumentCaptor.forClass(QueryCollectionDataRequest.class);
		verify(client).queryCollectionData(captor.capture());
		QueryCollectionDataRequest capturedRequest = captor.getValue();
		assertThat(capturedRequest.getTopK()).isEqualTo(5L);
		assertThat(capturedRequest.getContent()).isEqualTo("question");
	}

	@Test
	void doDeleteByIdsBuildsAliyunFilter() throws Exception {
		Client client = baseClientMock();
		EmbeddingModel embeddingModel = mockEmbeddingModel();
		AnalyticDbVectorStore vectorStore = createVectorStore(client, embeddingModel);
		vectorStore.afterPropertiesSet();

		vectorStore.doDelete(List.of("doc-1", "doc-2"));

		ArgumentCaptor<DeleteCollectionDataRequest> captor = ArgumentCaptor.forClass(DeleteCollectionDataRequest.class);
		verify(client).deleteCollectionData(captor.capture());
		assertThat(captor.getValue().getCollectionDataFilter()).isEqualTo("refDocId IN ('doc-1', 'doc-2')");
	}

	@Test
	void doDeleteWithFilterExpressionUsesConverter() throws Exception {
		Client client = baseClientMock();
		EmbeddingModel embeddingModel = mockEmbeddingModel();
		when(client.deleteCollectionData(any())).thenReturn(new DeleteCollectionDataResponse());

		AnalyticDbVectorStore vectorStore = createVectorStore(client, embeddingModel);
		vectorStore.afterPropertiesSet();

		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression filterExpression = builder
			.and(builder.eq("status", "active"), builder.gte("score", 0.8))
			.build();

		vectorStore.doDelete(filterExpression);

		ArgumentCaptor<DeleteCollectionDataRequest> captor = ArgumentCaptor.forClass(DeleteCollectionDataRequest.class);
		verify(client, times(1)).deleteCollectionData(captor.capture());
		assertThat(captor.getValue().getCollectionDataFilter())
			.isEqualTo("$.status = \"active\" && $.score >= 0.8");
	}

	@Test
	void similaritySearchWithFilterPopulatesRequestFields() throws Exception {
		Client client = baseClientMock();
		EmbeddingModel embeddingModel = mockEmbeddingModel();
		AnalyticDbVectorStore vectorStore = createVectorStore(client, embeddingModel);
		vectorStore.afterPropertiesSet();

		QueryCollectionDataResponse emptyResponse = new QueryCollectionDataResponse().setBody(
				new QueryCollectionDataResponseBody()
					.setMatches(new QueryCollectionDataResponseBodyMatches().setMatch(List.of())));
		when(client.queryCollectionData(any())).thenReturn(emptyResponse);

		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression expression = builder.or(builder.eq("category", "news"), builder.eq("category", "blog")).build();
		SearchRequest request = SearchRequest.builder().query("question").topK(3).filterExpression(expression).build();

		vectorStore.doSimilaritySearch(request);

		ArgumentCaptor<QueryCollectionDataRequest> captor = ArgumentCaptor.forClass(QueryCollectionDataRequest.class);
		verify(client).queryCollectionData(captor.capture());
		QueryCollectionDataRequest captured = captor.getValue();
		assertThat(captured.getFilter()).isEqualTo("$.category = \"news\" || $.category = \"blog\"");
		assertThat(captured.getIncludeValues()).isTrue();
	}

	@Test
	void similaritySearchAppliesScoreThreshold() throws Exception {
		Client client = baseClientMock();
		EmbeddingModel embeddingModel = mockEmbeddingModel();
		AnalyticDbVectorStore vectorStore = createVectorStore(client, embeddingModel);
		vectorStore.afterPropertiesSet();

		QueryCollectionDataResponseBodyMatchesMatch high = new QueryCollectionDataResponseBodyMatchesMatch()
			.setScore(0.9)
			.setMetadata(Map.of("content", "high", "metadata", "{\"rank\":\"high\"}"));
		QueryCollectionDataResponseBodyMatchesMatch low = new QueryCollectionDataResponseBodyMatchesMatch()
			.setScore(0.3)
			.setMetadata(Map.of("content", "low", "metadata", "{\"rank\":\"low\"}"));
		QueryCollectionDataResponse response = new QueryCollectionDataResponse().setBody(
				new QueryCollectionDataResponseBody()
					.setMatches(new QueryCollectionDataResponseBodyMatches().setMatch(List.of(high, low))));
		when(client.queryCollectionData(any())).thenReturn(response);

		SearchRequest request = SearchRequest.builder().query("question").topK(5).similarityThreshold(0.5).build();

		List<Document> documents = vectorStore.doSimilaritySearch(request);

		assertThat(documents).extracting(Document::getText).containsExactly("high");
	}

	@Test
	void doAddWithEmptyDocumentsSkipsClientInteraction() throws Exception {
		Client client = baseClientMock();
		EmbeddingModel embeddingModel = mockEmbeddingModel();
		AnalyticDbVectorStore vectorStore = createVectorStore(client, embeddingModel);
		vectorStore.afterPropertiesSet();

		vectorStore.doAdd(List.of());

		verify(embeddingModel, never()).embed(any(), any(), any());
		verify(client, never()).upsertCollectionData(any());
	}

	@Test
	void doAddWithNullDocumentsThrowsException() throws Exception {
		Client client = baseClientMock();
		EmbeddingModel embeddingModel = mockEmbeddingModel();
		AnalyticDbVectorStore vectorStore = createVectorStore(client, embeddingModel);
		vectorStore.afterPropertiesSet();

		assertThatThrownBy(() -> vectorStore.doAdd(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("should not be null");
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
		return baseClientMock();
	}

	private static Client baseClientMock() {
		Client client = Mockito.mock(Client.class);
		try {
			when(client.initVectorDatabase(any())).thenReturn(new InitVectorDatabaseResponse());
			when(client.describeNamespace(any())).thenReturn(new DescribeNamespaceResponse());
			when(client.describeCollection(any())).thenReturn(new DescribeCollectionResponse());
			when(client.deleteCollectionData(any())).thenReturn(new DeleteCollectionDataResponse());
			when(client.upsertCollectionData(any())).thenReturn(new UpsertCollectionDataResponse());
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

	private static AnalyticDbVectorStore createVectorStore(Client client, EmbeddingModel embeddingModel) {
		AnalyticDbConfig config = new AnalyticDbConfig().setAccessKeyId("ak")
			.setAccessKeySecret("sk")
			.setRegionId("cn-test-1")
			.setDbInstanceId("db-123")
			.setManagerAccount("manager")
			.setManagerAccountPassword("manager-pass")
			.setNamespace("default")
			.setNamespacePassword("ns-pass")
			.setMetrics("cosine");

		return AnalyticDbVectorStore.builder("test-collection", config, client, embeddingModel)
			.batchingStrategy(new TokenCountBatchingStrategy())
			.defaultTopK(5)
			.defaultSimilarityThreshold(0.2)
			.build();
	}

	private static String[] concat(String[] source, String... extra) {
		String[] result = new String[source.length + extra.length];
		System.arraycopy(source, 0, result, 0, source.length);
		System.arraycopy(extra, 0, result, source.length, extra.length);
		return result;
	}

}
