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

package com.alibaba.cloud.ai.example.deepresearch.service;

import java.util.List;

/**
 * 记录某会话Id的上下文（如线程ID等信息）
 *
 * @author vlsmb
 * @since 2025/8/6
 */
public interface SessionContextService {

	void addThreadId(String sessionId, String threadId);

	List<String> getGraphThreadIds(String sessionId);

	List<String> getReports(String sessionId, List<String> threadIds);

	List<String> getRecentReports(String sessionId, int count);

	default List<String> getRecentReports(String sessionId) {
		return this.getRecentReports(sessionId, 5);
	}

}
