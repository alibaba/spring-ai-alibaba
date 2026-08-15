/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.registry.nacos.properties;

import com.alibaba.nacos.api.PropertyKeyConst;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NacosA2aPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(TestConfiguration.class);

	@Test
	void shouldBindServerAddress() {
		contextRunner.withPropertyValues("spring.ai.alibaba.a2a.nacos.server-addr=192.168.1.10:8848")
			.run(context -> {
				NacosA2aProperties properties = context.getBean(NacosA2aProperties.class);

				assertThat(properties.getServerAddr()).isEqualTo("192.168.1.10:8848");
				assertThat(properties.getNacosProperties().getProperty(PropertyKeyConst.SERVER_ADDR))
					.isEqualTo("192.168.1.10:8848");
			});
	}

	@EnableConfigurationProperties(NacosA2aProperties.class)
	static class TestConfiguration {

	}

}
