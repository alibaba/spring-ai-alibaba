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

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author vlsmb
 * @since 2025/8/6
 */
@Service
public class InMemorySessionContextService implements SessionContextService {

	private final ReportService reportService;

	private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> sessionThreadMap;

	public InMemorySessionContextService(ReportService reportService) {
		this.reportService = reportService;
		this.sessionThreadMap = new ConcurrentHashMap<>();
	}

	@Override
	public void addThreadId(String sessionId, String threadId) {
		sessionThreadMap.putIfAbsent(sessionId, new CopyOnWriteArrayList<>());
		sessionThreadMap.get(sessionId).add(threadId);
	}

	@Override
	public List<String> getGraphThreadIds(String sessionId) {
		return List.copyOf(Optional.ofNullable(sessionThreadMap.get(sessionId)).orElse(new CopyOnWriteArrayList<>()));
	}

	@Override
	public List<String> getReports(String sessionId, List<String> threadIds) {
		return threadIds.stream()
			.filter(threadId -> Optional.ofNullable(sessionThreadMap.get(sessionId))
				.orElse(new CopyOnWriteArrayList<>())
				.contains(threadId))
			.map(reportService::getReport)
			.toList();
	}

	@Override
	public List<String> getRecentReports(String sessionId, int count) {
		List<String> list = Optional.ofNullable(sessionThreadMap.get(sessionId)).orElse(new CopyOnWriteArrayList<>());
		int size = list.size();
		return list.stream().skip(Math.max(0, size - count)).map(reportService::getReport).limit(count).toList();
	}

}
