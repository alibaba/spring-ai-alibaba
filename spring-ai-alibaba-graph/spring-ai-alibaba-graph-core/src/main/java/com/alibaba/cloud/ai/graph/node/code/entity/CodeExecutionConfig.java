/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
