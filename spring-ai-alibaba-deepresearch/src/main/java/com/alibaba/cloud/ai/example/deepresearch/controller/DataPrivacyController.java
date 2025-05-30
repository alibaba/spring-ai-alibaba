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

package com.alibaba.cloud.ai.example.deepresearch.controller;

import com.alibaba.cloud.ai.example.deepresearch.service.EnhancedDataPrivacyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据脱敏控制器
 *
 * @author deepresearch
 * @since 2025/1/15
 */
@RestController
@RequestMapping("/deep-research/privacy")
@ConditionalOnBean(EnhancedDataPrivacyService.class)
public class DataPrivacyController {

	@Autowired
	private EnhancedDataPrivacyService dataPrivacyService;

	/**
	 * 检查文本是否包含敏感信息并进行脱敏处理
	 *
	 * @param request 包含待检查文本的请求
	 * @return 脱敏处理结果
	 */
	@PostMapping("/check")
	public Map<String, Object> checkSensitiveData(@RequestBody Map<String, String> request) {
		String text = request.get("text");
		if (text == null || text.isEmpty()) {
			return Map.of("error", "文本内容不能为空");
		}

		boolean hasSensitive = dataPrivacyService.containsSensitiveData(text);
		String sanitized = dataPrivacyService.sanitizeData(text);
		var report = dataPrivacyService.getSanitizationReport(text, sanitized);

		return Map.of("has_sensitive_data", hasSensitive, "original_text", text, "sanitized_text", sanitized,
				"original_length", report.getOriginalLength(), "sanitized_length", report.getSanitizedLength());
	}

	/**
	 * 仅对文本进行脱敏处理
	 *
	 * @param request 包含待脱敏文本的请求
	 * @return 脱敏后的文本
	 */
	@PostMapping("/sanitize")
	public Map<String, Object> sanitizeText(@RequestBody Map<String, String> request) {
		String text = request.get("text");
		if (text == null || text.isEmpty()) {
			return Map.of("error", "文本内容不能为空");
		}

		String sanitized = dataPrivacyService.sanitizeData(text);
		return Map.of("sanitized_text", sanitized);
	}

	/**
	 * 检查文本是否包含敏感信息
	 *
	 * @param text 待检查的文本
	 * @return 检查结果
	 */
	@GetMapping("/contains-sensitive")
	public Map<String, Object> containsSensitiveData(@RequestParam("text") String text) {
		if (text == null || text.isEmpty()) {
			return Map.of("error", "文本内容不能为空");
		}

		boolean hasSensitive = dataPrivacyService.containsSensitiveData(text);
		return Map.of("has_sensitive_data", hasSensitive, "text_length", text.length());
	}

} 