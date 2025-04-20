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
package com.alibaba.cloud.ai.example.graph.openmanus;

import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.example.graph.openmanus.tool.Plan;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.PlanningTool;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson.JSON;

public class SupervisorAgent implements NodeAction {

	private final PlanningTool planningTool;

	public SupervisorAgent(PlanningTool planningTool) {
		this.planningTool = planningTool;
	}

	@Override
	public Map<String, Object> apply(OverAllState t) throws Exception {

		String planStr = (String) t.value("plan").orElseThrow();
		Plan tempPlan = parsePlan(planStr);
		Plan plan = planningTool.getGraphPlan(tempPlan.getPlan_id());

		Optional<Object> optionalOutput = t.value("step_output");

		if (optionalOutput.isPresent()) {
			String finalStepOutput = String.format("This is the final output of step %s:\n %s", plan.getCurrentStep(),
					optionalOutput.get());
			plan.updateStepStatus(plan.getCurrentStep(), finalStepOutput);
		}

		String promptForNextStep;
		if (!plan.isFinished()) {
			promptForNextStep = plan.nextStepPrompt();
		}
		else {
			promptForNextStep = "Plan completed.";
		}

		return Map.of("step_prompt", promptForNextStep);
	}

	public String think(OverAllState state) {

		String nextPrompt = (String) state.value("step_prompt").orElseThrow();

		if (nextPrompt.equalsIgnoreCase("Plan completed.")) {
			state.updateState(Map.of("final_output", state.value("step_output").orElseThrow()));
			return "end";
		}

		return "continue";
	}

	private Plan parsePlan(String planJson) {
		planJson = removeMarkdownCodeBlockSyntax(planJson);
		return JSON.parseObject(planJson, Plan.class);
	}

	/**
	 * 移除字符串中的Markdown代码块标记（```json 和 ```） 如果字符串不包含这些标记，则返回原始字符串
	 * @param input 可能包含Markdown代码块标记的字符串
	 * @return 去除了代码块标记的字符串
	 */
	public static String removeMarkdownCodeBlockSyntax(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		// 去除开头的 ```json 或 ```任何语言
		String result = input.trim();
		if (result.startsWith("```")) {
			int firstLineEnd = result.indexOf('\n');
			if (firstLineEnd != -1) {
				result = result.substring(firstLineEnd).trim();
			}
		}

		// 去除结尾的 ```
		if (result.endsWith("```")) {
			result = result.substring(0, result.length() - 3).trim();
		}

		return result;
	}

}
