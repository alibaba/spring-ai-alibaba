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

import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.WorkflowStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * workflow response.
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse implements Serializable {

	@JsonProperty("request_id")
	private String requestId;

	@JsonProperty("conversation_id")
	private String conversationId;

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("node_id")
	private String nodeId;

	@JsonProperty("node_name")
	private String nodeName;

	@JsonProperty("node_type")
	private String nodeType;

	@JsonProperty("node_status")
	private String nodeStatus;

	@JsonProperty("node_msg_seq_id")
	private Integer nodeMsgSeqId;

	@JsonProperty("node_is_completed")
	private Boolean nodeIsCompleted;

	private WorkflowStatus status;

	private ChatMessage message;

	private Error error;

	@JsonIgnore
	public boolean isSuccess() {
		return error == null;
	}

}
