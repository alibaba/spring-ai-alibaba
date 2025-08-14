package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 异步任务结果响应
 */
@Data
public class AsyncResultResponse implements Serializable {

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("request_id")
	private String requestId;

	@JsonProperty("conversation_id")
	private String conversationId;

	@JsonProperty("task_status")
	private String taskStatus;

	@JsonProperty("error_code")
	private String errorCode;

	@JsonProperty("error_info")
	private String errorInfo;

	@JsonProperty("task_exec_time")
	private String taskExecTime;

	@JsonProperty("outputs")
	private List<Output> outputs;

	@Data
	public static class Output implements Serializable {

		@JsonProperty("content")
		private String content;

		@JsonProperty("node_id")
		private String nodeId;

		@JsonProperty("node_name")
		private String nodeName;

		@JsonProperty("node_type")
		private String nodeType;

		@JsonProperty("node_status")
		private String nodeStatus;

	}

}
