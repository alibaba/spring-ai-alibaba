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

package com.alibaba.cloud.ai.service.code;

/**
 * 运行Python任务的容器池接口
 *
 * @author vlsmb
 * @since 2025/7/12
 */
public interface CodePoolExecutorService {

	TaskResponse runTask(TaskRequest request);

	record TaskRequest(String code, String input, String requirement) {

	}

	record TaskResponse(boolean isSuccess, String stdOut, String stdErr, String exceptionMsg) {
		public static TaskResponse error(String msg) {
			return new TaskResponse(false, null, null, "An exception occurred while executing the task: " + msg);
		}

		@Override
		public String toString() {
			return "TaskResponse{" + "isSuccess=" + isSuccess + ", stdOut='" + stdOut + '\'' + ", stdErr='" + stdErr
					+ '\'' + ", exceptionMsg='" + exceptionMsg + '\'' + '}';
		}
	}

	enum State {

		READY, RUNNING

	}

}
