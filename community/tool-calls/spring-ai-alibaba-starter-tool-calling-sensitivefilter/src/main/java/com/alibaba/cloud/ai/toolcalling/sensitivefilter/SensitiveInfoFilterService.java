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
import com.alibaba.cloud.ai.toolcalling.sensitivefilter.SensitiveInfoFilterProperties.PatternConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sensitive Information Filtering Service
 *
 * @author Makoto
 */
public class SensitiveInfoFilterService
		implements Function<SensitiveInfoFilterService.Request, SensitiveInfoFilterService.Response> {

	// Chinese ID card number pattern
	private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[0-9Xx]|\\d{15}");

	// Chinese mobile phone number pattern
	private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

	// Credit card number pattern
	private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\d{16}|\\d{13}");

	// Email address pattern
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

	@Override
	public Response apply(Request request) {
		String text = request.getText();
		List<String> detectedTypes = new ArrayList<>();
		String filteredText = text;

		// Detect and filter ID card numbers
		if (request.isFilterIdCard()) {
			Matcher matcher = ID_CARD_PATTERN.matcher(filteredText);
			if (matcher.find()) {
				detectedTypes.add("ID_CARD");
				filteredText = matcher.replaceAll(id -> "******" + id.group().substring(id.group().length() - 4));
			}
		}

		// Detect and filter phone numbers
		if (request.isFilterPhone()) {
			Matcher matcher = PHONE_PATTERN.matcher(filteredText);
			if (matcher.find()) {
				detectedTypes.add("PHONE_NUMBER");
				filteredText = matcher
					.replaceAll(phone -> phone.group().substring(0, 3) + "****" + phone.group().substring(7));
			}
		}

		// Detect and filter credit card numbers
		if (request.isFilterCreditCard()) {
			Matcher matcher = CREDIT_CARD_PATTERN.matcher(filteredText);
			if (matcher.find()) {
				detectedTypes.add("CREDIT_CARD");
				filteredText = matcher
					.replaceAll(card -> "**** **** **** " + card.group().substring(card.group().length() - 4));
			}
		}

		// Detect and filter email addresses
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

		// Process custom sensitive patterns
		if (request.getCustomPatterns() != null && !request.getCustomPatterns().isEmpty()) {
			for (PatternConfig patternConfig : request.getCustomPatterns()) {
				try {
					Pattern customPattern = Pattern.compile(patternConfig.getPattern());
					Matcher matcher = customPattern.matcher(filteredText);
					if (matcher.find()) {
						detectedTypes.add("CUSTOM_PATTERN");
						String replacement = patternConfig.getReplacement() != null ? patternConfig.getReplacement()
								: "******";
						filteredText = matcher.replaceAll(replacement);
					}
				}
				catch (Exception e) {
					System.err.println("Error processing custom pattern: " + patternConfig.getPattern());
				}
			}
		}

		boolean containsSensitiveInfo = !detectedTypes.isEmpty();

		return new Response(filteredText, containsSensitiveInfo, detectedTypes);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Sensitive information filtering request for detecting and masking sensitive data in text")
	public static class Request {

		@JsonProperty(required = true)
		@JsonPropertyDescription("Text content to be filtered")
		private String text;

		@JsonProperty(required = false, defaultValue = "true")
		@JsonPropertyDescription("Whether to filter ID card numbers")
		private boolean filterIdCard = true;

		@JsonProperty(required = false, defaultValue = "true")
		@JsonPropertyDescription("Whether to filter phone numbers")
		private boolean filterPhone = true;

		@JsonProperty(required = false, defaultValue = "true")
		@JsonPropertyDescription("Whether to filter credit card numbers")
		private boolean filterCreditCard = true;

		@JsonProperty(required = false, defaultValue = "true")
		@JsonPropertyDescription("Whether to filter email addresses")
		private boolean filterEmail = true;

		@JsonProperty(required = false)
		@JsonPropertyDescription("List of custom sensitive information regex patterns")
		private List<PatternConfig> customPatterns;

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

		public List<PatternConfig> getCustomPatterns() {
			return customPatterns;
		}

		public void setCustomPatterns(List<PatternConfig> customPatterns) {
			this.customPatterns = customPatterns;
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Sensitive information filtering response")
	public static class Response {

		@JsonProperty
		@JsonPropertyDescription("Filtered text content")
		private final String filteredText;

		@JsonProperty
		@JsonPropertyDescription("Whether the text contains sensitive information")
		private final boolean containsSensitiveInfo;

		@JsonProperty
		@JsonPropertyDescription("List of detected sensitive information types")
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

