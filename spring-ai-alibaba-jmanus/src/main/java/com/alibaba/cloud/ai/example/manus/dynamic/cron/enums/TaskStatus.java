/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.cron.enums;

/**
 * 定时任务状态枚举
 */
public enum TaskStatus {

	/**
	 * 启用状态
	 */
	ENABLED(0, "启用"),

	/**
	 * 禁用状态
	 */
	DISABLED(1, "禁用");

	private final Integer code;

	private final String description;

	TaskStatus(Integer code, String description) {
		this.code = code;
		this.description = description;
	}

	public Integer getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public static TaskStatus fromCode(Integer code) {
		for (TaskStatus status : TaskStatus.values()) {
			if (status.getCode().equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown task status code: " + code);
	}

}
