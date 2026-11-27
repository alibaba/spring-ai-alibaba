/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.node;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeRetrievalNodeContractTest {

	@Test
	void writesOriginalDocumentsWithoutOverwritingTheQuestion() throws Exception {
		var document = Document.builder().id("manual:23").text("Warranty policy")
			.metadata(Map.of("source", "manual")).score(0.8).build();
		var node = KnowledgeRetrievalNode.builder()
			.inputKey("question")
			.outputKey("evidence")
			.documentRetriever(query -> {
				assertThat(query.text()).isEqualTo("What is covered?");
				return List.of(document);
			})
			.augmentPrompt(false)
			.build();

		var state = new OverAllState(Map.of("question", "What is covered?"));
		assertThat(node.apply(state)).containsExactlyEntriesOf(Map.of("evidence", List.of(document)));
		assertThat(state.value("question", String.class)).contains("What is covered?");
	}

	@Test
	void rejectsAFilterThatTheSuppliedRetrieverWouldIgnore() {
		var filter = new FilterExpressionBuilder().eq("tenant", "a").build();
		assertThatThrownBy(() -> KnowledgeRetrievalNode.builder()
			.documentRetriever(query -> List.of())
			.filterExpression(filter)
			.build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("documentRetriever");
		assertThatThrownBy(() -> KnowledgeRetrievalNode.builder()
			.documentRetriever(query -> List.of())
			.inputKey("question")
			.outputKey("question")
			.augmentPrompt(false)
			.build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("keys must differ");
	}

	@Test
	void keepsLegacyPromptOutputAndVectorSearchOptions() throws Exception {
		var document = new Document("answer");
		var vectorStore = mock(VectorStore.class);
		var filter = new FilterExpressionBuilder().eq("tenant", "a").build();
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
			SearchRequest request = invocation.getArgument(0);
			assertThat(request.getQuery()).isEqualTo("question");
			assertThat(request.getTopK()).isEqualTo(2);
			assertThat(request.getSimilarityThreshold()).isEqualTo(0.5);
			assertThat(request.getFilterExpression()).isEqualTo(filter);
			return List.of(document);
		});
		var node = KnowledgeRetrievalNode.builder()
			.inputKey("question")
			.topK(5)
			.topKKey("top_k")
			.similarityThreshold(0.5)
			.filterExpressionKey("filter")
			.vectorStore(vectorStore)
			.build();

		assertThat(node.apply(new OverAllState(Map.of("question", "question", "top_k", 2, "filter", filter))))
			.containsEntry("question", "questionDocument: \n\nanswer\n")
			.containsEntry("output", List.of(document));
	}

	@Test
	void isolatesConcurrentAndFailedInvocations() throws Exception {
		var vectorStore = mock(VectorStore.class);
		var firstSearch = new CountDownLatch(1);
		var releaseFirstSearch = new CountDownLatch(1);
		var tenantA = new FilterExpressionBuilder().eq("tenant", "a").build();
		var tenantB = new FilterExpressionBuilder().eq("tenant", "b").build();
		var failure = new IllegalStateException("retriever unavailable");
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
			SearchRequest request = invocation.getArgument(0);
			return switch (request.getQuery()) {
				case "first" -> {
					assertThat(request.getFilterExpression()).isEqualTo(tenantA);
					firstSearch.countDown();
					assertThat(releaseFirstSearch.await(5, TimeUnit.SECONDS)).isTrue();
					yield List.of(new Document("first result"));
				}
				case "second" -> {
					assertThat(request.getFilterExpression()).isEqualTo(tenantB);
					throw failure;
				}
				case "default" -> {
					assertThat(request.getFilterExpression()).isNull();
					yield List.of();
				}
				default -> throw new AssertionError("Unexpected query " + request.getQuery());
			};
		});
		var node = KnowledgeRetrievalNode.builder()
			.inputKey("question")
			.userPrompt("default")
			.filterExpressionKey("filter")
			.vectorStore(vectorStore)
			.build();
		var executor = Executors.newSingleThreadExecutor();
		try {
			var first = executor.submit(() -> node.apply(new OverAllState(Map.of("question", "first", "filter", tenantA))));
			assertThat(firstSearch.await(5, TimeUnit.SECONDS)).isTrue();
			assertThatThrownBy(() -> node.apply(new OverAllState(Map.of("question", "second", "filter", tenantB))))
				.isSameAs(failure);
			releaseFirstSearch.countDown();
			assertThat(first.get(5, TimeUnit.SECONDS)).containsEntry("question", "firstDocument: \n\nfirst result\n");
			assertThat(node.apply(new OverAllState(Map.of())))
				.containsEntry("question", "default")
				.containsEntry("output", List.of());
		}
		finally {
			releaseFirstSearch.countDown();
			executor.shutdownNow();
		}
	}

}
