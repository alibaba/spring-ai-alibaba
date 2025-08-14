package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 调试请求
 */
@Data
public class TaskRunRequest implements Serializable {

	@JsonProperty("app_id")
	private String appId;

	private List<TaskRunParam> inputs;

	@JsonProperty("conversation_id")
	private String conversationId;

	private String version;

}
