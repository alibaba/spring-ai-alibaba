/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tools.task;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("TaskTool Tests")
class TaskToolTest {

	private TaskRepository taskRepository;

	private ReactAgent mockSubAgent;

	@BeforeEach
	void setUp() {
		this.taskRepository = new DefaultTaskRepository();
		this.mockSubAgent = mock(ReactAgent.class);
	}

	@Nested
	@DisplayName("Builder Validation Tests")
	class BuilderValidationTests {

		@Test
		@DisplayName("Should fail when subAgents is empty")
		void shouldFailWhenSubAgentsIsEmpty() {
			assertThatThrownBy(() -> TaskTool.builder().taskRepository(taskRepository).build())
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("At least one sub-agent must be configured");
		}

		@Test
		@DisplayName("Should fail when taskRepository is null")
		void shouldFailWhenTaskRepositoryIsNull() {
			assertThatThrownBy(() -> TaskTool.builder()
					.subAgent("Explore", mockSubAgent)
					.taskRepository(null)
					.build()).isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("taskRepository must not be null");
		}

		@Test
		@DisplayName("Should build successfully with valid config")
		void shouldBuildSuccessfullyWithValidConfig() {
			TaskTool.Builder builder = TaskTool.builder()
					.subAgent("Explore", mockSubAgent)
					.taskRepository(taskRepository);
			assertThat(builder).isNotNull();
			assertThat(builder.build()).isNotNull();
		}
	}

	@Nested
	@DisplayName("Synchronous Execution Tests")
	class SynchronousExecutionTests {

		@Test
		@DisplayName("Should execute sub-agent synchronously and return result")
		void shouldExecuteSubAgentSynchronouslyAndReturnResult() throws Exception {
			when(mockSubAgent.call(anyString())).thenReturn(new AssistantMessage("Exploration result"));

			TaskTool taskTool = new TaskTool(Map.of("Explore", mockSubAgent), taskRepository);
			TaskTool.Request request = new TaskTool.Request("Explore codebase", "Find all REST controllers",
					"Explore", false);

			String result = taskTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).isEqualTo("Exploration result");
		}

		@Test
		@DisplayName("Should return error for unknown subagent type")
		void shouldReturnErrorForUnknownSubagentType() {
			TaskTool taskTool = new TaskTool(Map.of("Explore", mockSubAgent), taskRepository);
			TaskTool.Request request = new TaskTool.Request("Explore", "Find files", "unknown-type", false);

			String result = taskTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).startsWith("Error: Unknown subagent type");
			assertThat(result).contains("unknown-type");
		}

		@Test
		@DisplayName("Should return error when prompt is empty")
		void shouldReturnErrorWhenPromptIsEmpty() {
			TaskTool taskTool = new TaskTool(Map.of("Explore", mockSubAgent), taskRepository);
			TaskTool.Request request = new TaskTool.Request("Explore", "", "Explore", false);

			String result = taskTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).startsWith("Error: prompt is required");
		}

		@Test
		@DisplayName("Should return error when subagent_type is empty")
		void shouldReturnErrorWhenSubagentTypeIsEmpty() {
			TaskTool taskTool = new TaskTool(Map.of("Explore", mockSubAgent), taskRepository);
			TaskTool.Request request = new TaskTool.Request("Explore", "Find files", "", false);

			String result = taskTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).startsWith("Error: subagent_type is required");
		}

		@Test
		@DisplayName("Should return error message when sub-agent throws")
		void shouldReturnErrorMessageWhenSubAgentThrows() throws Exception {
			when(mockSubAgent.call(anyString())).thenThrow(new RuntimeException("Sub-agent failed"));

			TaskTool taskTool = new TaskTool(Map.of("Explore", mockSubAgent), taskRepository);
			TaskTool.Request request = new TaskTool.Request("Explore", "Find files", "Explore", false);

			String result = taskTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).contains("Error executing sub-agent");
			assertThat(result).contains("Sub-agent failed");
		}
	}

	@Nested
	@DisplayName("Background Execution Tests")
	class BackgroundExecutionTests {

		@Test
		@DisplayName("Should return task_id when run_in_background is true")
		void shouldReturnTaskIdWhenRunInBackgroundIsTrue() {
			TaskTool taskTool = new TaskTool(Map.of("Explore", mockSubAgent), taskRepository);
			TaskTool.Request request = new TaskTool.Request("Explore", "Find files", "Explore", true);

			String result = taskTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).contains("task_id:");
			assertThat(result).contains("Background task started");
			assertThat(result).contains("TaskOutput");
		}

		@Test
		@DisplayName("Should store task in repository for background execution")
		void shouldStoreTaskInRepositoryForBackgroundExecution() throws Exception {
			when(mockSubAgent.call(anyString())).thenReturn(new AssistantMessage("Done"));

			TaskTool taskTool = new TaskTool(Map.of("Explore", mockSubAgent), taskRepository);
			TaskTool.Request request = new TaskTool.Request("Explore", "Find files", "Explore", true);

			String result = taskTool.apply(request, new ToolContext(Map.of()));

			String taskId = result.replaceAll("(?s).*task_id: (task_[^\\s]+).*", "$1").trim();
			assertThat(taskId).startsWith("task_");

			BackgroundTask bgTask = taskRepository.getTask(taskId);
			assertThat(bgTask).isNotNull();

			boolean completed = bgTask.waitForCompletion(5000);
			assertThat(completed).isTrue();
			assertThat(bgTask.getResult()).isEqualTo("Done");
		}
	}

}
