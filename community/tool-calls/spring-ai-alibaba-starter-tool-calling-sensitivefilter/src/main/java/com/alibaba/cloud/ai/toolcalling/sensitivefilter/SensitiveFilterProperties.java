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

/**
 * Configuration properties for sensitive information filter
 *
 * @author Makoto
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.toolcalling.sensitivefilter")
public class SensitiveFilterProperties {

	private boolean enabled = true;

	private String replacement = "***";

	private boolean filterPhoneNumber = true;

	private boolean filterIdCard = true;

	private boolean filterBankCard = true;

	private boolean filterEmail = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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

}
