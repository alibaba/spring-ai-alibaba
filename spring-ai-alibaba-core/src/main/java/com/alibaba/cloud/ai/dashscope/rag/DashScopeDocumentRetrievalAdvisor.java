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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
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
			指令：您需要仅使用提供的搜索文档为给定问题写出高质量的答案，并正确引用它们。 引用多个搜索结果时，请使用<ref>[编号]</ref>格式，注意确保这些引用直接有助于解答问题，编号需与材料原始编号一致且唯一。请注意，每个句子中必须至少引用一个文档。换句话说，你禁止在没有引用任何文献的情况下写句子。此外，您应该在每个句子中添加引用符号，注意在句号之前。

			对于每个问题按照下面的推理步骤得到带引用的答案：

			步骤1：我判断文档1和文档2与问题相关。

			步骤2：根据文档1，我写了一个回答陈述并引用了该文档。

			步骤3：根据文档2，我写一个答案声明并引用该文档。

			步骤4：我将以上两个答案语句进行合并、排序和连接，以获得流畅连贯的答案。

			$$材料：
			[1] 【文档名】植物中的光合作用.pdf
			【标题】光合作用位置
			【正文】光合作用主要在叶绿体中进行，涉及光能到化学能的转化。
			[2] 【文档名】光合作用.pdf
			【标题】光合作用转化
			【正文】光合作用是利用阳光将CO2和H2O转化为氧气和葡萄糖的过程。

			$$材料:
			{question_answer_context}
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

		List<Document> documents = retriever.retrieve(new Query(request.userText()));

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
					document.getText());

			documentContext.append(docInfo);
			documentContext.append(System.lineSeparator());

			document.getMetadata().put("index_id", i);
			documentMap.put(indexId, document);
		}

		context.put(RETRIEVED_DOCUMENTS, documentMap);

		Map<String, Object> advisedUserParams = new HashMap<>(request.userParams());
		advisedUserParams.put(RETRIEVED_DOCUMENTS, documentContext);

		return AdvisedRequest.from(request)
			.userText(request.userText() + System.lineSeparator() + this.userTextAdvise)
			.userParams(advisedUserParams)
			.adviseContext(context)
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
					+ response.getResult().getOutput().getText();
			context.put("full_content", fullContent);
			return advisedResponse;
		}

		String content = context.getOrDefault("full_content", "").toString();
		if ("".equalsIgnoreCase(content)) {
			content = response.getResult().getOutput().getText();
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
		metadataBuilder.keyValue(RETRIEVED_DOCUMENTS, advisedResponse.adviseContext().get(RETRIEVED_DOCUMENTS));

		ChatResponseMetadata metadata = advisedResponse.response().getMetadata();
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
