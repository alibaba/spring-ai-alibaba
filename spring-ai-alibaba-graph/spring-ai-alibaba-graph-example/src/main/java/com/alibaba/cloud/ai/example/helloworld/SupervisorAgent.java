/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.helloworld;

import java.util.Map;

import com.alibaba.cloud.ai.example.helloworld.tool.Plan;
import com.alibaba.cloud.ai.example.helloworld.tool.PlanningTool;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

public class SupervisorAgent implements NodeAction {

	private PlanningTool planningTool = new PlanningTool(Map.of());

	@Override
	public Map<String, Object> apply(OverAllState t) throws Exception {
		String planId = (String) t.value("plan").orElseThrow();
		Plan plan = planningTool.getPlans(planId);
		String promptForNextStep;
		if (!plan.isFinished()) {
			String step = plan.nextStep();
			promptForNextStep = "What is the next step for " + step + "?";
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

}
