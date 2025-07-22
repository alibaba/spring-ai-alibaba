package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用编排调试出参
 */
@Data
public class TaskRunResponse implements Serializable {

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("conversation_id")
	private String conversationId;

	@JsonProperty("request_id")
	private String requestId;

}
