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

/**
 * Represents the result of code execution.
 *
 * @param exitCode 0 if the code executes successfully.
 * @param logs the error message if the code fails to execute, the stdout otherwise.
 * @param extra commandLine code_file or the docker image name after container run when
 * docker is used.
 * @author HeYQ
 * @since 0.0.1
 */
public record CodeExecutionResult(int exitCode, String logs, String extra) {

	public CodeExecutionResult(int exitCode, String logs) {
		this(exitCode, logs, null);
	}
}
