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
package com.alibaba.cloud.ai.dashscope.agent;

import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Title Dashscope Agent test cases.<br>
 * Description Dashscope Agent test cases.<br>
 *
 * @author yuluo
 * @author linkesheng.lks
 * @since 1.0.0-M2
 */

class DashScopeAgentTests {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeAgentTests.class);

	private static final String TEST_API_KEY = System.getenv("DASHSCOPE_API_KEY");

	private static final String TEST_APP_ID = System.getenv("APP_ID");

	private static final String TEST_FILE_ID = System.getenv("FILE_ID");

	private final DashScopeAgentApi dashscopeAgentApi = new DashScopeAgentApi(TEST_API_KEY);

	@Test
	void callWithRagOptionsFileIds() {
		DashScopeAgent dashScopeAgent = new DashScopeAgent(dashscopeAgentApi);

		Flux<ChatResponse> response = dashScopeAgent.stream(new Prompt("梁随板失败怎么办？",
				DashScopeAgentOptions.builder()
					.withAppId(TEST_APP_ID)
					.withIncrementalOutput(true)
					.withRagOptions(DashScopeAgentRagOptions.builder().withFileIds(List.of(TEST_FILE_ID)).build())
					.build()));

		printResponse(response);
	}

	@Test
	void callWithImageList() {
		DashScopeAgent dashScopeAgent = new DashScopeAgent(dashscopeAgentApi);

		Flux<ChatResponse> response = dashScopeAgent.stream(new Prompt("图中描绘的是什么景象?", DashScopeAgentOptions.builder()
			.withAppId(TEST_APP_ID)
			.withIncrementalOutput(true)
			.withImages(List
				.of("https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241022/emyrja/dog_and_girl.jpeg"))
			.build()));

		printResponse(response);
	}

	@Test
	void callWithSystemMessage() {
		DashScopeAgent dashScopeAgent = new DashScopeAgent(dashscopeAgentApi);

		Flux<ChatResponse> response = dashScopeAgent
			.stream(new Prompt(List.of(new SystemMessage("你是一个新闻记者，请记住你的角色。"), new UserMessage("你是谁?")),
					DashScopeAgentOptions.builder().withAppId(TEST_APP_ID).withIncrementalOutput(true).build()));

		printResponse(response);
	}

	@Test
	void callWithDeepSeeek() {
		DashScopeAgent dashScopeAgent = new DashScopeAgent(dashscopeAgentApi,
				DashScopeAgentOptions.builder()
					.withAppId(TEST_APP_ID)
					.withIncrementalOutput(true)
					.withHasThoughts(true)
					.build());

		Flux<ChatResponse> response = dashScopeAgent.stream(new Prompt("x的平方等于4，x等于多少？"));

		printResponse(response);
	}

	static void printResponse(Flux<ChatResponse> response) {
		CountDownLatch cdl = new CountDownLatch(1);
		response.subscribe(data -> {
			System.out.printf("%s%n", data.getResult().getOutput());
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