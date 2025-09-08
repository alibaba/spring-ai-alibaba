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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory report service class (used when Redis is unavailable)
 *
 * @author huangzhen
 * @since 2025/6/20
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
public class ReportMemoryService implements ReportService {

	private static final Logger logger = LoggerFactory.getLogger(ReportMemoryService.class);

	/**
	 * In-memory storage using ConcurrentHashMap to ensure thread safety
	 */
	private final Map<String, String> reportStorage = new ConcurrentHashMap<>();

	/**
	 * Stores reports in memory
	 * @param threadId Thread ID
	 * @param report Report content
	 */
	@Override
	public void saveReport(String threadId, String report) {
		try {
			reportStorage.put(threadId, report);
			logger.info("Report saved to memory, thread ID: {}", threadId);
		}
		catch (Exception e) {
			logger.error("Failed to save report to memory, thread ID: {}", threadId, e);
			throw new RuntimeException("Failed to save report", e);
		}
	}

	/**
	 * Retrieves reports from memory
	 * @param threadId Thread ID
	 * @return Report content, returns null if not found
	 */
	@Override
	public String getReport(String threadId) {
		try {
			String report = reportStorage.get(threadId);
			if (report != null) {
				logger.info("Successfully retrieved report from memory, thread ID: {}", threadId);
				return report;
			}
			else {
				logger.warn("Report not found in memory, thread ID: {}", threadId);
				return null;
			}
		}
		catch (Exception e) {
			logger.error("Failed to get report from memory, thread ID: {}", threadId, e);
			throw new RuntimeException("Failed to get report", e);
		}
	}

	/**
	 * Checks if a report exists
	 * @param threadId Thread ID
	 * @return Whether the report exists
	 */
	@Override
	public boolean existsReport(String threadId) {
		try {
			boolean exists = reportStorage.containsKey(threadId);
			logger.debug("Checking if report exists, thread ID: {}, exists: {}", threadId, exists);
			return exists;
		}
		catch (Exception e) {
			logger.error("Failed to check if report exists, thread ID: {}", threadId, e);
			return false;
		}
	}

	/**
	 * Deletes a report
	 * @param threadId Thread ID
	 */
	@Override
	public void deleteReport(String threadId) {
		try {
			reportStorage.remove(threadId);
			logger.info("Report deleted from memory, thread ID: {}", threadId);
		}
		catch (Exception e) {
			logger.error("Failed to delete report, thread ID: {}", threadId, e);
			throw new RuntimeException("Failed to delete report", e);
		}
	}

}
