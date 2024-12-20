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
package com.alibaba.cloud.ai.agent;

import com.alibaba.cloud.ai.autoconfig.dashscope.DashScopeAutoConfiguration;
import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgentOptions;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput.DashScopeAgentResponseOutputDocReference;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi.DashScopeAgentResponse.DashScopeAgentResponseOutput.DashScopeAgentResponseOutputThoughts;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Title Dashscope Agent test cases.<br>
 * Description Dashscope Agent test cases.<br>
 *
 * @author linkesheng.lks
 * @since 1.0.0-M2
 */

@TestPropertySource("classpath:application.yml")
@SpringBootTest(classes = DashScopeAutoConfiguration.class)
public class DashScopeAgentIT {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeAgentIT.class);

	@Autowired
	private DashScopeAgentApi dashscopeAgentApi;

	@Value("${spring.ai.dashscope.agent.app-id}")
	private String appId;

	@Test
	void callWithDocReferencesAndThoughts() {
		DashScopeAgent dashScopeAgent = new DashScopeAgent(dashscopeAgentApi);

		ChatResponse response = dashScopeAgent
			.call(new Prompt("如何使用SDK快速调用阿里云百炼的应用?", DashScopeAgentOptions.builder().withAppId(appId).build()));
		if (response == null) {
			logger.error("chat response is null");
			return;
		}

		AssistantMessage app_output = response.getResult().getOutput();
		String content = app_output.getContent();

		DashScopeAgentResponseOutput output = (DashScopeAgentResponseOutput) app_output.getMetadata().get("output");
		List<DashScopeAgentResponseOutputDocReference> docReferences = output.docReferences();
		List<DashScopeAgentResponseOutputThoughts> thoughts = output.thoughts();

		Assertions.assertNotNull(content);
		logger.info("content:\n{}\n\n", content);

		if (docReferences != null && !docReferences.isEmpty()) {
			for (DashScopeAgentResponseOutputDocReference docReference : docReferences) {
				logger.info("{}\n\n", docReference);
			}
		}

		if (thoughts != null && !thoughts.isEmpty()) {
			for (DashScopeAgentResponseOutputThoughts thought : thoughts) {
				logger.info("{}\n\n", thought);
			}
		}
	}

	@Test
	void streamWithMultiTurn() {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode bizParams = objectMapper.createObjectNode();
		bizParams.put("name", "Alice");
		bizParams.put("age", 30);

		DashScopeAgent dashScopeAgent = new DashScopeAgent(dashscopeAgentApi,
				DashScopeAgentOptions.builder()
					.withSessionId("current_session_id")
					.withIncrementalOutput(true)
					.withHasThoughts(true)
					.withBizParams(bizParams)
					.build());

		Flux<ChatResponse> first_response = dashScopeAgent
			.stream(new Prompt("奥运会乒乓球总共有几个项目?", DashScopeAgentOptions.builder().withAppId(appId).build()));

		PrintStream(first_response);

		Flux<ChatResponse> second_response = dashScopeAgent
			.stream(new Prompt("那羽毛球呢?", DashScopeAgentOptions.builder().withAppId(appId).build()));

		PrintStream(second_response);
	}

	void PrintStream(Flux<ChatResponse> response) {
		CountDownLatch cdl = new CountDownLatch(1);
		response.subscribe(data -> {
			System.out.printf("%s", data.getResult().getOutput().getContent());
		}, err -> {
			logger.error("err: {}", err.getMessage(), err);
		}, () -> {
			System.out.println("\n");
			logger.info("done");
			cdl.countDown();
		});

		try {
			cdl.await();
		}
		catch (InterruptedException e) {
			throw new DashScopeException(e.getMessage());
		}
	}

}
