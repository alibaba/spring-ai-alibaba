package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 任务重入结果
 */
@Data
public class TaskResumeResponse implements Serializable {

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("request_id")
	private String requestId;

}
