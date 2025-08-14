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
import java.util.Map;

@Data
public class ApiTaskMsg implements Serializable {

	/**
	 * @see Event
	 */
	private String event;

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("conversation_id")
	private String conversationId;

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

	private Map<String, Object> ext;

	@JsonProperty("content_type")
	private String contentType = "text";

	@JsonProperty("text_content")
	private String textContent;

	@JsonProperty("error_code")
	private String error_code;

	@JsonProperty("error_message")
	private String error_message;

	@JsonProperty("pause_data")
	private Object pause_data;

	/**
	 * @see PauseType
	 */
	@JsonProperty("pause_type")
	private String pauseType;

	public enum Event {

		Message, Error, Finished, Paused

	}

	public enum PauseType {

		InputNodeInterrupt

	}

}
