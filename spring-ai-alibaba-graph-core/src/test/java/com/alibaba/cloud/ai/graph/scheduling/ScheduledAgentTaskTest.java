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
package com.alibaba.cloud.ai.graph.scheduling;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for ScheduledAgentTask using external TaskScheduler
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class ScheduledAgentTaskTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// 先创建 DashScopeApi 实例
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// 创建 DashScope ChatModel 实例
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@AfterEach
	void tearDown() {
		// Clean up any active scheduled executions
		try {
			DefaultScheduledAgentManager manager = DefaultScheduledAgentManager.getInstance();
			for (String taskId : manager.getAllActiveTaskIds()) {
				manager.getTask(taskId).ifPresent(ScheduledAgentTask::stop);
			}
		}
		catch (Exception e) {
			// Ignore cleanup errors
		}
	}

	@Test
	public void testScheduleReactAgentWithCron() throws Exception {
		// Counter for tracking executions
		final AtomicInteger executionCount = new AtomicInteger(0);
		final CountDownLatch executionLatch = new CountDownLatch(2); // Wait for at least
		// 1 execution

		// Create lifecycle listener
		ScheduleLifecycleListener listener = new ScheduleLifecycleListener() {
			@Override
			public void onEvent(ScheduleEvent event, Object data) {
				if (event == ScheduleEvent.EXECUTION_COMPLETED) {
					int count = executionCount.incrementAndGet();
					System.out.println("Cron execution #" + count + " " + event + " at " + System.currentTimeMillis());
					if (data instanceof OverAllState) {
						String textContent = ((OverAllState) data).value("output")
							.filter(AssistantMessage.class::isInstance)
							.map(AssistantMessage.class::cast)
							.map(AssistantMessage::getText)
							.orElse("No content available");
						System.out.println("output:" + textContent);
					}
					System.out.println((2 - executionLatch.getCount())
							+ ": -------------------------------------------------------------------------------------\n\n");
					executionLatch.countDown();
				}
				if (event == ScheduleEvent.EXECUTION_FAILED) {
					int count = executionCount.incrementAndGet();
					System.out.println("Cron execution #" + count + " " + event + " at " + System.currentTimeMillis());
					if (data instanceof Exception) {
						fail((Exception) data);
					}
					executionLatch.countDown();
				}
			}
		};

		try {
			// Create ReactAgent
			ReactAgent agent = ReactAgent.builder()
				.name("cron_scheduled_agent")
				.model(chatModel)
				.instruction("You are a cron test agent. respond briefly.")
				.outputKey("output")
				.build();

			long startTime = System.currentTimeMillis();
			System.out.println("Cron test started at: " + startTime);

			// Schedule with cron expression - every 5 seconds
			ScheduleConfig config = ScheduleConfig.builder()
				.cronExpression("*/15 * * * * ?") // Every 5 seconds
				.inputs(Map.of("messages", List.of(new UserMessage("Is '0 */1 * * * ?' a right expression?"))))
				.addListener(listener)
				.build();

			// Schedule the agent
			ScheduledAgentTask task = agent.schedule(config);

			// Verify task is registered and started
			assertThat(task).isNotNull();
			assertThat(task.isStarted()).isTrue();

			System.out.println("Cron scheduled task with ID: " + task.getTaskId());
			System.out.println("Waiting for cron execution (max 2 MINUTES)...");

			// Wait for execution with timeout (2 MINUTES should be enough)
			boolean executionCompleted = executionLatch.await(2, TimeUnit.MINUTES);

			// Stop the task
			task.stop();

			long endTime = System.currentTimeMillis();
			System.out.println("Cron test completed at: " + endTime + ", duration: " + (endTime - startTime) + "ms");

			// Verify at least one execution occurred
			assertThat(executionCompleted).as("Expected at least 1 cron execution within 10 seconds").isTrue();

			assertThat(executionCount.get()).as("Should have at least 1 execution").isGreaterThanOrEqualTo(1);

			// Verify task is stopped
			assertThat(task.isStopped()).isTrue();

			System.out.println("✅ Cron test completed successfully - executed " + executionCount.get() + " times");

		}
		catch (Exception e) {
			fail("testScheduleReactAgentWithCron failed: " + e.getMessage());
		}
	}

}
