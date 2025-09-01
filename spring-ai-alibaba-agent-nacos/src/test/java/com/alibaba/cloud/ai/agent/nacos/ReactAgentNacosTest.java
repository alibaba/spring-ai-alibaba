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

package com.alibaba.cloud.ai.agent.nacos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import com.alibaba.cloud.ai.agent.nacos.tools.NacosMcpGatewayToolCallback;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

class ReactAgentNacosTest {

	private Properties properties = null;

	@BeforeEach
	void setUp() {
		System.setProperty("logging.level.com.alibaba.", "ERROR");
		((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(NacosMcpGatewayToolCallback.class)).setLevel(Level.ERROR);
		((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("com.alibaba.nacos")).setLevel(Level.ERROR);
		((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("io.modelcontextprotocol")).setLevel(Level.ERROR);

		properties = new Properties();
		properties.put("serverAddr", "127.0.0.1:8848");
		properties.put("namespace", "71ced124-cb8e-4c2e-9ad1-f124a6f77a93");
	}

	@Test
	public void testReactAgent() throws Exception {
		properties.put("agentId", "agent0001");
		NacosOptions nacosOptions = new NacosOptions(properties);
		ReactAgent agent = ((NacosReactAgentBuilder) ReactAgent.builder(new NacosAgentBuilderFactory())).nacosOptions(
				nacosOptions).build();

		for (int i = 0; i < 100; i++) {
			System.out.println("第" + (i+1) + "轮");
			var runnableConfig = RunnableConfig.builder().threadId(UUID.randomUUID().toString()).build();
			try {
				Optional<OverAllState> result = agent.invoke(
						Map.of("messages", List.of(new UserMessage("我现在在哪个区"))), runnableConfig);
				//System.out.println(result.get().data());

				System.out.println(((ArrayList)(result.get().data().get("messages"))).stream().filter(a->a instanceof AssistantMessage&&((AssistantMessage) a).getToolCalls().isEmpty()).collect(Collectors.toList()));
			}
			catch (Throwable throwable) {
				throwable.printStackTrace();
				Thread.sleep(5000L);
			}
		}

		Thread.sleep(1000000L);

	}

}