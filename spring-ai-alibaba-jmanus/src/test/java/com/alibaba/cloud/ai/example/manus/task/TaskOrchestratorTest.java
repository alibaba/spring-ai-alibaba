/* * Copyright 2025 the original author or authors. * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * *      https://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */
package com.alibaba.cloud.ai.example.manus.task;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.alibaba.cloud.ai.example.manus.context.ContextKey;
import com.alibaba.cloud.ai.example.manus.context.JManusExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class TaskOrchestratorTest {

	@Mock
	private ExecutionContext mockExecutionContext;

	@InjectMocks
	private TaskOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator = new TaskOrchestrator();
	}

	@Test
	@DisplayName("Should initialize TaskOrchestrator correctly")
	void testTaskOrchestratorInitialization() {
		TaskOrchestrator newOrchestrator = new TaskOrchestrator();
		assertNotNull(newOrchestrator);
	}

	@Test
	@DisplayName("Should handle null task list")
	void testHandleNullTaskList() {
		assertThrows(IllegalArgumentException.class, () -> {
			orchestrator.executeTasks("test-plan", null, mockExecutionContext);
		});
	}

	@Test
	@DisplayName("Should handle null plan ID")
	void testHandleNullPlanId() {
		List<StatefulTask> tasks = Collections.singletonList(new TestTask("Task", true));
		assertThrows(IllegalArgumentException.class, () -> {
			orchestrator.executeTasks(null, tasks, mockExecutionContext);
		});
	}

	@Test
	@DisplayName("Should handle empty plan ID")
	void testHandleEmptyPlanId() {
		List<StatefulTask> tasks = Collections.singletonList(new TestTask("Task", true));
		assertThrows(IllegalArgumentException.class, () -> {
			orchestrator.executeTasks("", tasks, mockExecutionContext);
		});
	}

	@Test
	@DisplayName("Should handle empty task list")
	void testHandleEmptyTaskList() {
		List<StatefulTask> tasks = Collections.emptyList();
		TaskOrchestrator.ExecutionResult result = orchestrator.executeTasks("test-plan", tasks, mockExecutionContext);
		assertNotNull(result);
		assertTrue(result.isSuccess());
		assertTrue(result.getTaskResults().isEmpty());
	}

	@Test
	@DisplayName("Should handle single successful task")
	void testHandleSingleSuccessfulTask() {
		StatefulTask successfulTask = new TestTask("SuccessTask", true);
		List<StatefulTask> tasks = Collections.singletonList(successfulTask);
		TaskOrchestrator.ExecutionResult result = orchestrator.executeTasks("test-plan", tasks, mockExecutionContext);

		assertNotNull(result);
		assertTrue(result.isSuccess());
		assertEquals(1, result.getTaskResults().size());
		assertTrue(result.getTaskResults().get(0).isSuccess());
	}

	@Test
	@DisplayName("Should handle single failed task")
	void testHandleSingleFailedTask() {
		StatefulTask failedTask = new TestTask("FailTask", false);
		List<StatefulTask> tasks = Collections.singletonList(failedTask);

		TaskOrchestrator.ExecutionResult result = orchestrator.executeTasks("test-plan", tasks, mockExecutionContext);

		assertNotNull(result);
		assertFalse(result.isSuccess());
		assertNotNull(result.getErrorMessage());
	}

	@Test
	@DisplayName("Should execute multiple tasks in sequence")
	void testExecuteMultipleTasks() {
		List<StatefulTask> tasks = Arrays.asList(new TestTask("Task1", true), new TestTask("Task2", true),
				new TestTask("Task3", true));

		TaskOrchestrator.ExecutionResult result = orchestrator.executeTasks("test-plan", tasks, mockExecutionContext);

		assertNotNull(result);
		assertTrue(result.isSuccess());
		assertEquals(3, result.getTaskResults().size());
		assertTrue(result.getTaskResults().stream().allMatch(TaskOrchestrator.TaskExecutionResult::isSuccess));
	}

	@Test
	@DisplayName("Should stop execution on first failure")
	void testStopOnFirstFailure() {
		List<StatefulTask> tasks = Arrays.asList(new TestTask("Task1", true), new TestTask("FailingTask", false),
				new TestTask("Task3", true));

		TaskOrchestrator.ExecutionResult result = orchestrator.executeTasks("test-plan", tasks, mockExecutionContext);

		assertNotNull(result);
		assertFalse(result.isSuccess());
		// Only first task completed, second failed
		assertEquals(2, result.getTaskResults().size());
		assertTrue(result.getTaskResults().get(0).isSuccess());
		assertFalse(result.getTaskResults().get(1).isSuccess());
		assertNotNull(result.getErrorMessage());
	}

	@Test
	@DisplayName("Should handle task execution exception")
	void testHandleTaskExecutionException() {
		StatefulTask exceptionTask = new TestTask("ExceptionTask", new TaskExecutionException("Test exception"));
		List<StatefulTask> tasks = Collections.singletonList(exceptionTask);

		TaskOrchestrator.ExecutionResult result = orchestrator.executeTasks("test-plan", tasks, mockExecutionContext);

		assertNotNull(result);
		assertFalse(result.isSuccess());
		assertNotNull(result.getErrorMessage());
		assertTrue(result.getErrorMessage().contains("Test exception"));
	}

	/**
	 * Simple test task implementation for testing purposes.
	 */
	private static class TestTask implements StatefulTask {

		private final String name;

		private final boolean shouldSucceed;

		private final TaskExecutionException exception;

		public TestTask(String name, boolean shouldSucceed) {
			this.name = name;
			this.shouldSucceed = shouldSucceed;
			this.exception = null;
		}

		public TestTask(String name, TaskExecutionException exception) {
			this.name = name;
			this.shouldSucceed = false;
			this.exception = exception;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void execute(JManusExecutionContext context) throws TaskExecutionException {
			if (exception != null) {
				throw exception;
			}
			if (!shouldSucceed) {
				throw new TaskExecutionException("Task " + name + " failed");
			}
			// Task execution successful - store result in context
			ContextKey<String> resultKey = ContextKey.of("task_" + name + "_result", String.class);
			context.put(resultKey, "SUCCESS");
		}

		@Override
		public String getDescription() {
			return "Test task: " + name;
		}

	}

}
