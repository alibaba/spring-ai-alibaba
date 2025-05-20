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
package com.alibaba.cloud.ai.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Title Document retrieval advisor.<br>
 * Description Document retrieval advisor.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */
public class DocumentRetrievalAdvisor implements CallAdvisor, StreamAdvisor {

	private static final String DEFAULT_USER_TEXT_ADVISE = """
			Context information is below.
			---------------------
			{question_answer_context}
			---------------------
			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.
			""";

	private static final int DEFAULT_ORDER = 0;

	public static String RETRIEVED_DOCUMENTS = "question_answer_context";

	private final DocumentRetriever retriever;

	private final String userTextAdvise;

	private final boolean protectFromBlocking;

	private final int order;

	public DocumentRetrievalAdvisor(DocumentRetriever retriever) {
		this(retriever, DEFAULT_USER_TEXT_ADVISE);
	}

	public DocumentRetrievalAdvisor(DocumentRetriever retriever, String userTextAdvise) {
		this(retriever, userTextAdvise, true);
	}

	public DocumentRetrievalAdvisor(DocumentRetriever retriever, String userTextAdvise, boolean protectFromBlocking) {
		this(retriever, userTextAdvise, protectFromBlocking, DEFAULT_ORDER);
	}

	public DocumentRetrievalAdvisor(DocumentRetriever retriever, String userTextAdvise, boolean protectFromBlocking,
			int order) {
		this.retriever = retriever;
		this.userTextAdvise = userTextAdvise;
		this.protectFromBlocking = protectFromBlocking;
		this.order = order;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

		chatClientRequest = this.before(chatClientRequest);

		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

		return this.after(chatClientResponse);
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		// This can be executed by both blocking and non-blocking Threads
		// E.g. a command line or Tomcat blocking Thread implementation
		// or by a WebFlux dispatch in a non-blocking manner.
		Flux<ChatClientResponse> advisedResponses = (this.protectFromBlocking) ?
		// @formatter:off
				Mono.just(chatClientRequest)
						.publishOn(Schedulers.boundedElastic())
						.map(this::before)
						.flatMapMany(streamAdvisorChain::nextStream)
				: streamAdvisorChain.nextStream(this.before(chatClientRequest));
		// @formatter:on

		return advisedResponses.map(ar -> {
			if (onFinishReason().test(ar)) {
				ar = after(ar);
			}
			return ar;
		});
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	private ChatClientRequest before(ChatClientRequest request) {
		// TODO 需要补充新版本实现
		return request;
	}

	private ChatClientResponse after(ChatClientResponse advisedResponse) {
		ChatResponseMetadata.Builder metadataBuilder = ChatResponseMetadata.builder();
		metadataBuilder.keyValue(RETRIEVED_DOCUMENTS, advisedResponse.context().get(RETRIEVED_DOCUMENTS));

		ChatResponseMetadata metadata = advisedResponse.chatResponse().getMetadata();
		if (metadata != null) {
			metadataBuilder.id(metadata.getId());
			metadataBuilder.model(metadata.getModel());
			metadataBuilder.usage(metadata.getUsage());
			metadataBuilder.promptMetadata(metadata.getPromptMetadata());
			metadataBuilder.rateLimit(metadata.getRateLimit());

			Set<Map.Entry<String, Object>> entries = metadata.entrySet();
			for (Map.Entry<String, Object> entry : entries) {
				metadataBuilder.keyValue(entry.getKey(), entry.getValue());
			}
		}

		ChatResponse chatResponse = new ChatResponse(advisedResponse.chatResponse().getResults(),
				metadataBuilder.build());
		return new ChatClientResponse(chatResponse, advisedResponse.context());
	}

	/**
	 * Controls whether {@link DocumentRetrievalAdvisor#after} should be executed.<br />
	 * Called only on Flux elements that contain a finish reason. Usually the last element
	 * in the Flux. The response advisor can modify the elements before they are returned
	 * to the client.<br />
	 */
	private Predicate<ChatClientResponse> onFinishReason() {

		return (advisedResponse) -> advisedResponse.chatResponse()
			.getResults()
			.stream()
			.anyMatch(result -> result != null && result.getMetadata() != null
					&& StringUtils.hasText(result.getMetadata().getFinishReason()));
	}

}
