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

package com.alibaba.cloud.ai.example.deepresearch.tool;

import org.springframework.ai.tool.annotation.Tool;

/**
 * @author yingzi
 * @date 2025/5/17 18:10
 */

public class PlannerTool {

	@Tool(name = "handoff_to_planner", description = "Handoff to planner agent to do plan.")
	public void handoffToPlanner(String taskTitle) {
		// This method is not returning anything. It is used as a way for LLM
		// to signal that it needs to hand off to the planner agent.
	}

}
