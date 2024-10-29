/*
 * Copyright 2023-2024 the original author or authors.
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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentRetriever;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Title Document retrieval advisor.<br>
 * Description Document retrieval advisor.<br>
 *
 * @author yuanci.ytb
 * @since 2024/8/16 11:29
 */

public class DashScopeDocumentRetrievalAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

	private static final Pattern RAG_REFERENCE_PATTERN = Pattern.compile("<ref>(.*?)</ref>");

	private static final Pattern RAG_REFERENCE_INNER_PATTERN = Pattern.compile("\\[([0-9]+)(?:[,，]?([0-9]+))*]");

	private static final String DEFAULT_USER_TEXT_ADVISE = """
			# 知识库
			请记住以下材料，他们可能对回答问题有帮助。
			---------------------
			$$材料:
			{question_answer_context}
			---------------------
			你只能结合上下文和历史消息记录回答问题，如果答案不在上下文中，请告知用户您无法回答该问题。
			""";

	private static final int DEFAULT_ORDER = 0;

	public static String RETRIEVED_DOCUMENTS = "question_answer_context";

	private final DocumentRetriever retriever;

	private final String userTextAdvise;

	private final boolean enableReference;

	private final boolean protectFromBlocking;

	private final int order;

	public DashScopeDocumentRetrievalAdvisor(DocumentRetriever retriever, boolean enableReference) {
		this(retriever, DEFAULT_USER_TEXT_ADVISE, enableReference);
	}

	public DashScopeDocumentRetrievalAdvisor(DocumentRetriever retriever, String userTextAdvise,
			boolean enableReference) {
		this(retriever, userTextAdvise, enableReference, true);
	}

	public DashScopeDocumentRetrievalAdvisor(DocumentRetriever retriever, String userTextAdvise,
			boolean enableReference, boolean protectFromBlocking) {
		this(retriever, userTextAdvise, enableReference, protectFromBlocking, DEFAULT_ORDER);
	}

	public DashScopeDocumentRetrievalAdvisor(DocumentRetriever retriever, String userTextAdvise,
			boolean enableReference, boolean protectFromBlocking, int order) {
		this.retriever = retriever;
		this.userTextAdvise = userTextAdvise;
		this.enableReference = enableReference;
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
						.flatMapMany(chain::nextAroundStream)
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

		Map<String, Document> documentMap = new HashMap<>();
		StringBuffer documentContext = new StringBuffer();
		for (int i = 0; i < documents.size(); i++) {
			Document document = documents.get(i);
			String indexId = String.format("[%d]", i + 1);
			String docInfo = String.format("""
					%s 【文档名】%s
					【标题】%s
					【正文】%s
					""", indexId, document.getMetadata().get("doc_name"), document.getMetadata().get("title"),
					document.getContent());

			documentContext.append(docInfo);
			documentContext.append(System.lineSeparator());

			document.getMetadata().put("index_id", i);
			documentMap.put(indexId, document);
		}

		context.put(RETRIEVED_DOCUMENTS, documentMap);

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
		if (!enableReference) {
			return advisedResponse;
		}

		var response = advisedResponse.response();
		var context = advisedResponse.adviseContext();
		ChatCompletionFinishReason finishReason = ChatCompletionFinishReason
			.valueOf(response.getResult().getMetadata().getFinishReason());
		if (finishReason == ChatCompletionFinishReason.NULL) {
			String fullContent = context.getOrDefault("full_content", "").toString()
					+ response.getResult().getOutput().getContent();
			context.put("full_content", fullContent);
			return advisedResponse;
		}

		String content = context.getOrDefault("full_content", "").toString();
		if ("".equalsIgnoreCase(content)) {
			content = response.getResult().getOutput().getContent();
		}

		Map<String, Document> documentMap = (Map<String, Document>) context.get(RETRIEVED_DOCUMENTS);
		List<Document> referencedDocuments = new ArrayList<>();

		Matcher refMatcher = RAG_REFERENCE_PATTERN.matcher(content);
		while (refMatcher.find()) {
			String refContent = refMatcher.group();
			Matcher numberMatcher = RAG_REFERENCE_INNER_PATTERN.matcher(refContent);

			while (numberMatcher.find()) {
				for (int i = 1; i <= numberMatcher.groupCount(); i++) {
					if (numberMatcher.group(i) != null) {
						String index = numberMatcher.group(i - 1);
						Document document = documentMap.get(index);
						referencedDocuments.add(document);
					}
				}
			}
		}

		// fix meta data loss issue since ChatResponse.from won't copy meta info like id,
		// model, usage, etc. This will
		// be changed once new version of spring ai core is updated.
		ChatResponseMetadata.Builder metadataBuilder = ChatResponseMetadata.builder();
		metadataBuilder.withKeyValue(RETRIEVED_DOCUMENTS, advisedResponse.adviseContext().get(RETRIEVED_DOCUMENTS));

		ChatResponseMetadata metadata = advisedResponse.response().getMetadata();
		if (metadata != null) {
			metadataBuilder.withId(metadata.getId());
			metadataBuilder.withModel(metadata.getModel());
			metadataBuilder.withUsage(metadata.getUsage());
			metadataBuilder.withPromptMetadata(metadata.getPromptMetadata());
			metadataBuilder.withRateLimit(metadata.getRateLimit());

			Set<Map.Entry<String, Object>> entries = metadata.entrySet();
			for (Map.Entry<String, Object> entry : entries) {
				metadataBuilder.withKeyValue(entry.getKey(), entry.getValue());
			}
		}

		ChatResponse chatResponse = new ChatResponse(advisedResponse.response().getResults(), metadataBuilder.build());
		return new AdvisedResponse(chatResponse, context);
	}

	/**
	 * Controls whether {@link DashScopeDocumentRetrievalAdvisor#after(AdvisedResponse)}
	 * should be executed.<br />
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
			.anyMatch(result -> result != null && result.getMetadata() != null
					&& StringUtils.hasText(result.getMetadata().getFinishReason()));
	}

}
