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

package com.alibaba.cloud.ai.example.deepresearch.rag;

/**
 * 数据源类型枚举 对应数据来源标识符：user_upload、professional_kb_es、professional_kb_api
 */
public enum SourceTypeEnum {

	/**
	 * 用户上传文件
	 */
	USER_UPLOAD("user_upload"),

	/**
	 * 专业知识库 - Elasticsearch存储
	 */
	PROFESSIONAL_KB_ES("professional_kb_es"),

	/**
	 * 专业知识库 - API接口
	 */
	PROFESSIONAL_KB_API("professional_kb_api");

	private final String value;

	SourceTypeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
