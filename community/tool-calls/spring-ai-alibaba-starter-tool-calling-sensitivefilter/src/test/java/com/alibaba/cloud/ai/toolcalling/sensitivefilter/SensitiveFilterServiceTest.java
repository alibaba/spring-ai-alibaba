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
 * SensitiveFilterService unit tests
 *
 * @author Makoto
 */
@DisplayName("Sensitive information filtering service tests")
class SensitiveFilterServiceTest {

	private SensitiveFilterProperties properties;

	private SensitiveFilterService service;

	@BeforeEach
	void setUp() {
		properties = new SensitiveFilterProperties();
		service = new SensitiveFilterService(properties);
	}

	@Test
	@DisplayName("Test phone number de-identification")
	void testPhoneNumberFiltering() {
		String text = "我的手机号是13912345678，请联系我";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的手机号是***，请联系我");
	}

	@Test
	@DisplayName("Test ID card number de-identification")
	void testIdCardFiltering() {
		String text = "我的身份证号是110101199001011234";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的身份证号是***");
	}

	@Test
	@DisplayName("Test bank card number de-identification")
	void testBankCardFiltering() {
		String text = "我的银行卡号是4123456789012345";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的银行卡号是***");
	}

	@Test
	@DisplayName("Test email de-identification")
	void testEmailFiltering() {
		String text = "我的邮箱是user@example.com";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的邮箱是***");
	}

	@Test
	@DisplayName("Test mixed sensitive information de-identification")
	void testMixedSensitiveInfoFiltering() {
		String text = "联系方式：手机13912345678，邮箱user@example.com，身份证110101199001011234";
		String result = service.apply(text);
		assertThat(result).isEqualTo("联系方式：手机***，邮箱***，身份证***");
	}

	@Test
	@DisplayName("Test custom de-identification rules")
	void testCustomPatternFiltering() {
		// Configure custom QQ number de-identification rule
		SensitiveFilterProperties.CustomPattern qqPattern = new SensitiveFilterProperties.CustomPattern();
		qqPattern.setName("qq");
		qqPattern.setPattern("QQ[：:]?\\d{5,11}");
		qqPattern.setReplacement("[QQ号]");
		qqPattern.setEnabled(true);

		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();
		customPatterns.add(qqPattern);
		properties.setCustomPatterns(customPatterns);

		service = new SensitiveFilterService(properties);

		String text = "我的QQ：123456789";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的[QQ号]");
	}

	@Test
	@DisplayName("Test custom replacement text")
	void testCustomReplacement() {
		properties.setReplacement("[已脱敏]");
		service = new SensitiveFilterService(properties);

		String text = "手机号：13912345678";
		String result = service.apply(text);
		assertThat(result).isEqualTo("手机号：[已脱敏]");
	}

	@Test
	@DisplayName("Test disable phone number filtering")
	void testDisablePhoneNumberFiltering() {
		properties.setFilterPhoneNumber(false);
		service = new SensitiveFilterService(properties);

		String text = "手机号：13912345678";
		String result = service.apply(text);
		assertThat(result).isEqualTo("手机号：13912345678");
	}

	@Test
	@DisplayName("Test disable ID card number filtering")
	void testDisableIdCardFiltering() {
		properties.setFilterIdCard(false);
		service = new SensitiveFilterService(properties);

		String text = "身份证：110101199001011234";
		String result = service.apply(text);
		assertThat(result).isEqualTo("身份证：110101199001011234");
	}

	@Test
	@DisplayName("Test disable bank card number filtering")
	void testDisableBankCardFiltering() {
		properties.setFilterBankCard(false);
		service = new SensitiveFilterService(properties);

		String text = "银行卡：4123456789012345";
		String result = service.apply(text);
		assertThat(result).isEqualTo("银行卡：4123456789012345");
	}

	@Test
	@DisplayName("Test disable email filtering")
	void testDisableEmailFiltering() {
		properties.setFilterEmail(false);
		service = new SensitiveFilterService(properties);

		String text = "邮箱：user@example.com";
		String result = service.apply(text);
		assertThat(result).isEqualTo("邮箱：user@example.com");
	}

	@Test
	@DisplayName("Test null and empty input")
	void testNullAndEmptyInput() {
		assertThat(service.apply(null)).isNull();
		assertThat(service.apply("")).isEmpty();
		assertThat(service.apply("   ")).isEqualTo("   ");
	}

	@Test
	@DisplayName("Test text without sensitive information")
	void testTextWithoutSensitiveInfo() {
		String text = "This is a normal text, without any sensitive information";
		String result = service.apply(text);
		assertThat(result).isEqualTo(text);
	}

	@Test
	@DisplayName("Test boundary case - incomplete phone number")
	void testIncompletePhoneNumber() {
		String text = "Incomplete phone number: 139123456";
		String result = service.apply(text);
		assertThat(result).isEqualTo("Incomplete phone number: 139123456");
	}

	@Test
	@DisplayName("Test boundary case - incomplete ID card number")
	void testIncompleteIdCard() {
		String text = "Incomplete ID card: 11010119900101";
		String result = service.apply(text);
		assertThat(result).isEqualTo("Incomplete ID card: 11010119900101");
	}

	@Test
	@DisplayName("Test Function interface implementation")
	void testFunctionInterface() {
		java.util.function.Function<String, String> function = service;
		String result = function.apply("手机：13912345678");
		assertThat(result).isEqualTo("手机：***");
	}

	@Test
	@DisplayName("Test invalid custom pattern")
	void testInvalidCustomPattern() {
		SensitiveFilterProperties.CustomPattern invalidPattern = new SensitiveFilterProperties.CustomPattern();
		invalidPattern.setName("invalid");
		invalidPattern.setPattern("[invalid"); // invalid regular expression
		invalidPattern.setEnabled(true);

		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();
		customPatterns.add(invalidPattern);
		properties.setCustomPatterns(customPatterns);

		// Should not throw an exception, but ignore invalid rules
		service = new SensitiveFilterService(properties);
		String text = "Test text";
		String result = service.apply(text);
		assertThat(result).isEqualTo("Test text");
	}

	@Test
	@DisplayName("Test disabled custom pattern")
	void testDisabledCustomPattern() {
		SensitiveFilterProperties.CustomPattern disabledPattern = new SensitiveFilterProperties.CustomPattern();
		disabledPattern.setName("disabled");
		disabledPattern.setPattern("test");
		disabledPattern.setEnabled(false);

		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();
		customPatterns.add(disabledPattern);
		properties.setCustomPatterns(customPatterns);

		service = new SensitiveFilterService(properties);

		String text = "test content";
		String result = service.apply(text);
		assertThat(result).isEqualTo("test content");
	}

	@Test
	@DisplayName("Test complex scenario - avoid false matches")
	void testAvoidFalseMatches() {
		// Test should not be mis-matched
		String text = "商品编号：123456789012345，这不是银行卡号";
		String result = service.apply(text);
		// Should remain unchanged, because it is not a bank card number starting with
		// 4-6
		assertThat(result).isEqualTo(text);
	}

}
