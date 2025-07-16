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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for sensitive information filter
 *
 * @author Makoto
 */
@ConfigurationProperties(prefix = SensitiveFilterConstants.CONFIG_PREFIX)
public class SensitiveFilterProperties extends CommonToolCallProperties {

	private String replacement = "***";

	private boolean filterPhoneNumber = true;

	private boolean filterIdCard = true;

	private boolean filterBankCard = true;

	private boolean filterEmail = true;

	/**
	 * List of custom desensitization modes
	 */
	private List<CustomPattern> customPatterns = new ArrayList<>();

	/**
	 * Custom desensitization mode configuration
	 */
	public static class CustomPattern {

		private String name;

		private String pattern;

		private String replacement;

		private boolean enabled = true;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPattern() {
			return pattern;
		}

		public void setPattern(String pattern) {
			this.pattern = pattern;
		}

		public String getReplacement() {
			return replacement;
		}

		public void setReplacement(String replacement) {
			this.replacement = replacement;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	public boolean isFilterPhoneNumber() {
		return filterPhoneNumber;
	}

	public void setFilterPhoneNumber(boolean filterPhoneNumber) {
		this.filterPhoneNumber = filterPhoneNumber;
	}

	public boolean isFilterIdCard() {
		return filterIdCard;
	}

	public void setFilterIdCard(boolean filterIdCard) {
		this.filterIdCard = filterIdCard;
	}

	public boolean isFilterBankCard() {
		return filterBankCard;
	}

	public void setFilterBankCard(boolean filterBankCard) {
		this.filterBankCard = filterBankCard;
	}

	public boolean isFilterEmail() {
		return filterEmail;
	}

	public void setFilterEmail(boolean filterEmail) {
		this.filterEmail = filterEmail;
	}

	public List<CustomPattern> getCustomPatterns() {
		return customPatterns;
	}

	public void setCustomPatterns(List<CustomPattern> customPatterns) {
		this.customPatterns = customPatterns;
	}

}
