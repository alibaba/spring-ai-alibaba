package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkflowStatus {

	@JsonProperty("completed")
	COMPLETED("completed"),

	@JsonProperty("failed")
	FAILED("failed"),

	@JsonProperty("in_progress")
	IN_PROGRESS("in_progress"),

	@JsonProperty("pause")
	PAUSE("pause"),;

	private final String value;

}
