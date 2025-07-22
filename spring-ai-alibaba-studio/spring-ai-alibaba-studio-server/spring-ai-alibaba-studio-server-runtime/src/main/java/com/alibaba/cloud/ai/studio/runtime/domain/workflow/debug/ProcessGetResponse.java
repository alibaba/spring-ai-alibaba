package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工作流控制台调试结果出参
 */
@Data
public class ProcessGetResponse implements Serializable {

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("request_id")
	private String requestId;

	@JsonProperty("conversation_id")
	private String conversationId;

	@JsonProperty("task_status")
	private String taskStatus;

	@JsonProperty("task_results")
	private List<ProcessOutput> taskResults;

	@JsonProperty("error_code")
	private String errorCode;

	@JsonProperty("error_info")
	private String errorInfo;

	@JsonProperty("task_exec_time")
	private String taskExecTime;

	@JsonProperty("node_results")
	private List<NodeResult> nodeResults;

	// 变量内容 todo

	@Data
	public static class ProcessOutput {

		@JsonProperty("node_type")
		private String nodeType;

		@JsonProperty("node_name")
		private String nodeName;

		@JsonProperty("node_id")
		private String nodeId;

		@JsonProperty("parent_node_id")
		private String parentNodeId;

		@JsonProperty("node_content")
		private Object nodeContent;

		@JsonProperty("node_status")
		private String nodeStatus;

		@JsonProperty("index")
		private Integer index;

	}

}
