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
package com.alibaba.cloud.ai.toolcalling.serpapi;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.ThreadLocalRandom;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.DEFAULT_USER_AGENTS;

/**
 * @author 北极星
 * @author sixiyida
 */
@ConfigurationProperties(prefix = SerpApiConstants.CONFIG_PREFIX)
public class SerpApiProperties extends CommonToolCallProperties {

	public static final String USER_AGENT_VALUE = DEFAULT_USER_AGENTS[ThreadLocalRandom.current()
		.nextInt(DEFAULT_USER_AGENTS.length)];

	public SerpApiProperties() {
		super("https://serpapi.com/search");
		this.setPropertiesFromEnv(SerpApiConstants.API_KEY_ENV, null, null, null);
	}

	private String engine;

	public String getEngine() {
		return engine;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

}
