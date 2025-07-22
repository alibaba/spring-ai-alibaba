package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 异步任务结果查询请求
 */
@Data
public class AsyncResultRequest implements Serializable {

	@JsonProperty("task_id")
	private String taskId;

}
