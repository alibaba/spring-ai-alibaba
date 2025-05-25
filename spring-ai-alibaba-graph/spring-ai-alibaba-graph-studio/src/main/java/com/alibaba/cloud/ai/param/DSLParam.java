package com.alibaba.cloud.ai.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DSLParam {

	@Schema(description = "dsl raw content")
	private String content;

	@Schema(description = "dsl dialect", examples = { "dify", "flowise", "custom" })
	private String dialect;

}
