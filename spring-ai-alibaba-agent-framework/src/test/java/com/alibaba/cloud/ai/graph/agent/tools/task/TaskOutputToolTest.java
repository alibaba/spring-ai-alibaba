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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TaskOutputTool Tests")
class TaskOutputToolTest {

	private TaskRepository taskRepository;

	private TaskOutputTool taskOutputTool;

	@BeforeEach
	void setUp() {
		this.taskRepository = new DefaultTaskRepository();
		this.taskOutputTool = new TaskOutputTool(taskRepository);
	}

	@Nested
	@DisplayName("Builder Validation Tests")
	class BuilderValidationTests {

		@Test
		@DisplayName("Should fail when taskRepository is null")
		void shouldFailWhenTaskRepositoryIsNull() {
			assertThatThrownBy(() -> TaskOutputTool.builder().taskRepository(null).build())
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("taskRepository must not be null");
		}

		@Test
		@DisplayName("Should build successfully with valid config")
		void shouldBuildSuccessfullyWithValidConfig() {
			TaskOutputTool.Builder builder = TaskOutputTool.builder()
					.taskRepository(taskRepository);
			assertThat(builder).isNotNull();
			assertThat(builder.build()).isNotNull();
		}
	}

	@Nested
	@DisplayName("Task Retrieval Tests")
	class TaskRetrievalTests {

		@Test
		@DisplayName("Should return error for unknown task_id")
		void shouldReturnErrorForUnknownTaskId() {
			TaskOutputTool.Request request = new TaskOutputTool.Request("task_unknown", null, null);

			String result = taskOutputTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).startsWith("Error: No background task found");
			assertThat(result).contains("task_unknown");
		}

		@Test
		@DisplayName("Should return completed task result")
		void shouldReturnCompletedTaskResult() {
			String taskId = "task_123";
			String expectedResult = "Task completed successfully";
			taskRepository.putTask(taskId, () -> expectedResult);

			BackgroundTask bgTask = taskRepository.getTask(taskId);
			// Wait for completion (sync execution)
			try {
				bgTask.waitForCompletion(5000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			TaskOutputTool.Request request = new TaskOutputTool.Request(taskId, true, 5000L);
			String result = taskOutputTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).contains("Task ID: task_123");
			assertThat(result).contains("Status: Completed");
			assertThat(result).contains("Result:");
			assertThat(result).contains(expectedResult);
		}

		@Test
		@DisplayName("Should return error when task failed")
		void shouldReturnErrorWhenTaskFailed() {
			String taskId = "task_fail";
			taskRepository.putTask(taskId, () -> {
				throw new RuntimeException("Task execution failed");
			});

			BackgroundTask bgTask = taskRepository.getTask(taskId);
			try {
				bgTask.waitForCompletion(5000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			TaskOutputTool.Request request = new TaskOutputTool.Request(taskId, true, 5000L);
			String result = taskOutputTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).contains("Status: Failed");
			assertThat(result).contains("Error:");
			assertThat(result).contains("Task execution failed");
		}

		@Test
		@DisplayName("Should indicate task still running when not completed")
		void shouldIndicateTaskStillRunningWhenNotCompleted() {
			String taskId = "task_slow";
			CompletableFuture<String> future = new CompletableFuture<>();
			taskRepository.putTask(taskId, () -> {
				try {
					return future.get(10, java.util.concurrent.TimeUnit.SECONDS);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			TaskOutputTool.Request request = new TaskOutputTool.Request(taskId, false, null);
			String result = taskOutputTool.apply(request, new ToolContext(Map.of()));

			assertThat(result).contains("Task ID: task_slow");
			assertThat(result).contains("Status: Running");
			assertThat(result).contains("Task still running");

			future.complete("done");
		}
	}

	@Nested
	@DisplayName("DefaultTaskRepository Tests")
	class DefaultTaskRepositoryTests {

		@Test
		@DisplayName("Should put and get task")
		void shouldPutAndGetTask() {
			String taskId = "task_1";
			BackgroundTask task = taskRepository.putTask(taskId, () -> "result");

			assertThat(task).isNotNull();
			assertThat(task.getTaskId()).isEqualTo(taskId);
			assertThat(taskRepository.getTask(taskId)).isSameAs(task);
		}

		@Test
		@DisplayName("Should remove task")
		void shouldRemoveTask() {
			String taskId = "task_1";
			taskRepository.putTask(taskId, () -> "result");
			assertThat(taskRepository.getTask(taskId)).isNotNull();

			taskRepository.removeTask(taskId);
			assertThat(taskRepository.getTask(taskId)).isNull();
		}

		@Test
		@DisplayName("Should clear all tasks")
		void shouldClearAllTasks() {
			taskRepository.putTask("task_1", () -> "r1");
			taskRepository.putTask("task_2", () -> "r2");

			taskRepository.clear();
			assertThat(taskRepository.getTask("task_1")).isNull();
			assertThat(taskRepository.getTask("task_2")).isNull();
		}
	}

}
