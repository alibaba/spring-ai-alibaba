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
package com.alibaba.cloud.ai.oltp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author 肖云涛
 * @since 2024/12/5
 */
@Configuration
@ConfigurationProperties(StudioObservabilityProperties.CONFIG_PREFIX)
public class StudioObservabilityProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.studio";

	private boolean enabled;

	private String outputFile;

	public boolean isEnabled() {
		return this.enabled;
	}

	public String getOutputFile() {
		return this.outputFile;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public StudioObservabilityProperties() {
		this.enabled = true;
		this.outputFile = "spring-ai-alibaba-studio/spans.json";
	}

}
