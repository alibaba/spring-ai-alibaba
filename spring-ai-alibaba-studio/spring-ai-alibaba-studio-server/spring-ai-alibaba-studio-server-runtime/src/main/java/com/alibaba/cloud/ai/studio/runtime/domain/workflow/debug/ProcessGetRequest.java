package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工作流控制台调试结果入参
 */
@Data
public class ProcessGetRequest implements Serializable {

	@JsonProperty("task_id")
	private String taskId;

}
