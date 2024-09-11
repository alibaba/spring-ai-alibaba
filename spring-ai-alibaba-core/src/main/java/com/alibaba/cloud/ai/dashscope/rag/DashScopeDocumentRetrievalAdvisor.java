/*
 * All rights Reserved, Designed By Alibaba Group Inc.
 * Copyright: Copyright(C) 1999-2024
 * Company  : Alibaba Group Inc.
 */
package com.alibaba.cloud.ai.dashscope.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.ChatCompletionFinishReason;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentRetriever;
import org.springframework.ai.model.Content;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Title Document retrieval advisor.<br>
 * Description Document retrieval advisor.<br>
 * Created at 2024-08-27 10:47
 *
 * @author yuanci.ytb
 * @version 1.0.0
 * @since jdk8
 */

public class DashScopeDocumentRetrievalAdvisor implements RequestResponseAdvisor {

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
			{documents}
			""";

	public static String RETRIEVED_DOCUMENTS = "documents";

	private final DocumentRetriever retriever;

	private final String userTextAdvise;

	private final boolean enableReference;

	public DashScopeDocumentRetrievalAdvisor(DocumentRetriever retriever, boolean enableReference) {
		this.retriever = retriever;
		this.enableReference = enableReference;
		this.userTextAdvise = DEFAULT_USER_TEXT_ADVISE;
	}

	public DashScopeDocumentRetrievalAdvisor(DocumentRetriever retriever, String userTextAdvise,
			boolean enableReference) {
		this.retriever = retriever;
		this.userTextAdvise = userTextAdvise;
		this.enableReference = enableReference;
	}

	@Override
	public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
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
			.build();
	}

	@Override
	public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
		return handleReferencedDocuments(response, context);
	}

	@Override
	public Flux<ChatResponse> adviseResponse(Flux<ChatResponse> fluxResponse, Map<String, Object> context) {
		return fluxResponse.map(cr -> handleReferencedDocuments(cr, context));
	}

	private ChatResponse handleReferencedDocuments(ChatResponse response, Map<String, Object> context) {
		if (!enableReference) {
			return response;
		}

		ChatCompletionFinishReason finishReason = ChatCompletionFinishReason
			.valueOf(response.getResult().getMetadata().getFinishReason());
		if (finishReason == ChatCompletionFinishReason.NULL) {
			String fullContent = context.getOrDefault("full_content", "").toString()
					+ response.getResult().getOutput().getContent();
			context.put("full_content", fullContent);
			return response;
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

		ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(response);
		return chatResponseBuilder.withMetadata(RETRIEVED_DOCUMENTS, referencedDocuments).build();
	}

}
