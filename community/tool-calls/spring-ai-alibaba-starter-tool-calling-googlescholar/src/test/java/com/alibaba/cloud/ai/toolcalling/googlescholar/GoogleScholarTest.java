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
package com.alibaba.cloud.ai.toolcalling.googlescholar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Makoto
 */
public class GoogleScholarTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GoogleScholarAutoConfiguration.class));

	@Test
	public void testAutoConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(GoogleScholarService.class);
			assertThat(context).hasSingleBean(GoogleScholarProperties.class);
		});
	}

	@Test
	public void testSearchFunctionality() {
		this.contextRunner.run((context) -> {
			GoogleScholarService service = context.getBean(GoogleScholarService.class);
			assertThat(service).isNotNull();

			// Create a test request
			GoogleScholarService.Request request = GoogleScholarService.Request.simpleQuery("machine learning");
			assertThat(request.getQuery()).isEqualTo("machine learning");

			// Note: Actual network testing should be done separately with proper test
			// environment
			// This is just to verify the service can be instantiated and basic
			// functionality works
		});
	}

	@Test
	public void testCustomConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.ai.alibaba.toolcalling.googlescholar.numResults=5",
					"spring.ai.alibaba.toolcalling.googlescholar.language=zh",
					"spring.ai.alibaba.toolcalling.googlescholar.includeCitations=false")
			.run((context) -> {
				GoogleScholarProperties properties = context.getBean(GoogleScholarProperties.class);
				assertThat(properties.getNumResults()).isEqualTo(5);
				assertThat(properties.getLanguage()).isEqualTo("zh");
				assertThat(properties.isIncludeCitations()).isFalse();
			});
	}

}
