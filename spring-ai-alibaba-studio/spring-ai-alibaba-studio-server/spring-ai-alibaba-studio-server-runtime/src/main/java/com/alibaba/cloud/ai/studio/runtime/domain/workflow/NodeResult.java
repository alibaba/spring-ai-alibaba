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
package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class NodeResult implements Serializable {

	// 是否多分支节点
	@JsonProperty("is_multi_branch")
	private boolean isMultiBranch = false;

	// 多分支节点结果
	@JsonProperty("multi_branch_results")
	private List<MultiBranchReference> multiBranchResults;

	@JsonProperty("node_id")
	private String nodeId;

	@JsonProperty("node_name")
	private String nodeName;

	@JsonProperty("node_type")
	private String nodeType;

	@JsonProperty("node_status")
	private String nodeStatus;

	@JsonProperty("node_exec_time")
	private String nodeExecTime;

	private Retry retry;

	@JsonProperty("try_catch")
	private TryCatch tryCatch;

	// 短期记忆
	@JsonProperty("short_memory")
	private ShortMemory shortMemory;

	@JsonProperty("error_code")
	private String errorCode;

	@JsonProperty("error_info")
	private String errorInfo;

	private Error error;

	private String input;

	private String output;

	// json or text
	@JsonProperty("output_type")
	private String outputType = "json";

	// 增量输出，用于api输出
	@JsonProperty("increment_output")
	private String incrementOutput;

	private List<Usage> usages;

	@JsonProperty("parent_node_id")
	private String parentNodeId;

	// 判���是否批处理节点
	@JsonProperty("is_batch")
	private boolean isBatch = false;

	// 批处理结果返回
	private List<NodeResult> batches = new CopyOnWriteArrayList<>();

	// 批次序号,仅isBatch = false时生效
	private Integer index;

	private String ext;

	public static NodeResult error(Node node, Error error) {
		NodeResult result = new NodeResult();
		result.setNodeId(node.getId());
		result.setNodeName(node.getName() == null ? node.getId() : node.getName());
		result.setNodeType(node.getType());
		result.setNodeStatus(NodeStatusEnum.FAIL.getCode());
		result.setErrorInfo(error.getMessage());
		result.setError(error);
		return result;
	}

	public static NodeResult error(Node node, String errorMsg) {
		NodeResult result = new NodeResult();
		result.setNodeId(node.getId());
		result.setNodeName(node.getName() == null ? node.getId() : node.getName());
		result.setNodeType(node.getType());
		result.setNodeStatus(NodeStatusEnum.FAIL.getCode());
		result.setErrorInfo(errorMsg);
		return result;
	}

	@Data
	public static class Retry implements Serializable {

		private static final long serialVersionUID = -1L;

		private boolean happened = false;

		@JsonProperty("retry_times")
		private Integer retryTimes;

	}

	@Data
	public static class TryCatch implements Serializable {

		private static final long serialVersionUID = -1L;

		private boolean happened = false;

		private String strategy;

	}

	@Data
	public static class ShortMemory implements Serializable {

		private static final long serialVersionUID = -1L;

		@JsonProperty("current_self_chat_messages")
		private List<ChatMessage> currentSelfChatMessages;

		private Integer round;

	}

	@Data
	public static class MultiBranchReference implements Serializable {

		private static final long serialVersionUID = -1L;

		@JsonProperty("condition_id")
		private String conditionId;

		@JsonProperty("target_ids")
		private List<String> targetIds;

	}

}
