package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ApiTaskRunRequest {

	private List<TaskRunParam> inputs;

	@JsonProperty("conversation_id")
	private String conversationId;

}
