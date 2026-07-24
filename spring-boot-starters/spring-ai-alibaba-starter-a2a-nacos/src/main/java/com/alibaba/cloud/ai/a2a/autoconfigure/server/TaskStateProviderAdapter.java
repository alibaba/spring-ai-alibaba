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
import org.a2aproject.sdk.spec.TaskStatus;

final class TaskStateProviderAdapter {

	private TaskStateProviderAdapter() {
	}

	static TaskStateProvider from(TaskStore taskStore) {
		if (taskStore instanceof TaskStateProvider taskStateProvider) {
			return taskStateProvider;
		}
		return new TaskStateProvider() {
			@Override
			public boolean isTaskActive(String taskId) {
				Task task = taskStore.get(taskId);
				return task != null && !isFinal(task);
			}

			@Override
			public boolean isTaskFinalized(String taskId) {
				return isFinal(taskStore.get(taskId));
			}
		};
	}

	private static boolean isFinal(Task task) {
		if (task == null) {
			return false;
		}
		TaskStatus status = task.status();
		return status != null && status.state() != null && status.state().isFinal();
	}

}
