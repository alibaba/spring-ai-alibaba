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

package com.alibaba.cloud.ai.studio.core.workflow.processor.impl;

import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ShortTermMemory;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.DocumentRetrieverManager;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.collections.CollectionUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Knowledge Base Retrieval Node Processor
 * <p>
 * This class is responsible for handling the execution of knowledge base retrieval nodes
 * in the workflow. It provides functionality for: 1. Retrieving relevant document chunks
 * from knowledge bases 2. Supporting multiple knowledge base sources 3. Configurable
 * similarity thresholds and top-k retrieval 4. Integration with short-term memory for
 * context-aware retrieval 5. Customizable retrieval strategies 6. Input validation and
 * error handling 7. Result aggregation and formatting
 *
 * @version 1.0.0-M1
 */
@Component("RetrievalExecuteProcessor")
public class RetrievalExecuteProcessor extends AbstractExecuteProcessor {

	private final DocumentRetrieverManager documentRetrieverManager;

	public RetrievalExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig,
			DocumentRetrieverManager documentRetrieverManager) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.documentRetrieverManager = documentRetrieverManager;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.RETRIEVAL.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.RETRIEVAL.getDesc();
	}

	/**
	 * Executes the knowledge base retrieval operation
	 * @param graph The workflow graph
	 * @param node The current node to be executed
	 * @param context The workflow context
	 * @return NodeResult containing the retrieved document chunks
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		FileSearchOptions searchOptions = new FileSearchOptions();
		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		searchOptions.setTopK(config.getTopK());
		float similarityThreshold = config.getSimilarityThreshold() == null ? 0.5f : config.getSimilarityThreshold();
		searchOptions.setSimilarityThreshold(similarityThreshold);
		searchOptions.setKbIds(config.getKnowledgeBaseIds());

		List<Node.InputParam> inputParams = node.getConfig().getInputParams();
		String userInput = VariableUtils.getValueStringFromContext(inputParams.get(0), context);
		ShortTermMemory shortMemory = config.getShortMemory();

		assert userInput != null;
		Query query = Query.builder()
			.text(userInput)
			.history(constructShortTermMemory(node, shortMemory, context))
			.build();
		List<DocumentChunk> chunks = documentRetrieverManager.retrieve(query, searchOptions);

		nodeResult.setInput(JsonUtils.toJson(constructInputParamsMap(node, context)));
		RetrievalResult retrievalResult = new RetrievalResult().setChunkList(chunks);
		nodeResult.setOutput(JsonUtils.toJson(retrievalResult));
		return nodeResult;
	}

	/**
	 * Result container for retrieval operations
	 */
	@Data
	@Accessors(chain = true)
	public static class RetrievalResult {

		@JsonProperty("chunk_list")
		private List<DocumentChunk> chunkList;

	}

	/**
	 * Configuration parameters for the retrieval node
	 */
	@Data
	public static class NodeParam {

		// 知识库ids
		@JsonProperty("knowledge_base_ids")
		private List<String> knowledgeBaseIds;

		// 知识库检索策略
		@JsonProperty("prompt_strategy")
		private String promptStrategy = "topk";

		// 检索topK
		@JsonProperty("top_k")
		private Integer topK;

		@JsonProperty("similarity_threshold")
		private Float similarityThreshold;

		@JsonProperty("short_memory")
		private ShortTermMemory shortMemory;

	}

	/**
	 * Validates the input parameters for the retrieval operation
	 * @param inputParams List of input parameters to validate
	 * @return CheckNodeParamResult containing validation results
	 */
	@Override
	protected CheckNodeParamResult checkInputParams(List<Node.InputParam> inputParams) {
		CheckNodeParamResult result = CheckNodeParamResult.success();
		if (CollectionUtils.isEmpty(inputParams) || inputParams.get(0).getValue() == null) {
			result.setSuccess(false);
			result.getErrorInfos().add("Input is empty");
		}
		return result;
	}

	/**
	 * Validates the node parameters including input parameters and knowledge base
	 * configuration
	 * @param graph The workflow graph
	 * @param node The node to validate
	 * @return CheckNodeParamResult containing validation results
	 */
	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		CheckNodeParamResult inputParamsResult = checkInputParams(node.getConfig().getInputParams());
		if (!inputParamsResult.isSuccess()) {
			result.setSuccess(false);
			result.getErrorInfos().addAll(inputParamsResult.getErrorInfos());
		}
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		List<String> knowledgeBaseCodeList = nodeParam.getKnowledgeBaseIds();
		if (CollectionUtils.isEmpty(knowledgeBaseCodeList)) {
			result.setSuccess(false);
			result.getErrorInfos().add("[KnowledgeBase] is null");
		}
		return result;
	}

}
