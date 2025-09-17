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

package com.alibaba.cloud.ai.studio.admin;

import com.alibaba.cloud.ai.studio.admin.generator.GeneratorApplication;
import com.alibaba.cloud.ai.studio.admin.generator.config.GraphProjectGenerationConfiguration;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Main entry point for the Spring AI Alibaba Studio application.
 * <p>
 * A Spring Boot application that provides core server functionality for the Spring AI
 * Alibaba Studio platform, enabling AI agent management and orchestration.
 * <p>
 * Features: - Component scanning across studio package - Customizable configuration
 * properties - Graceful shutdown handling - Middleware integration (MySQL, Redis,
 * RocketMQ) - Support for agent and workflow-based AI applications
 *
 * @since 1.0.0.3
 */
@SpringBootApplication
@ComponentScan(basePackages = { "com.alibaba.cloud.ai.studio" },
		excludeFilters = {
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
						classes = GraphProjectGenerationConfiguration.class),
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GeneratorApplication.class),
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
						classes = GeneratorApplication.MockLoginController.class) })
@EnableConfigurationProperties(StudioProperties.class)
public class SaaStudioApplication {

	/**
	 * Bootstraps the Spring Boot application. Initializes the application context and
	 * registers shutdown hook for graceful termination.
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(SaaStudioApplication.class, args).registerShutdownHook();
	}

}
