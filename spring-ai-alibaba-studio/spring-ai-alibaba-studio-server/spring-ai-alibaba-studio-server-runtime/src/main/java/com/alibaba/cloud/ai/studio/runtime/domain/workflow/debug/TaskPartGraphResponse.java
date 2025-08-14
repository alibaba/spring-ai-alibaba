package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 单节点调试出参
 */
@Data
public class TaskPartGraphResponse implements Serializable {

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("request_id")
	private String requestId;

}
