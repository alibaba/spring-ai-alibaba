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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 敏感词过滤自动配置测试类
 *
 * @author Makoto
 */
@DisplayName("敏感词过滤自动配置测试")
class SensitiveFilterAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SensitiveFilterAutoConfiguration.class));

	@Test
	@DisplayName("当enabled=true时，应该创建SensitiveFilterService Bean")
	void shouldCreateSensitiveFilterServiceWhenEnabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.toolcalling.sensitivefilter.enabled=true")
			.run(context -> {
				assertThat(context).hasSingleBean(SensitiveFilterService.class);
				assertThat(context).hasSingleBean(SensitiveFilterProperties.class);
			});
	}

	@Test
	@DisplayName("当enabled=false时，不应该创建SensitiveFilterService Bean")
	void shouldNotCreateSensitiveFilterServiceWhenDisabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.toolcalling.sensitivefilter.enabled=false")
			.run(context -> {
				assertThat(context).doesNotHaveBean(SensitiveFilterService.class);
			});
	}

	@Test
	@DisplayName("当没有设置enabled属性时，不应该创建SensitiveFilterService Bean")
	void shouldNotCreateSensitiveFilterServiceWhenPropertyNotSet() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(SensitiveFilterService.class);
		});
	}

	@Test
	@DisplayName("当已存在SensitiveFilterService Bean时，不应该创建新的Bean")
	void shouldNotCreateSensitiveFilterServiceWhenBeanAlreadyExists() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.toolcalling.sensitivefilter.enabled=true")
			.withBean(SensitiveFilterService.class, () -> {
				SensitiveFilterProperties properties = new SensitiveFilterProperties();
				return new SensitiveFilterService(properties);
			})
			.run(context -> {
				assertThat(context).hasSingleBean(SensitiveFilterService.class);
			});
	}

	@Test
	@DisplayName("测试配置属性的正确绑定")
	void shouldBindPropertiesCorrectly() {
		this.contextRunner
			.withPropertyValues("spring.ai.alibaba.toolcalling.sensitivefilter.enabled=true",
					"spring.ai.alibaba.toolcalling.sensitivefilter.replacement=[MASKED]",
					"spring.ai.alibaba.toolcalling.sensitivefilter.filter-phone-number=false",
					"spring.ai.alibaba.toolcalling.sensitivefilter.filter-id-card=false",
					"spring.ai.alibaba.toolcalling.sensitivefilter.filter-bank-card=true",
					"spring.ai.alibaba.toolcalling.sensitivefilter.filter-email=true")
			.run(context -> {
				assertThat(context).hasSingleBean(SensitiveFilterService.class);

				SensitiveFilterProperties properties = context.getBean(SensitiveFilterProperties.class);
				assertThat(properties.getReplacement()).isEqualTo("[MASKED]");
				assertThat(properties.isFilterPhoneNumber()).isFalse();
				assertThat(properties.isFilterIdCard()).isFalse();
				assertThat(properties.isFilterBankCard()).isTrue();
				assertThat(properties.isFilterEmail()).isTrue();
			});
	}

	@Test
	@DisplayName("测试自定义模式配置的绑定")
	void shouldBindCustomPatternsCorrectly() {
		this.contextRunner
			.withPropertyValues("spring.ai.alibaba.toolcalling.sensitivefilter.enabled=true",
					"spring.ai.alibaba.toolcalling.sensitivefilter.custom-patterns[0].name=qq",
					"spring.ai.alibaba.toolcalling.sensitivefilter.custom-patterns[0].pattern=QQ[：:]?\\d{5,11}",
					"spring.ai.alibaba.toolcalling.sensitivefilter.custom-patterns[0].replacement=[QQ号]",
					"spring.ai.alibaba.toolcalling.sensitivefilter.custom-patterns[0].enabled=true")
			.run(context -> {
				assertThat(context).hasSingleBean(SensitiveFilterService.class);

				SensitiveFilterProperties properties = context.getBean(SensitiveFilterProperties.class);
				assertThat(properties.getCustomPatterns()).hasSize(1);

				SensitiveFilterProperties.CustomPattern pattern = properties.getCustomPatterns().get(0);
				assertThat(pattern.getName()).isEqualTo("qq");
				assertThat(pattern.getPattern()).isEqualTo("QQ[：:]?\\d{5,11}");
				assertThat(pattern.getReplacement()).isEqualTo("[QQ号]");
				assertThat(pattern.isEnabled()).isTrue();
			});
	}

	@Test
	@DisplayName("测试服务功能是否正常工作")
	void shouldWorkCorrectlyWithConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.ai.alibaba.toolcalling.sensitivefilter.enabled=true",
					"spring.ai.alibaba.toolcalling.sensitivefilter.replacement=[HIDDEN]")
			.run(context -> {
				assertThat(context).hasSingleBean(SensitiveFilterService.class);

				SensitiveFilterService service = context.getBean(SensitiveFilterService.class);
				String input = "我的手机号是13812345678";
				String result = service.apply(input);
				assertThat(result).isEqualTo("我的手机号是[HIDDEN]");
			});
	}

}