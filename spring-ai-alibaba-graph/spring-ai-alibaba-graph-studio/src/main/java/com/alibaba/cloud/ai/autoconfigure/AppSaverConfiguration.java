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

package com.alibaba.cloud.ai.autoconfigure;

import com.alibaba.cloud.ai.saver.AppMemorySaver;
import com.alibaba.cloud.ai.saver.AppSaver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppSaverConfiguration {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.studio";

	@Bean
	@ConditionalOnProperty(prefix = CONFIG_PREFIX, name = "saver", havingValue = "memory", matchIfMissing = true)
	public AppSaver appMemorySaver() {
		return new AppMemorySaver();
	}

}
