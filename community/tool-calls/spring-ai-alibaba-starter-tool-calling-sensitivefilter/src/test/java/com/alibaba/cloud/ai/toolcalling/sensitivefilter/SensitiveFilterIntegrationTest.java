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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感词过滤组件集成测试类
 *
 * @author Makoto
 */
@SpringBootTest(classes = { SensitiveFilterAutoConfiguration.class })
@TestPropertySource(properties = { "spring.ai.alibaba.toolcalling.sensitivefilter.enabled=true",
		"spring.ai.alibaba.toolcalling.sensitivefilter.replacement=[MASKED]",
		"spring.ai.alibaba.toolcalling.sensitivefilter.filter-phone-number=true",
		"spring.ai.alibaba.toolcalling.sensitivefilter.filter-id-card=true",
		"spring.ai.alibaba.toolcalling.sensitivefilter.filter-bank-card=true",
		"spring.ai.alibaba.toolcalling.sensitivefilter.filter-email=true",
		"spring.ai.alibaba.toolcalling.sensitivefilter.custom-patterns[0].name=qq",
		"spring.ai.alibaba.toolcalling.sensitivefilter.custom-patterns[0].pattern=QQ[：:]?\\\\d{5,11}",
		"spring.ai.alibaba.toolcalling.sensitivefilter.custom-patterns[0].replacement=[QQ号]",
		"spring.ai.alibaba.toolcalling.sensitivefilter.custom-patterns[0].enabled=true" })
@DisplayName("敏感词过滤组件集成测试")
class SensitiveFilterIntegrationTest {

	@Autowired
	private SensitiveFilterService sensitiveFilterService;

	@Autowired
	private SensitiveFilterProperties sensitiveFilterProperties;

	@Test
	@DisplayName("测试服务Bean注入成功")
	void testServiceBeanInjection() {
		assertNotNull(sensitiveFilterService);
		assertNotNull(sensitiveFilterProperties);
	}

	@Test
	@DisplayName("测试配置属性正确加载")
	void testPropertiesLoading() {
		assertEquals("[MASKED]", sensitiveFilterProperties.getReplacement());
		assertTrue(sensitiveFilterProperties.isFilterPhoneNumber());
		assertTrue(sensitiveFilterProperties.isFilterIdCard());
		assertTrue(sensitiveFilterProperties.isFilterBankCard());
		assertTrue(sensitiveFilterProperties.isFilterEmail());

		assertEquals(1, sensitiveFilterProperties.getCustomPatterns().size());
		SensitiveFilterProperties.CustomPattern customPattern = sensitiveFilterProperties.getCustomPatterns().get(0);
		assertEquals("qq", customPattern.getName());
		assertEquals("QQ[：:]?\\d{5,11}", customPattern.getPattern());
		assertEquals("[QQ号]", customPattern.getReplacement());
		assertTrue(customPattern.isEnabled());
	}

	@Test
	@DisplayName("测试完整的敏感信息过滤流程")
	void testCompleteFilteringFlow() {
		String input = "用户信息：手机号13812345678，身份证110101199001011234，邮箱test@example.com，银行卡6222021234567890123，QQ：123456789";

		String result = sensitiveFilterService.apply(input);

		String expected = "用户信息：手机号[MASKED]，身份证[MASKED]，邮箱[MASKED]，银行卡[MASKED]，[QQ号]";
		assertEquals(expected, result);
	}

	@Test
	@DisplayName("测试Function接口在Spring环境中的使用")
	void testFunctionInterfaceInSpringContext() {
		// 测试可以作为Function使用
		java.util.function.Function<String, String> function = sensitiveFilterService;

		String input = "手机号：13812345678";
		String result = function.apply(input);

		assertEquals("手机号：[MASKED]", result);
	}

	@Test
	@DisplayName("测试性能 - 批量处理")
	void testPerformanceWithBatchProcessing() {
		String[] inputs = { "手机号：13812345678", "身份证：110101199001011234", "邮箱：test@example.com",
				"银行卡：6222021234567890123", "QQ：123456789" };

		long startTime = System.currentTimeMillis();

		for (int i = 0; i < 1000; i++) {
			for (String input : inputs) {
				sensitiveFilterService.apply(input);
			}
		}

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;

		// 5000次调用应该在合理时间内完成（这里设置为5秒）
		assertTrue(duration < 5000, "批量处理性能测试失败，耗时：" + duration + "ms");
	}

	@Test
	@DisplayName("测试复杂文本场景")
	void testComplexTextScenario() {
		String input = """
				张三的个人信息：
				联系电话：13812345678 或 15987654321
				身份证号码：110101199001011234
				邮箱地址：zhangsan@company.com 或 zhang.san@gmail.com
				银行账户：
				- 工商银行：6222021234567890123
				- 建设银行：4367421234567890123
				社交账号：
				- QQ：123456789
				- 微信：wxid_abc123def456
				""";

		String result = sensitiveFilterService.apply(input);

		// 验证所有敏感信息都被正确过滤
		assertFalse(result.contains("13812345678"));
		assertFalse(result.contains("15987654321"));
		assertFalse(result.contains("110101199001011234"));
		assertFalse(result.contains("zhangsan@company.com"));
		assertFalse(result.contains("zhang.san@gmail.com"));
		assertFalse(result.contains("6222021234567890123"));
		assertFalse(result.contains("4367421234567890123"));
		assertFalse(result.contains("QQ：123456789"));

		// 验证非敏感信息保留
		assertTrue(result.contains("张三的个人信息"));
		assertTrue(result.contains("联系电话"));
		assertTrue(result.contains("身份证号码"));
		assertTrue(result.contains("邮箱地址"));
		assertTrue(result.contains("银行账户"));
		assertTrue(result.contains("工商银行"));
		assertTrue(result.contains("建设银行"));
		assertTrue(result.contains("社交账号"));
		assertTrue(result.contains("微信"));
	}

}