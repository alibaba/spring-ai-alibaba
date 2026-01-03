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
package com.alibaba.cloud.ai.graph.agent.tools;

import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor.Todo;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor.TodoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.*;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY;
import static org.junit.jupiter.api.Assertions.*;


class WriteTodosToolTest {

	private WriteTodosTool writeTodosTool;

	@BeforeEach
	void setUp() {
		writeTodosTool = new WriteTodosTool();
	}

	@Test
	void testNormalTodoUpdate() {
		Map<String, Object> contextData = new HashMap<>();
		Map<String, Object> extraState = new HashMap<>();
		contextData.put(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, extraState);

		ToolContext toolContext = new ToolContext(contextData);

		List<Todo> todos = new ArrayList<>();
		todos.add(new Todo("Task 1", TodoStatus.PENDING));
		todos.add(new Todo("Task 2", TodoStatus.COMPLETED));

		WriteTodosTool.Request request = new WriteTodosTool.Request(todos);
		WriteTodosTool.Response response = writeTodosTool.apply(request, toolContext);

		assertNotNull(response);
		assertFalse(response.message().startsWith("Error:"));
		assertTrue(response.message().contains("Updated todo list"));

		assertEquals(todos, extraState.get("todos"));
	}

	@Test
	void testNullToolContext() {
		Map<String, Object> emptyContextData = new HashMap<>();
		ToolContext toolContext = new ToolContext(emptyContextData);

		List<Todo> todos = Collections.singletonList(new Todo("Task 1", TodoStatus.PENDING));
		WriteTodosTool.Request request = new WriteTodosTool.Request(todos);
		WriteTodosTool.Response response = writeTodosTool.apply(request, toolContext);

		assertTrue(response.message().startsWith("Error:"));
		assertTrue(response.message().contains("Extra state is not initialized"));
	}

	@Test
	void testInvalidExtraStateType() {
		Map<String, Object> contextData = new HashMap<>();
		contextData.put(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, "invalid type");

		ToolContext toolContext = new ToolContext(contextData);

		List<Todo> todos = Collections.singletonList(new Todo("Task 1", TodoStatus.PENDING));
		WriteTodosTool.Request request = new WriteTodosTool.Request(todos);
		WriteTodosTool.Response response = writeTodosTool.apply(request, toolContext);

		assertTrue(response.message().startsWith("Error:"));
		assertTrue(response.message().contains("Extra state has invalid type"));
	}

	@Test
	void testEmptyTodoList() {
		Map<String, Object> contextData = new HashMap<>();
		Map<String, Object> extraState = new HashMap<>();
		contextData.put(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, extraState);

		ToolContext toolContext = new ToolContext(contextData);

		List<Todo> todos = Collections.emptyList();
		WriteTodosTool.Request request = new WriteTodosTool.Request(todos);
		WriteTodosTool.Response response = writeTodosTool.apply(request, toolContext);

		assertNotNull(response);
		assertFalse(response.message().startsWith("Error:"));
		assertEquals(todos, extraState.get("todos"));
	}

	@Test
	void testMultipleTodoUpdates() {
		Map<String, Object> contextData = new HashMap<>();
		Map<String, Object> extraState = new HashMap<>();
		contextData.put(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, extraState);

		ToolContext toolContext = new ToolContext(contextData);

		List<Todo> todos1 = Collections.singletonList(new Todo("Task 1", TodoStatus.PENDING));
		WriteTodosTool.Request request1 = new WriteTodosTool.Request(todos1);
		WriteTodosTool.Response response1 = writeTodosTool.apply(request1, toolContext);

		assertFalse(response1.message().startsWith("Error:"));
		assertEquals(todos1, extraState.get("todos"));
		List<Todo> todos2 = Arrays.asList(new Todo("Task 2", TodoStatus.PENDING),
				new Todo("Task 3", TodoStatus.COMPLETED));
		WriteTodosTool.Request request2 = new WriteTodosTool.Request(todos2);
		WriteTodosTool.Response response2 = writeTodosTool.apply(request2, toolContext);

		assertFalse(response2.message().startsWith("Error:"));
		assertEquals(todos2, extraState.get("todos"));
	}

	@Test
	void testTodoStatusTransitions() {
		Map<String, Object> contextData = new HashMap<>();
		Map<String, Object> extraState = new HashMap<>();
		contextData.put(AGENT_STATE_FOR_UPDATE_CONTEXT_KEY, extraState);

		ToolContext toolContext = new ToolContext(contextData);

		List<Todo> todos = Arrays.asList(new Todo("Task 1", TodoStatus.PENDING),
				new Todo("Task 2", TodoStatus.IN_PROGRESS),
				new Todo("Task 3", TodoStatus.COMPLETED));

		WriteTodosTool.Request request = new WriteTodosTool.Request(todos);
		WriteTodosTool.Response response = writeTodosTool.apply(request, toolContext);

		assertNotNull(response);
		assertFalse(response.message().startsWith("Error:"));
		assertEquals(todos, extraState.get("todos"));
		assertEquals(3, todos.size());
	}

}
