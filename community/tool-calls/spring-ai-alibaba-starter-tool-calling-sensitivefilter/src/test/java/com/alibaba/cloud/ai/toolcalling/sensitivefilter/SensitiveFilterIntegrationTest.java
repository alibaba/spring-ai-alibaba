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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveFilterService integration tests
 *
 * @author Makoto
 */
@SpringJUnitConfig
@SpringBootTest(classes = { TestApplication.class })
@DisplayName("Sensitive filter integration tests")
@ActiveProfiles("test")
class SensitiveFilterIntegrationTest {

	@Autowired
	private SensitiveFilterService sensitiveFilterService;

	@Autowired
	private SensitiveFilterProperties sensitiveFilterProperties;

	@Test
	@DisplayName("Test Spring Boot context integration")
	void testSpringBootContextIntegration() {
		assertThat(sensitiveFilterService).isNotNull();
		assertThat(sensitiveFilterProperties).isNotNull();

		// Verify configuration properties
		assertThat(sensitiveFilterProperties.getReplacement()).isEqualTo("[已脱敏]");
		assertThat(sensitiveFilterProperties.isFilterPhoneNumber()).isTrue();
		assertThat(sensitiveFilterProperties.getCustomPatterns()).hasSize(1);

		SensitiveFilterProperties.CustomPattern customPattern = sensitiveFilterProperties.getCustomPatterns().get(0);
		assertThat(customPattern.getName()).isEqualTo("qq");
		assertThat(customPattern.isEnabled()).isTrue();
	}

	@Test
	@DisplayName("Test complete filtering workflow")
	void testCompleteFilteringWorkflow() {
		String complexText = """
				用户信息：
				姓名：张三
				手机：13912345678
				邮箱：zhangsan@example.com
				身份证：110101199001011234
				银行卡：4123456789012345
				QQ：987654321
				备注：这是一个包含多种敏感信息的测试文本
				""";

		String result = sensitiveFilterService.apply(complexText);

		// Verify all sensitive information is correctly filtered
		assertThat(result).contains("[已脱敏]"); // phone number, email, ID card, bank card
		assertThat(result).contains("[QQ号]"); // custom QQ number filtering
		assertThat(result).doesNotContain("13912345678");
		assertThat(result).doesNotContain("zhangsan@example.com");
		assertThat(result).doesNotContain("110101199001011234");
		assertThat(result).doesNotContain("4123456789012345");
		assertThat(result).doesNotContain("QQ：987654321");

		// Verify non-sensitive information remains unchanged
		assertThat(result).contains("姓名：张三");
		assertThat(result).contains("备注：这是一个包含多种敏感信息的测试文本");
	}

	@Test
	@DisplayName("Test performance - large text processing")
	void testPerformanceWithLargeText() {
		StringBuilder largeTextBuilder = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			largeTextBuilder.append("用户")
				.append(i)
				.append("：")
				.append("手机13912345")
				.append(String.format("%03d", i))
				.append("，")
				.append("邮箱user")
				.append(i)
				.append("@example.com，")
				.append("身份证1101011990010112")
				.append(String.format("%02d", i % 100))
				.append("。");
		}
		String largeText = largeTextBuilder.toString();

		long startTime = System.currentTimeMillis();
		String result = sensitiveFilterService.apply(largeText);
		long endTime = System.currentTimeMillis();

		// Verify processing time is reasonable (should be completed in a few seconds)
		long processingTime = endTime - startTime;
		assertThat(processingTime).isLessThan(5000); // completed in 5 seconds

		// Verify filtering effect
		assertThat(result).doesNotContain("13912345");
		assertThat(result).doesNotContain("@example.com");
		assertThat(result).contains("[已脱敏]");
	}

	@Test
	@DisplayName("Test concurrent processing")
	void testConcurrentProcessing() throws InterruptedException {
		int threadCount = 10;
		int operationsPerThread = 500;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					for (int j = 0; j < operationsPerThread; j++) {
						String text = String.format("线程%d操作%d：手机13912345%03d，邮箱user%d@test.com", threadId, j,
								(threadId * 1000 + j) % 1000, threadId * 1000 + j);
						String result = sensitiveFilterService.apply(text);

						if (result.contains("[已脱敏]") && !result.contains("13912345") && !result.contains("@test.com")) {
							successCount.incrementAndGet();
						}
						else {
							errorCount.incrementAndGet();
						}
					}
				}
				catch (Exception e) {
					errorCount.incrementAndGet();
				}
				finally {
					latch.countDown();
				}
			});
		}

		boolean completed = latch.await(30, TimeUnit.SECONDS);
		executor.shutdown();

		assertThat(completed).isTrue();
		assertThat(successCount.get()).isEqualTo(threadCount * operationsPerThread);
		assertThat(errorCount.get()).isZero();
	}

	@Test
	@DisplayName("Test complex text scenarios")
	void testComplexTextScenarios() {
		// Test text containing special characters and formats
		String complexText = """
				### 客户信息表 ###

				**基本信息：**
				- 客户姓名：李四
				- 联系电话：13912345678 (手机)
				- 电子邮箱：<li.si@company.com.cn>
				- 身份证号：110101199001011234

				**金融信息：**
				- 工商银行卡：4123456789012345
				- 建设银行卡：5123456789012346

				**社交账号：**
				- QQ：123456789
				- 微信号：lisi_2024

				---
				备注：此信息为测试数据，请勿用于实际业务。
				""";

		String result = sensitiveFilterService.apply(complexText);

		// Verify sensitive information is correctly filtered
		assertThat(result).doesNotContain("13912345678");
		assertThat(result).doesNotContain("li.si@company.com.cn");
		assertThat(result).doesNotContain("110101199001011234");
		assertThat(result).doesNotContain("4123456789012345");
		assertThat(result).doesNotContain("5123456789012346");
		assertThat(result).doesNotContain("QQ：123456789");

		// Verify filtered text contains de-identification identifier
		assertThat(result).contains("[已脱敏]");
		assertThat(result).contains("[QQ号]");

		// Verify structured text format remains
		assertThat(result).contains("### 客户信息表 ###");
		assertThat(result).contains("**基本信息：**");
		assertThat(result).contains("- 客户姓名：李四");
		assertThat(result).contains("备注：此信息为测试数据");
	}

	@Test
	@DisplayName("Test Function interface usage in Spring environment")
	void testFunctionInterfaceInSpringContext() {
		// Use service as Function interface
		java.util.function.Function<String, String> filterFunction = sensitiveFilterService;

		String text = "客服热线：13912345678，投诉邮箱：complaint@company.com";
		String result = filterFunction.apply(text);

		assertThat(result).isEqualTo("客服热线：[已脱敏]，投诉邮箱：[已脱敏]");
	}

}
