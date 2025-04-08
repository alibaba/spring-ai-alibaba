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
package com.alibaba.cloud.ai.param;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreateAppParam {

	@Schema(description = "app name", example = "rag-demo")
	private String name;

	@Schema(description = "app mode", example = "one of `chatbot`, `workflow`")
	private String mode;

	@Schema(description = "app description")
	private String description;

	public String getName() {
		return name;
	}

	public CreateAppParam setName(String name) {
		this.name = name;
		return this;
	}

	public String getMode() {
		return mode;
	}

	public CreateAppParam setMode(String mode) {
		this.mode = mode;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public CreateAppParam setDescription(String description) {
		this.description = description;
		return this;
	}

}
