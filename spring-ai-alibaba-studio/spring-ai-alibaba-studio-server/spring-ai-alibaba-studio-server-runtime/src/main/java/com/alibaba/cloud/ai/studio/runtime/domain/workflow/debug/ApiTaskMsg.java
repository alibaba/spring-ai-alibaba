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
