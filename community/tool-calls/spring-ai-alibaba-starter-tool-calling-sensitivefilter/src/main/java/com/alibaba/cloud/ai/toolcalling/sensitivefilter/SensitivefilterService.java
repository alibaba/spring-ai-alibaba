/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.toolcalling.sensitivefilter;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 敏感信息过滤服务
 *
 * @author Makoto
 */
@Slf4j
public class SensitivefilterService
		implements Function<SensitivefilterService.Request, SensitivefilterService.Response> {

	// 中国身份证号码模式
	private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[0-9Xx]|\\d{15}");

	// 中国手机号模式
	private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

	// 信用卡号模式
	private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\d{16}|\\d{13}");

	// 邮箱地址模式
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

	@Override
	public Response apply(Request request) {
		String text = request.getText();
		List<String> detectedTypes = new ArrayList<>();
		String filteredText = text;

		// 检测并过滤身份证号
		if (request.isFilterIdCard()) {
			Matcher matcher = ID_CARD_PATTERN.matcher(filteredText);
			if (matcher.find()) {
				detectedTypes.add("ID_CARD");
				filteredText = matcher.replaceAll(id -> "******" + id.group().substring(id.group().length() - 4));
			}
		}

		// 检测并过滤手机号
		if (request.isFilterPhone()) {
			Matcher matcher = PHONE_PATTERN.matcher(filteredText);
			if (matcher.find()) {
				detectedTypes.add("PHONE_NUMBER");
				filteredText = matcher
					.replaceAll(phone -> phone.group().substring(0, 3) + "****" + phone.group().substring(7));
			}
		}

		// 检测并过滤信用卡号
		if (request.isFilterCreditCard()) {
			Matcher matcher = CREDIT_CARD_PATTERN.matcher(filteredText);
			if (matcher.find()) {
				detectedTypes.add("CREDIT_CARD");
				filteredText = matcher
					.replaceAll(card -> "**** **** **** " + card.group().substring(card.group().length() - 4));
			}
		}

		// 检测并过滤邮箱
		if (request.isFilterEmail()) {
			Matcher matcher = EMAIL_PATTERN.matcher(filteredText);
			if (matcher.find()) {
				detectedTypes.add("EMAIL");
				filteredText = matcher.replaceAll(email -> {
					String[] parts = email.group().split("@");
					return parts[0].substring(0, Math.min(3, parts[0].length())) + "****@" + parts[1];
				});
			}
		}

		// 处理自定义敏感词
		if (request.getCustomPatterns() != null && !request.getCustomPatterns().isEmpty()) {
			for (String pattern : request.getCustomPatterns()) {
				try {
					Pattern customPattern = Pattern.compile(pattern);
					Matcher matcher = customPattern.matcher(filteredText);
					if (matcher.find()) {
						detectedTypes.add("CUSTOM_PATTERN");
						filteredText = matcher.replaceAll("******");
					}
				}
				catch (Exception e) {
					log.error("Error processing custom pattern: {}", pattern, e);
				}
			}
		}

		boolean containsSensitiveInfo = !detectedTypes.isEmpty();

		return new Response(filteredText, containsSensitiveInfo, detectedTypes);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("敏感信息过滤请求，用于检测和屏蔽文本中的敏感信息")
	public static class Request {

		@JsonProperty(required = true)
		@JsonPropertyDescription("需要过滤的文本内容")
		private String text;

		@JsonProperty(required = false, defaultValue = "true")
		@JsonPropertyDescription("是否过滤身份证号码")
		private boolean filterIdCard = true;

		@JsonProperty(required = false, defaultValue = "true")
		@JsonPropertyDescription("是否过滤手机号码")
		private boolean filterPhone = true;

		@JsonProperty(required = false, defaultValue = "true")
		@JsonPropertyDescription("是否过滤信用卡号码")
		private boolean filterCreditCard = true;

		@JsonProperty(required = false, defaultValue = "true")
		@JsonPropertyDescription("是否过滤电子邮箱地址")
		private boolean filterEmail = true;

		@JsonProperty(required = false)
		@JsonPropertyDescription("自定义敏感信息正则表达式模式列表")
		private List<String> customPatterns;

		public Request() {
		}

		// getters and setters
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public boolean isFilterIdCard() {
			return filterIdCard;
		}

		public void setFilterIdCard(boolean filterIdCard) {
			this.filterIdCard = filterIdCard;
		}

		public boolean isFilterPhone() {
			return filterPhone;
		}

		public void setFilterPhone(boolean filterPhone) {
			this.filterPhone = filterPhone;
		}

		public boolean isFilterCreditCard() {
			return filterCreditCard;
		}

		public void setFilterCreditCard(boolean filterCreditCard) {
			this.filterCreditCard = filterCreditCard;
		}

		public boolean isFilterEmail() {
			return filterEmail;
		}

		public void setFilterEmail(boolean filterEmail) {
			this.filterEmail = filterEmail;
		}

		public List<String> getCustomPatterns() {
			return customPatterns;
		}

		public void setCustomPatterns(List<String> customPatterns) {
			this.customPatterns = customPatterns;
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("敏感信息过滤响应")
	public static class Response {

		@JsonProperty
		@JsonPropertyDescription("过滤后的文本内容")
		private final String filteredText;

		@JsonProperty
		@JsonPropertyDescription("是否包含敏感信息")
		private final boolean containsSensitiveInfo;

		@JsonProperty
		@JsonPropertyDescription("检测到的敏感信息类型列表")
		private final List<String> detectedTypes;

		public Response(String filteredText, boolean containsSensitiveInfo, List<String> detectedTypes) {
			this.filteredText = filteredText;
			this.containsSensitiveInfo = containsSensitiveInfo;
			this.detectedTypes = detectedTypes;
		}

		public String getFilteredText() {
			return filteredText;
		}

		public boolean isContainsSensitiveInfo() {
			return containsSensitiveInfo;
		}

		public List<String> getDetectedTypes() {
			return detectedTypes;
		}

	}

}