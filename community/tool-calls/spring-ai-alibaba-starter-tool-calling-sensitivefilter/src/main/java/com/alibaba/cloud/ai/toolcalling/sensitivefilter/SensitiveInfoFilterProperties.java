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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Sensitive Information Filtering Configuration Attributes
 * @author Makoto
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.toolcalling.sensitivefilter")
public class SensitiveInfoFilterProperties {

	/**
	 * Whether to enable sensitive information filtering
	 */
	private boolean enabled = true;

	/**
	 * Whether to filter ID numbers
	 */
	private boolean filterIdCard = true;

	/**
	 * Whether to filter cell phone numbers
	 */
	private boolean filterPhone = true;

	/**
	 * Whether to filter credit card numbers
	 */
	private boolean filterCreditCard = true;

	/**
	 * Whether to filter mailboxes
	 */
	private boolean filterEmail = true;

	/**
	 * Global Customized Sensitive Information Regular Expression List
	 */
	private List<String> customPatterns;

	// getters and setters
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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
