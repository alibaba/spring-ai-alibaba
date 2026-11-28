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
package com.alibaba.cloud.ai.studio.core.rag.retriever;

import com.alibaba.cloud.ai.studio.core.model.llm.ModelFactory;
import com.alibaba.cloud.ai.studio.core.rag.vectorstore.VectorStoreFactory;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeBaseDocumentRetrieverTest {

	@Test
	void shouldPreserveCallerInterruptWhenRetrievalIsCancelled() throws Exception {
		KnowledgeBase knowledgeBase = new KnowledgeBase();
		knowledgeBase.setWorkspaceId("lesson-workspace");
		knowledgeBase.setSearchConfig(new FileSearchOptions());
		VectorStoreFactory factory = mock(VectorStoreFactory.class, RETURNS_DEEP_STUBS);
		VectorStore store = mock(VectorStore.class);
		when(factory.getVectorStoreService().getVectorStore(any())).thenReturn(store);
		CountDownLatch searching = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		when(store.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenAnswer(call -> {
			searching.countDown();
			release.await();
			return List.of();
		});
		KnowledgeBaseDocumentRetriever retriever = new KnowledgeBaseDocumentRetriever(List.of(knowledgeBase), factory,
				mock(ModelFactory.class), new FileSearchOptions());
		AtomicReference<Throwable> failure = new AtomicReference<>();
		AtomicBoolean interrupted = new AtomicBoolean();
		Thread caller = new Thread(() -> {
			try {
				retriever.retrieve(Query.builder().text("Find the experiment instructions").build());
			}
			catch (Throwable error) {
				failure.set(error);
				interrupted.set(Thread.currentThread().isInterrupted());
			}
		});
		caller.setDaemon(true);
		try {
			caller.start();
			assertThat(searching.await(5, TimeUnit.SECONDS)).isTrue();
			caller.interrupt();
			caller.join(5000);
			assertThat(caller.isAlive()).isFalse();
			assertThat(failure.get()).isInstanceOf(BizException.class).hasCauseInstanceOf(InterruptedException.class);
			assertThat(interrupted.get()).isTrue();
		}
		finally {
			release.countDown();
			caller.interrupt();
			caller.join(5000);
		}
	}

	@Test
	void shouldKeepWorkerFailuresSeparateFromCallerInterruption() {
		KnowledgeBase knowledgeBase = new KnowledgeBase();
		VectorStoreFactory factory = mock(VectorStoreFactory.class);
		when(factory.getVectorStoreService()).thenThrow(new IllegalStateException("vector store unavailable"));
		KnowledgeBaseDocumentRetriever retriever = new KnowledgeBaseDocumentRetriever(List.of(knowledgeBase), factory,
				mock(ModelFactory.class), new FileSearchOptions());
		assertThat(Thread.currentThread().isInterrupted()).isFalse();
		assertThatThrownBy(() -> retriever.retrieve(Query.builder().text("Find lesson notes").build()))
			.isInstanceOf(BizException.class)
			.hasCauseInstanceOf(java.util.concurrent.ExecutionException.class)
			.hasRootCauseInstanceOf(IllegalStateException.class);
		assertThat(Thread.currentThread().isInterrupted()).isFalse();
	}

}
