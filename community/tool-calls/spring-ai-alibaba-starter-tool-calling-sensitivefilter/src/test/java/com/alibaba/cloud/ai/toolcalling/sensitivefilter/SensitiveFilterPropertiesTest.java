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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感词过滤配置属性测试类
 *
 * @author Makoto
 */
@DisplayName("敏感词过滤配置属性测试")
class SensitiveFilterPropertiesTest {

	private SensitiveFilterProperties properties;

	@BeforeEach
	void setUp() {
		properties = new SensitiveFilterProperties();
	}

	@Test
	@DisplayName("测试默认配置值")
	void testDefaultValues() {
		assertEquals("***", properties.getReplacement());
		assertTrue(properties.isFilterPhoneNumber());
		assertTrue(properties.isFilterIdCard());
		assertTrue(properties.isFilterBankCard());
		assertTrue(properties.isFilterEmail());
		assertNotNull(properties.getCustomPatterns());
		assertTrue(properties.getCustomPatterns().isEmpty());
	}

	@Test
	@DisplayName("测试设置和获取替换字符")
	void testReplacementProperty() {
		String replacement = "[HIDDEN]";
		properties.setReplacement(replacement);
		assertEquals(replacement, properties.getReplacement());
	}

	@Test
	@DisplayName("测试设置和获取手机号过滤开关")
	void testFilterPhoneNumberProperty() {
		properties.setFilterPhoneNumber(false);
		assertFalse(properties.isFilterPhoneNumber());

		properties.setFilterPhoneNumber(true);
		assertTrue(properties.isFilterPhoneNumber());
	}

	@Test
	@DisplayName("测试设置和获取身份证过滤开关")
	void testFilterIdCardProperty() {
		properties.setFilterIdCard(false);
		assertFalse(properties.isFilterIdCard());

		properties.setFilterIdCard(true);
		assertTrue(properties.isFilterIdCard());
	}

	@Test
	@DisplayName("测试设置和获取银行卡过滤开关")
	void testFilterBankCardProperty() {
		properties.setFilterBankCard(false);
		assertFalse(properties.isFilterBankCard());

		properties.setFilterBankCard(true);
		assertTrue(properties.isFilterBankCard());
	}

	@Test
	@DisplayName("测试设置和获取邮箱过滤开关")
	void testFilterEmailProperty() {
		properties.setFilterEmail(false);
		assertFalse(properties.isFilterEmail());

		properties.setFilterEmail(true);
		assertTrue(properties.isFilterEmail());
	}

	@Test
	@DisplayName("测试自定义模式配置")
	void testCustomPatterns() {
		SensitiveFilterProperties.CustomPattern pattern1 = new SensitiveFilterProperties.CustomPattern();
		pattern1.setName("qq");
		pattern1.setPattern("\\d{5,11}");
		pattern1.setReplacement("[QQ]");
		pattern1.setEnabled(true);

		SensitiveFilterProperties.CustomPattern pattern2 = new SensitiveFilterProperties.CustomPattern();
		pattern2.setName("wechat");
		pattern2.setPattern("wxid_[a-zA-Z0-9]+");
		pattern2.setReplacement("[微信号]");
		pattern2.setEnabled(false);

		List<SensitiveFilterProperties.CustomPattern> patterns = Arrays.asList(pattern1, pattern2);
		properties.setCustomPatterns(patterns);

		assertEquals(2, properties.getCustomPatterns().size());
		assertEquals("qq", properties.getCustomPatterns().get(0).getName());
		assertEquals("wechat", properties.getCustomPatterns().get(1).getName());
	}

	@Test
	@DisplayName("测试CustomPattern类的所有属性")
	void testCustomPatternClass() {
		SensitiveFilterProperties.CustomPattern pattern = new SensitiveFilterProperties.CustomPattern();

		// 测试默认值
		assertTrue(pattern.isEnabled());
		assertNull(pattern.getName());
		assertNull(pattern.getPattern());
		assertNull(pattern.getReplacement());

		// 测试设置和获取值
		pattern.setName("test");
		pattern.setPattern("\\d+");
		pattern.setReplacement("[数字]");
		pattern.setEnabled(false);

		assertEquals("test", pattern.getName());
		assertEquals("\\d+", pattern.getPattern());
		assertEquals("[数字]", pattern.getReplacement());
		assertFalse(pattern.isEnabled());
	}

	@Test
	@DisplayName("测试空的自定义模式列表")
	void testEmptyCustomPatterns() {
		properties.setCustomPatterns(new ArrayList<>());
		assertTrue(properties.getCustomPatterns().isEmpty());
	}

	@Test
	@DisplayName("测试null的替换字符")
	void testNullReplacement() {
		properties.setReplacement(null);
		assertNull(properties.getReplacement());
	}

	@Test
	@DisplayName("测试空字符串替换字符")
	void testEmptyReplacement() {
		properties.setReplacement("");
		assertEquals("", properties.getReplacement());
	}

	@Test
	@DisplayName("测试配置前缀常量")
	void testConfigurationPrefix() {
		// 通过反射检查前缀常量是否正确
		assertEquals("spring.ai.alibaba.toolcalling.sensitivefilter",
				SensitiveFilterProperties.SENSITIVE_FILTER_PREFIX);
	}

}