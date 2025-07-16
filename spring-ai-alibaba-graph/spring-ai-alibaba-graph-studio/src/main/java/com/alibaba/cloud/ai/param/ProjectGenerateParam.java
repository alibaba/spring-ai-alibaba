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

/**
 * generate project params
 */

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

	public String getDsl() {
		return dsl;
	}

	public ProjectGenerateParam setDsl(String dsl) {
		this.dsl = dsl;
		return this;
	}

	public String getDialect() {
		return dialect;
	}

	public ProjectGenerateParam setDialect(String dialect) {
		this.dialect = dialect;
		return this;
	}

	public String getBootVersion() {
		return bootVersion;
	}

	public ProjectGenerateParam setBootVersion(String bootVersion) {
		this.bootVersion = bootVersion;
		return this;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public ProjectGenerateParam setBaseDir(String baseDir) {
		this.baseDir = baseDir;
		return this;
	}

	public String getGroupId() {
		return groupId;
	}

	public ProjectGenerateParam setGroupId(String groupId) {
		this.groupId = groupId;
		return this;
	}

	public String getName() {
		return name;
	}

	public ProjectGenerateParam setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public ProjectGenerateParam setDescription(String description) {
		this.description = description;
		return this;
	}

	public String getPackageName() {
		return packageName;
	}

	public ProjectGenerateParam setPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}

	public String getPackaging() {
		return packaging;
	}

	public ProjectGenerateParam setPackaging(String packaging) {
		this.packaging = packaging;
		return this;
	}

	public String getJavaVersion() {
		return javaVersion;
	}

	public ProjectGenerateParam setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
		return this;
	}

}
