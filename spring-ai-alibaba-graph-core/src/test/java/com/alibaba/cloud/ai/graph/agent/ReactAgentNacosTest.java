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
package com.alibaba.cloud.ai.graph.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.nacos.NacosOptions;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.UserMessage;

class ReactAgentNacosTest {

	private NacosConfigService nacosConfigService = nacosConfigService();

	private NacosConfigService nacosConfigService() {
		Properties properties = new Properties();
		properties.put("serverAddr", "mse-401cbb30-p.nacos-ans.mse.aliyuncs.com:8848");
		properties.put("namespace", "b9ee889a-991e-4f5d-b0c1-bac4d08a1cd8");
		try {
			return new NacosConfigService(properties);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeEach
	void setUp() {
		// 先创建 DashScopeApi 实例
	}

	@Test
	public void testReactAgent() throws Exception {
		NacosOptions nacosOptions = new NacosOptions();
		nacosOptions.setNacosConfigService(nacosConfigService);
		//nacosOptions.setAgentId("agent0001");
		nacosOptions.setPromptKey("bookprompt2");
		ReactAgent agent = ReactAgent.builder().name("agent0001").nacosProxy(nacosOptions).build();


		//Thread.sleep(15000L);
		for (int i=0;i<20;i++){
			Optional<OverAllState> result = agent.invoke(Map.of("messages", List.of(new UserMessage("介绍下鲁迅。必须给我回答"))));
			System.out.println(result.get().data());
		}


		Thread.sleep(1000000L);
		//agent.invoke(Map.of("messages", List.of(new UserMessage("介绍下沈丛文。"))));
		//System.out.println(result.get());
	}

}