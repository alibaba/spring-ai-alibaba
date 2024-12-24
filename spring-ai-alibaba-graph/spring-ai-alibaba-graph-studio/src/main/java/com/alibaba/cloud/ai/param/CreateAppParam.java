package com.alibaba.cloud.ai.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateAppParam {

	@Schema(description = "app name", example = "rag-demo")
	private String name;

	@Schema(description = "app mode", example = "one of `chatbot`, `workflow`")
	private String mode;

	@Schema(description = "app description")
	private String description;

}
