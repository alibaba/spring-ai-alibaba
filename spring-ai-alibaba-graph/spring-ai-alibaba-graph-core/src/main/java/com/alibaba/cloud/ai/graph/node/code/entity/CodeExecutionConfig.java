package com.alibaba.cloud.ai.graph.node.code.entity;

import lombok.Builder;
import lombok.Data;

/**
 * Config for the code execution.
 *
 * @author HeYQ
 * @since 0.0.1
 */
@Data
@Builder
public class CodeExecutionConfig {

	/**
	 * the working directory for the code execution.
	 */
	@Builder.Default
	private String workDir = "extensions";

	/**
	 * the docker image to use for code execution.
	 */
	private String docker;

	/**
	 * the maximum execution time in seconds.
	 */
	@Builder.Default
	private int timeout = 600;

	/**
	 * the number of messages to look back for code execution. default value is 1, and -1
	 * indicates auto mode.
	 */
	@Builder.Default
	private int lastMessagesNumber = 1;

	@Builder.Default
	private int codeMaxDepth = 5;

}
