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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveFilterProperties unit tests
 *
 * @author Makoto
 */
@DisplayName("Sensitive information filtering configuration properties test")
class SensitiveFilterPropertiesTest {

	private SensitiveFilterProperties properties;

	@BeforeEach
	void setUp() {
		properties = new SensitiveFilterProperties();
	}

	@Test
	@DisplayName("Test default configuration values")
	void testDefaultValues() {
		assertThat(properties.getReplacement()).isEqualTo("***");
		assertThat(properties.isFilterPhoneNumber()).isTrue();
		assertThat(properties.isFilterIdCard()).isTrue();
		assertThat(properties.isFilterBankCard()).isTrue();
		assertThat(properties.isFilterEmail()).isTrue();
		assertThat(properties.getCustomPatterns()).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("Test replacement text setting")
	void testReplacementSetting() {
		properties.setReplacement("[HIDDEN]");
		assertThat(properties.getReplacement()).isEqualTo("[HIDDEN]");
	}

	@Test
	@DisplayName("Test phone number filter toggle")
	void testPhoneNumberFilterToggle() {
		properties.setFilterPhoneNumber(false);
		assertThat(properties.isFilterPhoneNumber()).isFalse();

		properties.setFilterPhoneNumber(true);
		assertThat(properties.isFilterPhoneNumber()).isTrue();
	}

	@Test
	@DisplayName("Test ID card number filter toggle")
	void testIdCardFilterToggle() {
		properties.setFilterIdCard(false);
		assertThat(properties.isFilterIdCard()).isFalse();

		properties.setFilterIdCard(true);
		assertThat(properties.isFilterIdCard()).isTrue();
	}

	@Test
	@DisplayName("Test bank card number filter toggle")
	void testBankCardFilterToggle() {
		properties.setFilterBankCard(false);
		assertThat(properties.isFilterBankCard()).isFalse();

		properties.setFilterBankCard(true);
		assertThat(properties.isFilterBankCard()).isTrue();
	}

	@Test
	@DisplayName("Test email filter toggle")
	void testEmailFilterToggle() {
		properties.setFilterEmail(false);
		assertThat(properties.isFilterEmail()).isFalse();

		properties.setFilterEmail(true);
		assertThat(properties.isFilterEmail()).isTrue();
	}

	@Test
	@DisplayName("Test custom pattern list setting")
	void testCustomPatternsList() {
		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();

		SensitiveFilterProperties.CustomPattern pattern1 = new SensitiveFilterProperties.CustomPattern();
		pattern1.setName("test1");
		pattern1.setPattern("\\d{4}");
		customPatterns.add(pattern1);

		properties.setCustomPatterns(customPatterns);
		assertThat(properties.getCustomPatterns()).hasSize(1);
		assertThat(properties.getCustomPatterns().get(0).getName()).isEqualTo("test1");
	}

	@Test
	@DisplayName("Test custom pattern - basic properties")
	void testCustomPatternBasicProperties() {
		SensitiveFilterProperties.CustomPattern pattern = new SensitiveFilterProperties.CustomPattern();

		pattern.setName("testPattern");
		assertThat(pattern.getName()).isEqualTo("testPattern");

		pattern.setPattern("\\d+");
		assertThat(pattern.getPattern()).isEqualTo("\\d+");

		pattern.setReplacement("[NUM]");
		assertThat(pattern.getReplacement()).isEqualTo("[NUM]");
	}

	@Test
	@DisplayName("Test custom pattern - enabled state")
	void testCustomPatternEnabledState() {
		SensitiveFilterProperties.CustomPattern pattern = new SensitiveFilterProperties.CustomPattern();

		// default enabled
		assertThat(pattern.isEnabled()).isTrue();

		pattern.setEnabled(false);
		assertThat(pattern.isEnabled()).isFalse();

		pattern.setEnabled(true);
		assertThat(pattern.isEnabled()).isTrue();
	}

	@Test
	@DisplayName("Test configuration prefix constant")
	void testConfigurationPrefix() {
		// Verify prefix constant through reflection or direct access
		assertThat(SensitiveFilterConstants.CONFIG_PREFIX).contains("toolcalling").contains("sensitivefilter");
	}

	@Test
	@DisplayName("Test complex custom pattern configuration")
	void testComplexCustomPatternConfiguration() {
		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();

		// QQ number pattern
		SensitiveFilterProperties.CustomPattern qqPattern = new SensitiveFilterProperties.CustomPattern();
		qqPattern.setName("qq");
		qqPattern.setPattern("QQ[：:]?\\d{5,11}");
		qqPattern.setReplacement("[QQ]");
		qqPattern.setEnabled(true);

		// Wechat number pattern
		SensitiveFilterProperties.CustomPattern wechatPattern = new SensitiveFilterProperties.CustomPattern();
		wechatPattern.setName("wechat");
		wechatPattern.setPattern("微信[：:]?[a-zA-Z][a-zA-Z0-9_-]{5,19}");
		wechatPattern.setReplacement("[微信]");
		wechatPattern.setEnabled(false);

		customPatterns.add(qqPattern);
		customPatterns.add(wechatPattern);

		properties.setCustomPatterns(customPatterns);

		assertThat(properties.getCustomPatterns()).hasSize(2);

		SensitiveFilterProperties.CustomPattern retrievedQq = properties.getCustomPatterns().get(0);
		assertThat(retrievedQq.getName()).isEqualTo("qq");
		assertThat(retrievedQq.getPattern()).isEqualTo("QQ[：:]?\\d{5,11}");
		assertThat(retrievedQq.getReplacement()).isEqualTo("[QQ]");
		assertThat(retrievedQq.isEnabled()).isTrue();

		SensitiveFilterProperties.CustomPattern retrievedWechat = properties.getCustomPatterns().get(1);
		assertThat(retrievedWechat.getName()).isEqualTo("wechat");
		assertThat(retrievedWechat.isEnabled()).isFalse();
	}

	@Test
	@DisplayName("Test null value handling")
	void testNullValueHandling() {
		properties.setReplacement(null);
		assertThat(properties.getReplacement()).isNull();

		properties.setCustomPatterns(null);
		assertThat(properties.getCustomPatterns()).isNull();

		SensitiveFilterProperties.CustomPattern pattern = new SensitiveFilterProperties.CustomPattern();
		pattern.setName(null);
		pattern.setPattern(null);
		pattern.setReplacement(null);

		assertThat(pattern.getName()).isNull();
		assertThat(pattern.getPattern()).isNull();
		assertThat(pattern.getReplacement()).isNull();
	}

}
