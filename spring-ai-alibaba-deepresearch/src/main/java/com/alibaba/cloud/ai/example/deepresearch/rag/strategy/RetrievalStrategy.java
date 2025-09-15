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
package com.alibaba.cloud.ai.example.deepresearch.rag.strategy;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 定义了从数据源检索文档的策略接口。 每种数据源（如用户上传的文件、专业知识库）都应有其具体的实现。
 */
public interface RetrievalStrategy {

	/**
	 * 根据查询和选项从特定数据源检索相关文档。
	 * @param query 用户的查询字符串。
	 * @param options 包含额外参数的映射，例如 session_id, user_id 等，用于上下文过滤。
	 * @return 相关文档的列表。
	 */
	List<Document> retrieve(String query, Map<String, Object> options);

	/**
	 * 返回此策略的唯一名称，用于在配置中识别和选择。
	 * @return 策略名称。
	 */
	String getStrategyName();

}
