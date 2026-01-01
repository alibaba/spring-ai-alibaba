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

package com.alibaba.cloud.ai.examples.documentation.framework.advanced.a2a;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring AI Alibaba Documentation Examples Application
 *
 * 本应用演示了 Spring AI Alibaba 的各种功能，包括�?
 * - Agent Framework 示例
 * - A2A (Agent-to-Agent) 分布式智能体示例
 * - Graph 工作流示�?
 */
@SpringBootApplication
public class DocumentationApplication {

	private static final Logger logger = LoggerFactory.getLogger(DocumentationApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DocumentationApplication.class, args);
	}

	// @Bean
	public CommandLineRunner demoRunner(A2AExample a2aExample) {
		return args -> {
			logger.info("=================================================");
			logger.info("Spring AI Alibaba Documentation Examples Started");
			logger.info("=================================================");
			// a2aExample.runDemo();
			logger.info("Application is ready. Hit /api/a2a/demo to run the A2A demo.");
		};
	}
}
