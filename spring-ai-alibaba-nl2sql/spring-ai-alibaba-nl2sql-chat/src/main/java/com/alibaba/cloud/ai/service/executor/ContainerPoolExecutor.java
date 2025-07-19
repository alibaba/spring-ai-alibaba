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
package com.alibaba.cloud.ai.service.executor;

import com.alibaba.cloud.ai.config.ContainerProperties;

/**
 * 运行Python任务的容器池接口
 *
 * @author vlsmb
 * @since 2025/7/12
 */
public interface ContainerPoolExecutor {

	TaskResponse runTask(TaskRequest request);

	static ContainerPoolExecutor getInstance(ContainerProperties properties) {
		if (properties.getContainerImpl().equals(ContainerProperties.ContainerImpl.DOCKER)) {
			return new DockerContainerPoolExecutor(properties);
		}
		else {
			throw new IllegalArgumentException("Unknown container impl: " + properties.getContainerImpl());
		}
	}

	record TaskRequest(String code, String input, String requirement) {

	}

	record TaskResponse(String output) {
		public static TaskResponse error(String msg) {
			return new TaskResponse("An exception occurred while executing the task: " + msg);
		}
	}

	enum State {

		READY, RUNNING

	}

}
