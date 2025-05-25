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
