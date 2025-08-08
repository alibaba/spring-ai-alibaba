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

import com.alibaba.cloud.ai.example.deepresearch.model.SessionHistory;
import com.alibaba.cloud.ai.example.deepresearch.model.req.GraphId;
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

	// key: threadId
	private final ConcurrentHashMap<String, SessionHistory> sessionHistoryMap;

	public InMemorySessionContextService(ReportService reportService) {
		this.reportService = reportService;
		this.sessionThreadMap = new ConcurrentHashMap<>();
		this.sessionHistoryMap = new ConcurrentHashMap<>();
	}

	@Override
	public void addSessionHistory(GraphId graphId, SessionHistory sessionHistory) {
		sessionThreadMap.putIfAbsent(graphId.sessionId(), new CopyOnWriteArrayList<>());
		sessionThreadMap.get(graphId.sessionId()).add(graphId.threadId());
		// 会话的报告信息由reportService维护
		reportService.saveReport(graphId.threadId(), sessionHistory.getReport());
		sessionHistory.setReport("");
		sessionHistoryMap.put(graphId.threadId(), sessionHistory);
	}

	@Override
	public List<String> getGraphThreadIds(String sessionId) {
		return List.copyOf(Optional.ofNullable(sessionThreadMap.get(sessionId)).orElse(new CopyOnWriteArrayList<>()));
	}

	@Override
	public List<SessionHistory> getReports(String sessionId, List<String> threadIds) {
		return threadIds.stream()
			.filter(threadId -> Optional.ofNullable(sessionThreadMap.get(sessionId))
				.orElse(new CopyOnWriteArrayList<>())
				.contains(threadId))
			.map(sessionHistoryMap::get)
			.peek(sessionHistory -> {
				String threadId = sessionHistory.getGraphId().threadId();
				sessionHistory.setReport(reportService.getReport(threadId));
			})
			.toList();
	}

	@Override
	public List<SessionHistory> getRecentReports(String sessionId, int count) {
		List<String> list = Optional.ofNullable(sessionThreadMap.get(sessionId)).orElse(new CopyOnWriteArrayList<>());
		int size = list.size();
		return this.getReports(sessionId,
				this.getGraphThreadIds(sessionId).stream().skip(Math.max(0, size - count)).toList());
	}

}
