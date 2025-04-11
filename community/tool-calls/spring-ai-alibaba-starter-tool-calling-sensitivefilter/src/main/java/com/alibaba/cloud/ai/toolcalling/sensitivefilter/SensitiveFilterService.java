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

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Service for filtering sensitive information in text
 *
 * @author Makoto
 */
@Service
public class SensitiveFilterService {

	private final SensitiveFilterProperties properties;

	private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

	private static final Pattern ID_CARD_PATTERN = Pattern
		.compile("[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]");

	private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");

	private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

	public SensitiveFilterService(SensitiveFilterProperties properties) {
		this.properties = properties;
	}

	public String filter(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}

		String result = text;

		if (properties.isFilterPhoneNumber()) {
			result = PHONE_PATTERN.matcher(result).replaceAll(properties.getReplacement());
		}

		if (properties.isFilterIdCard()) {
			result = ID_CARD_PATTERN.matcher(result).replaceAll(properties.getReplacement());
		}

		if (properties.isFilterBankCard()) {
			result = BANK_CARD_PATTERN.matcher(result).replaceAll(properties.getReplacement());
		}

		if (properties.isFilterEmail()) {
			result = EMAIL_PATTERN.matcher(result).replaceAll(properties.getReplacement());
		}

		return result;
	}

}
