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
package com.alibaba.cloud.ai.dashscope.rag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author nuocheng.lxm
 * @since 2024/7/22 15:14 百炼文档解析相关配置项
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeDocumentCloudReaderOptions {

	/**
	 * 类目ID 如果没有指定类目则会上传到默认类目
	 */
	private @JsonProperty("category_id") String categoryId;

	public DashScopeDocumentCloudReaderOptions() {
		this.categoryId = "default";
	}

	public DashScopeDocumentCloudReaderOptions(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryId() {
		return categoryId;
	}

}
