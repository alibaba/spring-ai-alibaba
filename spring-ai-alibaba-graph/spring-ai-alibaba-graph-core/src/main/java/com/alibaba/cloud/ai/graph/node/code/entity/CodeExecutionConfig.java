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

public class CodeExecutionConfig {

	private String workDir = "extensions";

	/**
	 * the docker image to use for code execution.
	 */
	private String docker;

	private int timeout = 600;

	private int lastMessagesNumber = 1;

	private int codeMaxDepth = 5;

	public String getWorkDir() {
		return workDir;
	}

	public CodeExecutionConfig setWorkDir(String workDir) {
		this.workDir = workDir;
		return this;
	}

	public String getDocker() {
		return docker;
	}

	public CodeExecutionConfig setDocker(String docker) {
		this.docker = docker;
		return this;
	}

	public int getTimeout() {
		return timeout;
	}

	public CodeExecutionConfig setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	public int getLastMessagesNumber() {
		return lastMessagesNumber;
	}

	public CodeExecutionConfig setLastMessagesNumber(int lastMessagesNumber) {
		this.lastMessagesNumber = lastMessagesNumber;
		return this;
	}

	public int getCodeMaxDepth() {
		return codeMaxDepth;
	}

	public CodeExecutionConfig setCodeMaxDepth(int codeMaxDepth) {
		this.codeMaxDepth = codeMaxDepth;
		return this;
	}

}
