package com.alibaba.cloud.ai.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * code generate param TODO complement
 */
@Data
public class CodeGenerateParam {

	@Schema(description = "node type", example = "CODE")
	private String nodeType;

	@Schema(description = "node data")
	private Map<String, Object> nodeData;

}
