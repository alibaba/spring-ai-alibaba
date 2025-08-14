package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InitRequest {

	@JsonProperty("app_id")
	private String appId;

	private String version;

}
