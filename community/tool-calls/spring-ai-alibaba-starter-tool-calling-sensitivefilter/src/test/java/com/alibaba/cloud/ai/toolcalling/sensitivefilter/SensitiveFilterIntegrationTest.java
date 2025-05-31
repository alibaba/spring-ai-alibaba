package com.alibaba.cloud.ai.toolcalling.sensitivefilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveFilter 集成测试
 */
@SpringJUnitConfig
@SpringBootTest(classes = { TestApplication.class, SensitiveFilterIntegrationTest.TestConfig.class })
@DisplayName("敏感信息过滤集成测试")
class SensitiveFilterIntegrationTest {

	@Autowired
	private SensitiveFilterService sensitiveFilterService;

	@Autowired
	private SensitiveFilterProperties sensitiveFilterProperties;

	@Test
	@DisplayName("测试Spring Boot上下文集成")
	void testSpringBootContextIntegration() {
		assertThat(sensitiveFilterService).isNotNull();
		assertThat(sensitiveFilterProperties).isNotNull();

		// 验证配置属性
		assertThat(sensitiveFilterProperties.getReplacement()).isEqualTo("[已脱敏]");
		assertThat(sensitiveFilterProperties.isFilterPhoneNumber()).isTrue();
		assertThat(sensitiveFilterProperties.getCustomPatterns()).hasSize(1);

		SensitiveFilterProperties.CustomPattern customPattern = sensitiveFilterProperties.getCustomPatterns().get(0);
		assertThat(customPattern.getName()).isEqualTo("qq");
		assertThat(customPattern.isEnabled()).isTrue();
	}

	@Test
	@DisplayName("测试完整的过滤工作流程")
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

		// 验证所有敏感信息都被正确过滤
		assertThat(result).contains("[已脱敏]"); // 手机号、邮箱、身份证、银行卡
		assertThat(result).contains("[QQ号]"); // 自定义QQ号过滤
		assertThat(result).doesNotContain("13912345678");
		assertThat(result).doesNotContain("zhangsan@example.com");
		assertThat(result).doesNotContain("110101199001011234");
		assertThat(result).doesNotContain("4123456789012345");
		assertThat(result).doesNotContain("QQ：987654321");

		// 验证非敏感信息保持原样
		assertThat(result).contains("姓名：张三");
		assertThat(result).contains("备注：这是一个包含多种敏感信息的测试文本");
	}

	@Test
	@DisplayName("测试性能 - 大量文本处理")
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

		// 验证处理时间合理（应该在几秒内完成）
		long processingTime = endTime - startTime;
		assertThat(processingTime).isLessThan(5000); // 5秒内完成

		// 验证过滤效果
		assertThat(result).doesNotContain("13912345");
		assertThat(result).doesNotContain("@example.com");
		assertThat(result).contains("[已脱敏]");
	}

	@Test
	@DisplayName("测试并发处理")
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
	@DisplayName("测试复杂文本场景")
	void testComplexTextScenarios() {
		// 测试包含特殊字符和格式的文本
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

		// 验证敏感信息被正确过滤
		assertThat(result).doesNotContain("13912345678");
		assertThat(result).doesNotContain("li.si@company.com.cn");
		assertThat(result).doesNotContain("110101199001011234");
		assertThat(result).doesNotContain("4123456789012345");
		assertThat(result).doesNotContain("5123456789012346");
		assertThat(result).doesNotContain("QQ：123456789");

		// 验证过滤后包含脱敏标识
		assertThat(result).contains("[已脱敏]");
		assertThat(result).contains("[QQ号]");

		// 验证结构化文本格式保持
		assertThat(result).contains("### 客户信息表 ###");
		assertThat(result).contains("**基本信息：**");
		assertThat(result).contains("- 客户姓名：李四");
		assertThat(result).contains("备注：此信息为测试数据");
	}

	@Test
	@DisplayName("测试Function接口在Spring环境中的使用")
	void testFunctionInterfaceInSpringContext() {
		// 将服务作为Function接口使用
		java.util.function.Function<String, String> filterFunction = sensitiveFilterService;

		String text = "客服热线：13912345678，投诉邮箱：complaint@company.com";
		String result = filterFunction.apply(text);

		assertThat(result).isEqualTo("客服热线：[已脱敏]，投诉邮箱：[已脱敏]");
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		public SensitiveFilterProperties sensitiveFilterProperties() {
			SensitiveFilterProperties properties = new SensitiveFilterProperties();
			properties.setReplacement("[已脱敏]");
			properties.setFilterPhoneNumber(true);
			properties.setFilterIdCard(true);
			properties.setFilterBankCard(true);
			properties.setFilterEmail(true);

			// 配置自定义QQ号模式
			SensitiveFilterProperties.CustomPattern qqPattern = new SensitiveFilterProperties.CustomPattern();
			qqPattern.setName("qq");
			qqPattern.setPattern("QQ[：:]?\\d{5,11}");
			qqPattern.setReplacement("[QQ号]");
			qqPattern.setEnabled(true);

			List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();
			customPatterns.add(qqPattern);
			properties.setCustomPatterns(customPatterns);

			return properties;
		}

		@Bean
		public SensitiveFilterService sensitiveFilterService(SensitiveFilterProperties properties) {
			return new SensitiveFilterService(properties);
		}

	}

}