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
package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import java.util.List;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:53
 */
public class AgentShell {

	private final String type;

	private final String name;

	private final String description;

	private final String instruction;

	private final List<String> inputKeys;

	private final String outputKey;

	public AgentShell(String type, String name, String description, String instruction, List<String> inputKeys,
			String outputKey) {
		this.type = type;
		this.name = name;
		this.description = description;
		this.instruction = instruction;
		this.inputKeys = inputKeys;
		this.outputKey = outputKey;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getInstruction() {
		return instruction;
	}

	public List<String> getInputKeys() {
		return inputKeys;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public static AgentShell of(String type, String name, String description, String instruction,
			List<String> inputKeys, String outputKey) {
		return new AgentShell(type, name, description, instruction, inputKeys, outputKey);
	}

}
