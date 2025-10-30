/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 * @author yHong
 */

package com.alibaba.cloud.ai.studio.workflow.assistant;

import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * The main entry point for the Workflow Assistant application.
 * <p>
 * This class uses {@link SpringBootApplication} to enable auto-configuration,
 * component scanning, and other Spring Boot features. It serves as the
 * standalone application runner for this module.
 * </p>
 * <p>
 * The {@code scanBasePackages} attribute is crucial for a multi-module setup.
 * It instructs Spring to scan not only the current module's packages
 * ({@code com.alibaba.cloud.ai.studio.workflow.assistant}) but also packages
 * from other modules like {@code core}
 * ({@code com.alibaba.cloud.ai.studio.core}). This ensures that components,
 * services, and controllers from dependent modules are discovered and registered
 * in the application context.
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.alibaba.cloud.ai.studio")
@EnableConfigurationProperties(StudioProperties.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
