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

package com.alibaba.cloud.ai.example.manus;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.agent.ManusAgent;
import com.alibaba.cloud.ai.example.manus.flow.PlanningFlow;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OpenManusSpringBootApplication {

	public static void main(String[] args) {
		initDriver();

		SpringApplication.run(OpenManusSpringBootApplication.class, args);
	}

	@Bean
	public PlanningFlow planningFlow(LlmService llmService, ToolCallingManager toolCallingManager) {
		ManusAgent manusAgent = new ManusAgent(llmService, toolCallingManager);

		Map<String, BaseAgent> agentMap = new HashMap<String, BaseAgent>() {
			{
				put("manus", manusAgent);
			}
		};
		Map<String, Object> data = new HashMap<>();
		return new PlanningFlow(agentMap, data);
	}

	public static void initDriver() {
		URL resource = OpenManusSpringBootApplication.class.getClassLoader().getResource("data/chromedriver");
		if (resource == null) {
			throw new IllegalStateException("Chromedriver not found");
		}
		String chromedriverPath = Paths.get(resource.getPath()).toFile().getAbsolutePath();
		System.setProperty("webdriver.chrome.driver", chromedriverPath);

	}

}
