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

package com.alibaba.cloud.ai.example.deepresearch.rag.kb.model;

import java.util.Map;

/**
 * 知识库搜索结果
 *
 * @author hupei
 */
public record KbSearchResult(String id, String title, String content, String url, Double score,
		Map<String, Object> metadata) {
	// 无参构造函数对应的静态工厂方法
	public static KbSearchResult empty() {
		return new KbSearchResult(null, null, null, null, null, null);
	}

	// 两个参数构造函数对应的静态工厂方法
	public static KbSearchResult of(String title, String content) {
		return new KbSearchResult(null, title, content, null, null, null);
	}

	// 五个参数构造函数（id, title, content, url, score）
	public static KbSearchResult of(String id, String title, String content, String url, Double score) {
		return new KbSearchResult(id, title, content, url, score, null);
	}
}
