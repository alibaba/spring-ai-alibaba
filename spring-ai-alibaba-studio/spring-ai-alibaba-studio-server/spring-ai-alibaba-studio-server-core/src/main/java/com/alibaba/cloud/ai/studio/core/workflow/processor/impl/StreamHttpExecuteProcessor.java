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

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.node.StreamHttpNode;
import com.alibaba.cloud.ai.graph.node.StreamHttpNodeParam;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("StreamHttpExecuteProcessor")
public class StreamHttpExecuteProcessor extends AbstractExecuteProcessor {

	public StreamHttpExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
	}

	/**
	 * Executes the StreamHttp node in the workflow
	 * @param graph The workflow graph
	 * @param node The StreamHttp node to execute
	 * @param context The workflow context
	 * @return NodeResult containing streaming call status and response
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {

		// Initialize and refresh context
		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		try {
			NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);

			// Build StreamHttpNodeParam from config
			StreamHttpNodeParam.Builder paramBuilder = StreamHttpNodeParam.builder()
				.webClient(WebClient.create())
				.method(HttpMethod.valueOf(config.getMethod().toUpperCase()))
				.url(replaceTemplateContent(config.getUrl(), context))
				.streamFormat(StreamHttpNodeParam.StreamFormat.valueOf(config.getStreamFormat()))
				.streamMode(StreamHttpNodeParam.StreamMode.valueOf(config.getStreamMode()))
				.delimiter(config.getDelimiter())
				.outputKey(config.getOutputKey())
				.readTimeout(Duration.ofMillis(config.getTimeout()));

			// Add headers if present
			if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
				Map<String, String> headers = new HashMap<>();
				config.getHeaders().forEach(header -> {
					String value = replaceTemplateContent(header.getValue(), context);
					headers.put(header.getKey(), value);
				});
				paramBuilder.headers(headers);
			}

			// Add body if present (skip body configuration for now)
			// TODO: Implement proper body configuration conversion

			StreamHttpNodeParam streamParam = paramBuilder.build();
			StreamHttpNode streamHttpNode = new StreamHttpNode(streamParam);

			// Create OverAllState from workflow context
			OverAllState state = createOverAllState(context);

			// Execute streaming and collect results
			Flux<Map<String, Object>> resultFlux = streamHttpNode.executeStreaming(state);

			// For workflow integration, we need to collect the streaming results
			// This is a blocking operation for workflow compatibility
			List<Map<String, Object>> results = resultFlux.collectList().block();

			// Set results
			Map<String, Object> output = new HashMap<>();
			if (results != null && !results.isEmpty()) {
				if (config.getOutputKey() != null && !config.getOutputKey().isEmpty()) {
					output.put(config.getOutputKey(), results);
				}
				else {
					// If no output key specified, put results directly
					if (results.size() == 1) {
						output.putAll(results.get(0));
					}
					else {
						output.put("results", results);
					}
				}
			}

			nodeResult.setOutput(JsonUtils.toJson(output));
			nodeResult.setNodeId(node.getId());
			nodeResult.setNodeType(node.getType());

			log.info("StreamHttp node executed successfully, nodeId: {}, resultsCount: {}", node.getId(),
					results != null ? results.size() : 0);

		}
		catch (Exception e) {
			log.error("StreamHttp node execution failed, nodeId: {}", node.getId(), e);
			nodeResult.setNodeStatus(com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum.FAIL.getCode());
			nodeResult.setOutput(null);
			nodeResult.setErrorInfo("StreamHttp node exception: " + e.getMessage());
			nodeResult.setError(com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode.WORKFLOW_EXECUTE_ERROR
				.toError("StreamHttp node exception: " + e.getMessage()));
		}

		return nodeResult;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.STREAM_HTTP.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.STREAM_HTTP.getDesc();
	}

	/**
	 * Create OverAllState from WorkflowContext
	 */
	private OverAllState createOverAllState(WorkflowContext context) {
		Map<String, Object> stateData = new HashMap<>();
		// Copy variables from workflow context to state
		if (context.getVariablesMap() != null) {
			stateData.putAll(context.getVariablesMap());
		}
		return new OverAllState(stateData);
	}

	/**
	 * Node parameter configuration
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("method")
		private String method = "GET";

		@JsonProperty("url")
		private String url;

		@JsonProperty("headers")
		private List<HeaderParam> headers;

		@JsonProperty("body")
		private Map<String, Object> body;

		@JsonProperty("streamFormat")
		private String streamFormat = "SSE";

		@JsonProperty("streamMode")
		private String streamMode = "DISTRIBUTE";

		@JsonProperty("delimiter")
		private String delimiter = "\n";

		@JsonProperty("outputKey")
		private String outputKey;

		@JsonProperty("timeout")
		private int timeout = 30000; // 30 seconds default

		@Data
		public static class HeaderParam {

			@JsonProperty("key")
			private String key;

			@JsonProperty("value")
			private String value;

		}

	}

}
