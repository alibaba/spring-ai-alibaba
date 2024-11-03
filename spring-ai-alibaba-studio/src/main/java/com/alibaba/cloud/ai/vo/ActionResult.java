package com.alibaba.cloud.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ActionResult {

	private String Response;

	@Schema(description = "stream response", nullable = true)
	private List<String> streamResponse;

}
