/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.autoconfigure.server;

import org.a2aproject.sdk.server.tasks.TaskStateProvider;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskStateProviderAdapterTest {

	@Test
	void missingAndIncompleteTasksHaveSafeStateSemantics() {
		TaskStore taskStore = mock(TaskStore.class);
		Task withoutStatus = task(null);
		Task withoutState = task(status(null));
		Task working = task(status(TaskState.TASK_STATE_WORKING));
		Task completed = task(status(TaskState.TASK_STATE_COMPLETED));
		when(taskStore.get("missing")).thenReturn(null);
		when(taskStore.get("without-status")).thenReturn(withoutStatus);
		when(taskStore.get("without-state")).thenReturn(withoutState);
		when(taskStore.get("working")).thenReturn(working);
		when(taskStore.get("completed")).thenReturn(completed);

		TaskStateProvider provider = TaskStateProviderAdapter.from(taskStore);

		assertThat(provider.isTaskActive("missing")).isFalse();
		assertThat(provider.isTaskFinalized("missing")).isFalse();
		assertThat(provider.isTaskActive("without-status")).isTrue();
		assertThat(provider.isTaskFinalized("without-status")).isFalse();
		assertThat(provider.isTaskActive("without-state")).isTrue();
		assertThat(provider.isTaskFinalized("without-state")).isFalse();
		assertThat(provider.isTaskActive("working")).isTrue();
		assertThat(provider.isTaskFinalized("working")).isFalse();
		assertThat(provider.isTaskActive("completed")).isFalse();
		assertThat(provider.isTaskFinalized("completed")).isTrue();
	}

	private static Task task(TaskStatus status) {
		Task task = mock(Task.class);
		when(task.status()).thenReturn(status);
		return task;
	}

	private static TaskStatus status(TaskState state) {
		TaskStatus status = mock(TaskStatus.class);
		when(status.state()).thenReturn(state);
		return status;
	}

}
