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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.example.deepresearch.config.DataPrivacyProperties;
import com.alibaba.cloud.ai.example.deepresearch.service.EnhancedDataPrivacyService;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据脱敏节点
 *
 * @author deepresearch
 * @since 2025/1/15
 */
public class DataPrivacyNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(DataPrivacyNode.class);

	private final EnhancedDataPrivacyService dataPrivacyService;

	private final DataPrivacyProperties dataPrivacyProperties;

	public DataPrivacyNode(EnhancedDataPrivacyService dataPrivacyService,
			DataPrivacyProperties dataPrivacyProperties) {
		this.dataPrivacyService = dataPrivacyService;
		this.dataPrivacyProperties = dataPrivacyProperties;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("数据脱敏节点开始处理...");

		Map<String, Object> updated = new HashMap<>();

		if (!dataPrivacyProperties.isEnabled()) {
			logger.info("数据脱敏功能已禁用，跳过处理");
			return updated;
		}

		// 1. 处理消息内容
		List<Message> messages = state.value("messages", List.class).map(list -> (List<Message>) list).orElse(List.of());

		if (!messages.isEmpty()) {
			List<Message> sanitizedMessages = messages.stream().map(this::sanitizeMessage).collect(Collectors.toList());
			updated.put("messages", sanitizedMessages);
			logger.info("已对 {} 条消息进行脱敏处理", sanitizedMessages.size());
		}

		// 2. 处理观察结果
		if (dataPrivacyProperties.isFilterIntermediateResults()) {
			List<String> observations = state.value("observations", List.class)
				.map(list -> (List<String>) list)
				.orElse(List.of());

			if (!observations.isEmpty()) {
				List<String> sanitizedObservations = observations.stream()
					.map(dataPrivacyService::sanitizeData)
					.collect(Collectors.toList());
				updated.put("observations", sanitizedObservations);
				logger.info("已对 {} 条观察结果进行脱敏处理", sanitizedObservations.size());
			}
		}

		// 3. 处理最终报告
		String finalReport = state.value("final_report", "");
		if (!finalReport.isEmpty()) {
			String sanitizedReport = dataPrivacyService.sanitizeData(finalReport);
			updated.put("final_report", sanitizedReport);

			// 生成脱敏报告
			var report = dataPrivacyService.getSanitizationReport(finalReport, sanitizedReport);
			if (report.hasSensitiveData()) {
				logger.warn("最终报告中发现敏感数据并已脱敏");
				updated.put("privacy_report", Map.of("has_sensitive_data", true, "original_length",
						report.getOriginalLength(), "sanitized_length", report.getSanitizedLength()));
			}
		}

		logger.info("数据脱敏节点处理完成");
		return updated;
	}

	/**
	 * 对消息进行脱敏处理
	 *
	 * @param message 原始消息
	 * @return 脱敏后的消息
	 */
	private Message sanitizeMessage(Message message) {
		if (message instanceof AssistantMessage assistantMessage) {
			String sanitizedText = dataPrivacyService.sanitizeData(assistantMessage.getText());
			return new AssistantMessage(sanitizedText);
		}
		return message;
	}

} 