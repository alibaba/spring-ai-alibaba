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
