/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.flow;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum PlanStepStatus {

	NOT_STARTED("not_started"), IN_PROGRESS("in_progress"), COMPLETED("completed"), BLOCKED("blocked");

	private final String value;

	PlanStepStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static List<String> getAllStatuses() {
		// Return a list of all possible step status values
		return Arrays.stream(PlanStepStatus.values()).map(PlanStepStatus::getValue).collect(Collectors.toList());
	}

	public static List<String> getActiveStatuses() {
		// Return a list of values representing active statuses (not started or in
		// progress)
		return Arrays.asList(NOT_STARTED.getValue(), IN_PROGRESS.getValue());
	}

	public static Map<String, String> getStatusMarks() {
		// Return a mapping of statuses to their marker symbols
		return new HashMap<String, String>() {
			{
				put(COMPLETED.getValue(), "[✓]");
				put(IN_PROGRESS.getValue(), "[→]");
				put(BLOCKED.getValue(), "[!]");
				put(NOT_STARTED.getValue(), "[ ]");
			}
		};
	}

	@Override
	public String toString() {
		return value;
	}

}
