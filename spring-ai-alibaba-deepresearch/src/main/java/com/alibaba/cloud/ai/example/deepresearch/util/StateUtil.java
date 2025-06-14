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

package com.alibaba.cloud.ai.example.deepresearch.util;

import com.alibaba.cloud.ai.example.deepresearch.model.dto.Plan;
import com.alibaba.cloud.ai.graph.OverAllState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yingzi
 * @since 2025/6/9
 */

public class StateUtil {

	public static final String EXECUTION_STATUS_ASSIGNED_PREFIX = "assigned_";

	public static final String EXECUTION_STATUS_PROCESSING_PREFIX = "processing_";

	public static final String EXECUTION_STATUS_COMPLETED_PREFIX = "completed_";

	public static List<String> getMessagesByType(OverAllState state, String name) {
		return state.value(name, List.class).map(obj -> new ArrayList<>((List<String>) obj)).orElseGet(ArrayList::new);
	}

	public static String getQuery(OverAllState state) {
		return state.value("query", "草莓蛋糕怎么做呀");
	}

	public static Plan getPlan(OverAllState state) {
		return state.value("current_plan", Plan.class).get();
	}

	public static Integer getPlanIterations(OverAllState state) {
		return state.value("plan_iterations", 0);
	}

	public static Integer getPlanMaxIterations(OverAllState state) {
		return state.value("plan_max_iterations", 1);
	}

	public static Integer getMaxStepNum(OverAllState state) {
		return state.value("max_step_num", 3);
	}

	public static String getThreadId(OverAllState state) {
		return state.value("thread_id", "__default__");
	}

	public static boolean getAutoAcceptedPlan(OverAllState state) {
		return state.value("auto_accepted_plan", true);
	}

}
