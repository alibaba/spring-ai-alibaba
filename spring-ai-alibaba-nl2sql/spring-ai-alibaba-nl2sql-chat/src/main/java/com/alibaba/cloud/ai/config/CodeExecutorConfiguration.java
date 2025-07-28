/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.service.code.executor.CodePoolExecutorService;
import com.alibaba.cloud.ai.service.code.executor.CodePoolExecutorServiceFactory;
import com.alibaba.cloud.ai.service.code.memory.SqlResultMemoryService;
import com.alibaba.cloud.ai.service.code.memory.SqlResultMemoryServiceFactory;
import com.alibaba.cloud.ai.tool.PythonExecutorTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author vlsmb
 * @since 2025/7/14
 */
@Configuration
@EnableConfigurationProperties(CodeExecutorProperties.class)
@ConditionalOnProperty(prefix = CodeExecutorProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class CodeExecutorConfiguration {

	@Bean
	public CodePoolExecutorService containerPoolExecutor(CodeExecutorProperties properties) {
		return CodePoolExecutorServiceFactory.newInstance(properties);
	}

	@Bean
	public SqlResultMemoryService sqlResultMemoryService(CodeExecutorProperties properties) {
		return SqlResultMemoryServiceFactory.newInstance(properties);
	}

	@Bean
	public PythonExecutorTool pythonExecutorTool(CodePoolExecutorService executor,
			SqlResultMemoryService sqlResultMemoryService) {
		return new PythonExecutorTool(executor, sqlResultMemoryService);
	}

}
