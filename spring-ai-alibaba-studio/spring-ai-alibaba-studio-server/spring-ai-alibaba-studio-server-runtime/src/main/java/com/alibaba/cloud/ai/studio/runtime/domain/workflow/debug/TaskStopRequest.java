package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Stop task request
 */
@Data
public class TaskStopRequest implements Serializable {

	@JsonProperty("task_id")
	private String taskId;

}
