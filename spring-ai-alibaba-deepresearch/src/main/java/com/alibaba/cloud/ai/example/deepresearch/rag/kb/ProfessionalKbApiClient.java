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

package com.alibaba.cloud.ai.example.deepresearch.rag.kb;

import java.util.List;
import java.util.Map;

/**
 * 专业知识库API客户端接口
 *
 * @author hupei
 */
public interface ProfessionalKbApiClient {

	/**
	 * 搜索知识库
	 * @param query 查询文本
	 * @param options 选项参数，可包含maxResults、timeout等
	 * @return 搜索结果列表
	 */
	List<KbSearchResult> search(String query, Map<String, Object> options);

	/**
	 * 获取支持的提供商类型
	 * @return 提供商类型，如"dashscope", "custom"等
	 */
	String getProvider();

	/**
	 * 检查客户端是否已正确配置
	 * @return 是否可用
	 */
	boolean isAvailable();

	/**
	 * 知识库搜索结果
	 */
	class KbSearchResult {

		private String id;

		private String title;

		private String content;

		private String url;

		private Double score;

		private Map<String, Object> metadata;

		public KbSearchResult() {
		}

		public KbSearchResult(String title, String content) {
			this.title = title;
			this.content = content;
		}

		public KbSearchResult(String id, String title, String content, String url, Double score) {
			this.id = id;
			this.title = title;
			this.content = content;
			this.url = url;
			this.score = score;
		}

		// Getters and Setters
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public Double getScore() {
			return score;
		}

		public void setScore(Double score) {
			this.score = score;
		}

		public Map<String, Object> getMetadata() {
			return metadata;
		}

		public void setMetadata(Map<String, Object> metadata) {
			this.metadata = metadata;
		}

	}

}
