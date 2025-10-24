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
package com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit;

/**
 * Exception raised when model call limits are exceeded.
 */
public class ModelCallLimitExceededException extends RuntimeException {

	private final int threadCount;
	private final int runCount;
	private final Integer threadLimit;
	private final Integer runLimit;

	public ModelCallLimitExceededException(int threadCount, int runCount, Integer threadLimit, Integer runLimit) {
		super(buildMessage(threadCount, runCount, threadLimit, runLimit));
		this.threadCount = threadCount;
		this.runCount = runCount;
		this.threadLimit = threadLimit;
		this.runLimit = runLimit;
	}

	private static String buildMessage(int threadCount, int runCount, Integer threadLimit, Integer runLimit) {
		StringBuilder sb = new StringBuilder("Model call limits exceeded: ");
		if (threadLimit != null && threadCount >= threadLimit) {
			sb.append(String.format("thread limit (%d/%d)", threadCount, threadLimit));
		}
		if (runLimit != null && runCount >= runLimit) {
			if (threadLimit != null && threadCount >= threadLimit) {
				sb.append(", ");
			}
			sb.append(String.format("run limit (%d/%d)", runCount, runLimit));
		}
		return sb.toString();
	}

	public int getThreadCount() {
		return threadCount;
	}

	public int getRunCount() {
		return runCount;
	}

	public Integer getThreadLimit() {
		return threadLimit;
	}

	public Integer getRunLimit() {
		return runLimit;
	}
}

