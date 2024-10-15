/*
 * Copyright 2024 the original author or authors.
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

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentRetriever;
import org.springframework.ai.model.Content;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Title Document retrieval advisor.<br>
 * Description Document retrieval advisor.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class DocumentRetrievalAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

	private static final String DEFAULT_USER_TEXT_ADVISE = """
			请记住以下材料，他们可能对回答问题有帮助。
			---------------------
			{documents}
			---------------------
			""";

	private static final int DEFAULT_ORDER = 0;

	public static String RETRIEVED_DOCUMENTS = "documents";

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
	public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

		advisedRequest = this.before(advisedRequest);

		AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

		return this.after(advisedResponse);
	}

	@Override
	public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {

		// This can be executed by both blocking and non-blocking Threads
		// E.g. a command line or Tomcat blocking Thread implementation
		// or by a WebFlux dispatch in a non-blocking manner.
		Flux<AdvisedResponse> advisedResponses = (this.protectFromBlocking) ?
		// @formatter:off
				Mono.just(advisedRequest)
						.publishOn(Schedulers.boundedElastic())
						.map(this::before)
						.flatMapMany(request -> chain.nextAroundStream(request))
				: chain.nextAroundStream(this.before(advisedRequest));
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

	private AdvisedRequest before(AdvisedRequest request) {

		var context = new HashMap<>(request.adviseContext());

		List<Document> documents = retriever.retrieve(request.userText());

		context.put(RETRIEVED_DOCUMENTS, documents);

		String documentContext = documents.stream()
			.map(Content::getContent)
			.collect(Collectors.joining(System.lineSeparator()));

		Map<String, Object> advisedUserParams = new HashMap<>(request.userParams());
		advisedUserParams.put(RETRIEVED_DOCUMENTS, documentContext);

		return AdvisedRequest.from(request)
			.withSystemText(this.userTextAdvise)
			.withSystemParams(advisedUserParams)
			.withUserText(request.userText())
			.withAdviseContext(context)
			.build();
	}

	private AdvisedResponse after(AdvisedResponse advisedResponse) {
		ChatResponse.Builder chatResponseBuilder = ChatResponse.builder()
			.from(advisedResponse.response())
			.withMetadata(RETRIEVED_DOCUMENTS, advisedResponse.adviseContext().get(RETRIEVED_DOCUMENTS));
		return new AdvisedResponse(chatResponseBuilder.build(), advisedResponse.adviseContext());
	}

	/**
	 * Controls whether {@link DocumentRetrievalAdvisor#after(AdvisedResponse)} should be
	 * executed.<br />
	 * Called only on Flux elements that contain a finish reason. Usually the last element
	 * in the Flux. The response advisor can modify the elements before they are returned
	 * to the client.<br />
	 * Inspired by
	 * {@link org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor}.
	 */
	private Predicate<AdvisedResponse> onFinishReason() {

		return (advisedResponse) -> advisedResponse.response()
			.getResults()
			.stream()
			.filter(result -> result != null && result.getMetadata() != null
					&& StringUtils.hasText(result.getMetadata().getFinishReason()))
			.findFirst()
			.isPresent();
	}

}
