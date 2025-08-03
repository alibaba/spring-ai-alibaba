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

package com.alibaba.cloud.ai.dto;

/**
 * 提示词配置请求DTO
 *
 * @author Makoto
 */
public record PromptConfigDTO(String id, // 配置ID（更新时需要）
		String name, // 配置名称
		String promptType, // 提示词类型
		String systemPrompt, // 用户自定义的系统提示词内容
		Boolean enabled, // 是否启用该配置
		String description, // 配置描述
		String creator // 创建者
) {
	public PromptConfigDTO(String promptType, String systemPrompt) {
		this(null, null, promptType, systemPrompt, true, null, null);
	}

	@Override
	public String toString() {
		return "PromptConfigDTO{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", promptType='" + promptType + '\''
				+ ", enabled=" + enabled + ", description='" + description + '\'' + ", creator='" + creator + '\''
				+ '}';
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String creator() {
		return creator;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String promptType() {
		return promptType;
	}

	@Override
	public String systemPrompt() {
		return systemPrompt;
	}

	@Override
	public Boolean enabled() {
		return enabled;
	}

	@Override
	public String description() {
		return description;
	}
}
