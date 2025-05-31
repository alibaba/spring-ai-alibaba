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
 * 敏感词过滤服务测试类
 *
 * @author Makoto
 */
@DisplayName("敏感词过滤服务测试")
class SensitiveFilterServiceTest {

	private SensitiveFilterService sensitiveFilterService;

	private SensitiveFilterProperties properties;

	@BeforeEach
	void setUp() {
		properties = new SensitiveFilterProperties();
		properties.setReplacement("***");
		properties.setFilterPhoneNumber(true);
		properties.setFilterIdCard(true);
		properties.setFilterBankCard(true);
		properties.setFilterEmail(true);
		properties.setCustomPatterns(new ArrayList<>());

		sensitiveFilterService = new SensitiveFilterService(properties);
	}

	@Test
	@DisplayName("测试手机号过滤")
	void testFilterPhoneNumber() {
		String input = "我的手机号是13812345678，请联系我";
		String expected = "我的手机号是***，请联系我";
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试多个手机号过滤")
	void testFilterMultiplePhoneNumbers() {
		String input = "张三的手机号是13812345678，李四的手机号是15987654321";
		String expected = "张三的手机号是***，李四的手机号是***";
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试身份证号过滤")
	void testFilterIdCard() {
		String input = "我的身份证号是110101199001011234";
		String expected = "我的身份证号是***";
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试银行卡号过滤")
	void testFilterBankCard() {
		String input = "我的银行卡号是6222021234567890123";
		String expected = "我的银行卡号是***";
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试邮箱过滤")
	void testFilterEmail() {
		String input = "我的邮箱是test@example.com，请发送邮件到admin@company.org";
		String expected = "我的邮箱是***，请发送邮件到***";
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试混合敏感信息过滤")
	void testFilterMixedSensitiveInfo() {
		String input = "张三，手机：13812345678，身份证：110101199001011234，邮箱：zhangsan@example.com";
		String expected = "张三，手机：***，身份证：***，邮箱：***";
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试自定义过滤规则")
	void testCustomPatterns() {
		// 设置自定义过滤规则 - 过滤QQ号，使用更精确的正则表达式
		SensitiveFilterProperties.CustomPattern customPattern = new SensitiveFilterProperties.CustomPattern();
		customPattern.setName("qq");
		customPattern.setPattern("QQ[：:]?\\d{5,11}");
		customPattern.setReplacement("[QQ号]");
		customPattern.setEnabled(true);

		properties.setCustomPatterns(Arrays.asList(customPattern));
		sensitiveFilterService = new SensitiveFilterService(properties);

		String input = "我的QQ：123456789";
		String expected = "我的[QQ号]";
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试禁用的自定义规则")
	void testDisabledCustomPattern() {
		SensitiveFilterProperties.CustomPattern customPattern = new SensitiveFilterProperties.CustomPattern();
		customPattern.setName("qq");
		customPattern.setPattern("QQ[：:]?\\d{5,11}");
		customPattern.setReplacement("[QQ号]");
		customPattern.setEnabled(false);

		properties.setCustomPatterns(Arrays.asList(customPattern));
		sensitiveFilterService = new SensitiveFilterService(properties);

		String input = "我的QQ：123456789";
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result); // 应该不被过滤
	}

	@Test
	@DisplayName("测试关闭手机号过滤")
	void testDisablePhoneNumberFilter() {
		properties.setFilterPhoneNumber(false);
		sensitiveFilterService = new SensitiveFilterService(properties);

		String input = "我的手机号是13812345678";
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result); // 应该不被过滤
	}

	@Test
	@DisplayName("测试关闭身份证过滤")
	void testDisableIdCardFilter() {
		properties.setFilterIdCard(false);
		sensitiveFilterService = new SensitiveFilterService(properties);

		String input = "我的身份证号是110101199001011234";
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result); // 应该不被过滤
	}

	@Test
	@DisplayName("测试关闭银行卡过滤")
	void testDisableBankCardFilter() {
		properties.setFilterBankCard(false);
		sensitiveFilterService = new SensitiveFilterService(properties);

		String input = "我的银行卡号是6222021234567890123";
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result); // 应该不被过滤
	}

	@Test
	@DisplayName("测试关闭邮箱过滤")
	void testDisableEmailFilter() {
		properties.setFilterEmail(false);
		sensitiveFilterService = new SensitiveFilterService(properties);

		String input = "我的邮箱是test@example.com";
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result); // 应该不被过滤
	}

	@Test
	@DisplayName("测试空字符串输入")
	void testEmptyInput() {
		String result = sensitiveFilterService.apply("");
		assertEquals("", result);
	}

	@Test
	@DisplayName("测试null输入")
	void testNullInput() {
		String result = sensitiveFilterService.apply(null);
		assertNull(result);
	}

	@Test
	@DisplayName("测试无敏感信息的文本")
	void testNoSensitiveInfo() {
		String input = "这是一段普通的文本，没有敏感信息";
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result);
	}

	@Test
	@DisplayName("测试自定义替换字符")
	void testCustomReplacement() {
		properties.setReplacement("[HIDDEN]");
		sensitiveFilterService = new SensitiveFilterService(properties);

		String input = "手机号：13812345678";
		String expected = "手机号：[HIDDEN]";
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试错误的自定义正则表达式")
	void testInvalidCustomPattern() {
		SensitiveFilterProperties.CustomPattern customPattern = new SensitiveFilterProperties.CustomPattern();
		customPattern.setName("invalid");
		customPattern.setPattern("["); // 无效的正则表达式
		customPattern.setEnabled(true);

		properties.setCustomPatterns(Arrays.asList(customPattern));

		// 应该能正常创建服务，但不会应用无效的正则表达式
		assertDoesNotThrow(() -> {
			sensitiveFilterService = new SensitiveFilterService(properties);
		});

		String input = "测试文本";
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result);
	}

	@Test
	@DisplayName("测试边界情况 - 不完整的手机号")
	void testIncompletePhoneNumber() {
		String input = "我的手机号是138123456"; // 只有9位
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result); // 不应该被过滤
	}

	@Test
	@DisplayName("测试边界情况 - 不完整的身份证号")
	void testIncompleteIdCard() {
		String input = "我的身份证号是11010119900101123"; // 少一位
		String result = sensitiveFilterService.apply(input);
		assertEquals(input, result); // 不应该被过滤
	}

	@Test
	@DisplayName("测试Function接口实现")
	void testFunctionInterface() {
		// 测试SensitiveFilterService作为Function的使用
		String input = "手机号：13812345678";
		String expected = "手机号：***";

		// 使用Function接口方法
		String result = sensitiveFilterService.apply(input);
		assertEquals(expected, result);

		// 验证Function接口的类型
		assertTrue(sensitiveFilterService instanceof java.util.function.Function);
	}

}