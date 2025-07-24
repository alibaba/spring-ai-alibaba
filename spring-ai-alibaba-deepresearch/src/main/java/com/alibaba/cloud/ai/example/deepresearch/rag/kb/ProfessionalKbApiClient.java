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

import com.alibaba.cloud.ai.example.deepresearch.rag.kb.model.KbSearchResult;

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

}
