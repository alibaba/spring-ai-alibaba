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

	record TaskResponse(boolean isSuccess, boolean executionSuccessButResultFailed, String stdOut, String stdErr,
			String exceptionMsg) {

		// 执行运行代码任务时发生异常
		public static TaskResponse exception(String msg) {
			return new TaskResponse(false, false, null, null, "An exception occurred while executing the task: " + msg);
		}

		// 执行运行代码任务成功，并且代码正常返回
		public static TaskResponse success(String stdOut) {
			return new TaskResponse(true, false, stdOut, null, null);
		}

		// 执行运行代码任务成功，但是代码异常返回
		public static TaskResponse failure(String stdOut, String stdErr) {
			return new TaskResponse(false, true, stdOut, stdErr, "StdErr: " + stdErr);
		}

		@Override
		public String toString() {
			return "TaskResponse{" + "isSuccess=" + isSuccess + ", stdOut='" + stdOut + '\'' + ", stdErr='" + stdErr
					+ '\'' + ", exceptionMsg='" + exceptionMsg + '\'' + '}';
		}
	}

	enum State {

		READY, RUNNING, REMOVING

	}

}
