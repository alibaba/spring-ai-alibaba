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

import com.alibaba.cloud.ai.example.deepresearch.config.DataPrivacyProperties;
import com.alibaba.cloud.ai.toolcalling.sensitivefilter.SensitiveFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 增强的数据脱敏服务
 * 
 * 现在主要依赖于增强后的SensitiveFilterService，提供额外的企业级脱敏功能
 *
 * @author deepresearch
 * @since 2025/1/15
 */
@Service
@ConditionalOnBean(SensitiveFilterService.class)
public class EnhancedDataPrivacyService {

	private static final Logger logger = LoggerFactory.getLogger(EnhancedDataPrivacyService.class);

	private final SensitiveFilterService sensitiveFilterService;

	private final DataPrivacyProperties dataPrivacyProperties;

	public EnhancedDataPrivacyService(SensitiveFilterService sensitiveFilterService,
			DataPrivacyProperties dataPrivacyProperties) {
		this.sensitiveFilterService = sensitiveFilterService;
		this.dataPrivacyProperties = dataPrivacyProperties;
	}

	/**
	 * 全面的数据脱敏处理
	 * 
	 * 现在主要使用SensitiveFilterService（已支持自定义正则表达式），
	 * 并添加一些额外的企业级脱敏规则
	 *
	 * @param text 待脱敏的文本
	 * @return 脱敏后的文本
	 */
	public String sanitizeData(String text) {
		if (!dataPrivacyProperties.isEnabled() || text == null || text.isEmpty()) {
			return text;
		}

		try {
			// 1. 使用增强的敏感词过滤器（现在支持自定义正则表达式）
			String result = sensitiveFilterService.apply(text);

			// 2. 应用额外的企业级脱敏规则
			result = applyEnterprisePrivacyRules(result);

			return result;
		}
		catch (Exception e) {
			logger.error("数据脱敏处理异常: {}", e.getMessage());
			return text; // 出现异常时返回原文本
		}
	}

	/**
	 * 企业级隐私规则（补充规则）
	 * 
	 * 这些规则补充sensitivefilter中未覆盖的场景
	 *
	 * @param text 待处理文本
	 * @return 处理后文本
	 */
	private String applyEnterprisePrivacyRules(String text) {
		String result = text;

		try {
			// URL中的敏感参数脱敏（如果sensitivefilter中没有配置）
			Pattern urlParamPattern = Pattern.compile("([?&](?:token|key|password|secret|apikey)=)[^&\\s]+", Pattern.CASE_INSENSITIVE);
			result = urlParamPattern.matcher(result).replaceAll("$1***");

			// 公司内部代码/编号脱敏（假设格式为字母+数字）
			Pattern internalCodePattern = Pattern.compile("\\b[A-Z]{2,4}\\d{6,10}\\b");
			result = internalCodePattern.matcher(result).replaceAll("***-INTERNAL-***");

			// MAC地址脱敏
			Pattern macAddressPattern = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
			result = macAddressPattern.matcher(result).replaceAll("**:**:**:**:**:**");

		}
		catch (Exception e) {
			logger.error("应用企业级隐私规则异常: {}", e.getMessage());
		}

		return result;
	}

	/**
	 * 检查文本是否包含敏感信息
	 *
	 * @param text 待检查文本
	 * @return 是否包含敏感信息
	 */
	public boolean containsSensitiveData(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}

		String sanitized = sanitizeData(text);
		return !text.equals(sanitized);
	}

	/**
	 * 获取脱敏统计信息
	 *
	 * @param originalText 原始文本
	 * @param sanitizedText 脱敏后文本
	 * @return 脱敏报告
	 */
	public SanitizationReport getSanitizationReport(String originalText, String sanitizedText) {
		return new SanitizationReport(originalText.length(), sanitizedText.length(),
				!originalText.equals(sanitizedText));
	}

	/**
	 * 脱敏报告
	 */
	public static class SanitizationReport {

		private final int originalLength;

		private final int sanitizedLength;

		private final boolean hasSensitiveData;

		public SanitizationReport(int originalLength, int sanitizedLength, boolean hasSensitiveData) {
			this.originalLength = originalLength;
			this.sanitizedLength = sanitizedLength;
			this.hasSensitiveData = hasSensitiveData;
		}

		public int getOriginalLength() {
			return originalLength;
		}

		public int getSanitizedLength() {
			return sanitizedLength;
		}

		public boolean hasSensitiveData() {
			return hasSensitiveData;
		}

	}

} 