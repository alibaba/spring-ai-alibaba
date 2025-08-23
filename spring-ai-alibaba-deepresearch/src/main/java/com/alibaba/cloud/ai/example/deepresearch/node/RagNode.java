/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.rag.core.HybridRagProcessor;
import com.alibaba.cloud.ai.example.deepresearch.rag.strategy.FusionStrategy;
import com.alibaba.cloud.ai.example.deepresearch.rag.strategy.RetrievalStrategy;
import com.alibaba.cloud.ai.example.deepresearch.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A NodeAction that uses the RAG pipeline to generate a response based on user input.
 * 使用统一的HybridRagProcessor进行RAG前后处理和混合查询
 *
 * @author hupei
 */
public class RagNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(RagNode.class);

	private final ChatClient ragAgent;

	private final List<RetrievalStrategy> retrievalStrategies;

	private final FusionStrategy fusionStrategy;

	private final HybridRagProcessor hybridRagProcessor;

	/**
	 * 支持传统的策略模式构造函数（向后兼容）
	 */
	public RagNode(List<RetrievalStrategy> retrievalStrategies, FusionStrategy fusionStrategy, ChatClient ragAgent) {
		this.retrievalStrategies = retrievalStrategies;
		this.fusionStrategy = fusionStrategy;
		this.ragAgent = ragAgent;
		this.hybridRagProcessor = null;
	}

	/**
	 * 新的统一RAG处理器构造函数
	 */
	public RagNode(HybridRagProcessor hybridRagProcessor, ChatClient ragAgent) {
		this.hybridRagProcessor = hybridRagProcessor;
		this.ragAgent = ragAgent;
		this.retrievalStrategies = null;
		this.fusionStrategy = null;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("rag_node is running.");
		String queryText = StateUtil.getQuery(state);

		Map<String, Object> options = new HashMap<>();
		state.value("session_id", String.class).ifPresent(v -> options.put("session_id", v));
		state.value("user_id", String.class).ifPresent(v -> options.put("user_id", v));
		options.put("query", queryText); // 添加查询文本供后处理使用

		List<Document> documents = new ArrayList<>();

		// 使用统一的RAG处理器或传统的策略模式
		if (hybridRagProcessor != null) {
			// 使用统一的RAG处理器，包含完整的前后处理逻辑
			Query query = new Query(queryText);
			documents = hybridRagProcessor.process(query, options);
		}
		else if (retrievalStrategies != null && fusionStrategy != null) {
			// 传统策略模式（向后兼容）
			List<List<Document>> allResults = new ArrayList<>();
			for (RetrievalStrategy strategy : retrievalStrategies) {
				allResults.add(strategy.retrieve(queryText, options));
			}
			documents = fusionStrategy.fuse(allResults);
		}

		// 构建上下文
		StringBuilder contextBuilder = new StringBuilder();
		for (Document doc : documents) {
			contextBuilder.append(doc.getText()).append("\n");
		}

		// 生成响应
		Flux<ChatResponse> streamResult = ragAgent.prompt()
			.messages(new UserMessage(contextBuilder.toString()))
			.user(queryText)
			.stream()
			.chatResponse()
			.timeout(Duration.ofSeconds(180))
			.retry(2);

		logger.info("RAG node produced a result.");

		var generatedContent = StreamingChatGenerator.builder()
			.startingNode("rag_llm_stream")
			.startingState(state)
			.mapResult(response -> Map.of("rag_content",
					Objects.requireNonNull(response.getResult().getOutput().getText())))
			.buildWithChatResponse(streamResult);

		logger.info("RAG node produced a streaming result.");

		Map<String, Object> updated = new HashMap<>();
		updated.put("rag_content", generatedContent);

		return updated;
	}

}
