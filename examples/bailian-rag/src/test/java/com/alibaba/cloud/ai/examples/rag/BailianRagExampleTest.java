/*
 * Copyright 2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.rag;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BailianRagExampleTest {

	private static final String QUESTION = "How long is the warranty?";

	private static final String ANSWER = "The warranty lasts 12 months [E1].";

	private static final Document WARRANTY = Document.builder().id("warranty:1").text("Warranty lasts 12 months.")
		.metadata("doc_name", "Warranty handbook").build();

	private static final Document INVOICE = Document.builder().id("invoice:2").text("Invoice policy.").build();

	private static final DocumentRetriever RETRIEVER = query -> List
		.of(query.text().contains("warranty") ? WARRANTY : INVOICE);

	@Test
	void graphKeepsIndependentSearchHistoriesAndSkipsGenerationWithoutEvidence() throws Exception {
		var generations = new AtomicInteger();
		ChatModel model = prompt -> {
			generations.incrementAndGet();
			assertThat(prompt.getUserMessage().getText()).contains(QUESTION, "Warranty lasts 12 months.", "E1");
			return answer(ANSWER);
		};
		var graph = BailianRagExample.graph(RETRIEVER, query -> new Query("warranty duration"), ChatClient.create(model));

		var direct = BailianRagExample.runGraph(graph, QUESTION, QUESTION, "12 months");
		assertThat(direct.queries()).containsExactly(QUESTION);
		assertThat(direct.citations()).extracting(BailianRagExample.Evidence::chunkId).containsExactly("warranty:1");

		var rewritten = BailianRagExample.runGraph(graph, QUESTION, "invoice", "12 months");
		assertThat(rewritten.status()).isEqualTo("answered");
		assertThat(rewritten.queries()).containsExactly("invoice", "warranty duration");
		assertThat(rewritten.citations()).extracting(BailianRagExample.Evidence::source)
			.containsExactly("Warranty handbook");

		var absent = BailianRagExample.runGraph(graph, QUESTION, "invoice", "36 months");
		assertThat(absent.status()).isEqualTo("insufficient_evidence");
		assertThat(absent.queries()).containsExactly("invoice", "warranty duration");
		assertThat(absent.citations()).isEmpty();
		assertThat(generations.get()).isEqualTo(2);
	}

	@Test
	void agentLooksUpEvidenceForQuestionsAndSkipsSearchForGreetings() throws Exception {
		ChatModel model = new ChatModel() {

			@Override
			public ChatResponse call(Prompt prompt) {
				if ("Hello".equals(prompt.getUserMessage().getText())) {
					return answer("Hello!");
				}
				var toolResponse = prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
					.map(ToolResponseMessage.class::cast).findFirst();
				if (toolResponse.isPresent()) {
					String data = toolResponse.orElseThrow().getResponses().get(0).responseData();
					return answer(data.contains("warranty:1") ? ANSWER : "INSUFFICIENT_EVIDENCE");
				}
				assertThat(((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks()).hasSize(1);
				var call = new AssistantMessage.ToolCall("1", "function", "search_knowledge",
						"{\"query\":\"warranty duration\"}");
				return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(call)).build())));
			}

			@Override
			public ToolCallingChatOptions getOptions() {
				return ToolCallingChatOptions.builder().build();
			}

		};

		var positive = BailianRagExample.runAgent(RETRIEVER, model, QUESTION, "12 months");
		assertThat(positive.queries()).containsExactly("warranty duration");
		assertThat(positive.citations()).extracting(BailianRagExample.Evidence::chunkId).containsExactly("warranty:1");

		var absent = BailianRagExample.runAgent(RETRIEVER, model, QUESTION, "36 months");
		assertThat(absent.status()).isEqualTo("insufficient_evidence");
		assertThat(absent.queries()).containsExactly("warranty duration");
		assertThat(absent.citations()).isEmpty();

		assertThat(BailianRagExample.runAgent(RETRIEVER, model, "Hello", "12 months").queries()).isEmpty();
	}

	@Test
	void rejectsReferencesThatNeverCameFromTheRetriever() throws Exception {
		ChatModel model = prompt -> answer("The answer cites an invented source [E99].");
		var graph = BailianRagExample.graph(RETRIEVER, query -> query, ChatClient.create(model));

		assertThatThrownBy(() -> BailianRagExample.runGraph(graph, QUESTION, QUESTION, "12 months"))
			.isInstanceOf(IllegalStateException.class).hasMessageContaining("outside the retrieved evidence");
	}

	private static ChatResponse answer(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

}
