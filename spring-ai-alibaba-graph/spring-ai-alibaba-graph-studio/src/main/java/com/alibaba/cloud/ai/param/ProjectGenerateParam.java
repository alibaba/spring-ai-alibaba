/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * generate project params
 */
@Data
public class ProjectGenerateParam {

	@Schema(description = "dsl raw content")
	private String dsl;

	@Schema(description = "dsl dialect", example = "dify")
	private String dialect;

	@Schema(description = "spring boot version")
	private String bootVersion;

	@Schema(description = "base directory", example = "demo")
	private String baseDir;

	@Schema(description = "group id", example = "com.example")
	private String groupId;

	@Schema(description = "artifact id", example = "demo")
	private String name;

	@Schema(description = "description")
	private String description;

	@Schema(description = "package name", example = "com.example.demo")
	private String packageName;

	@Schema(description = "packaging", example = "jar")
	private String packaging;

	@Schema(description = "javaVersion", example = "21")
	private String javaVersion;

}
