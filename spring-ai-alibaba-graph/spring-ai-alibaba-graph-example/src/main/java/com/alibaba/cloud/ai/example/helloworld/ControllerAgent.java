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

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.example.helloworld.tool.Plan;
import com.alibaba.cloud.ai.example.helloworld.tool.PlanningTool;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.LlmNode;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

public class ControllerAgent implements NodeAction {

	private PlanningTool planningTool;

	@Override
	public Map<String, Object> apply(OverAllState t) throws Exception {

		List<Message> messages = (List<Message>)t.value("messages").orElseThrow();
		AssistantMessage assistantMessage = (AssistantMessage)messages.get(messages.size() - 1);
		String planId = assistantMessage.getText();
		Plan plan = planningTool.getPlans(planId);
		String promptForNextStep;
		if (!plan.isFinished()) {
			String step = plan.nextStep();
			promptForNextStep = "What is the next step for " + step + "?";
		} else {
			promptForNextStep = "Plan completed.";
		}
		t.value("prompt_for_next_step", promptForNextStep);
		return Map.of();
	}

	public String think(OverAllState state) {
		Plan plan = (Plan)state.value("plan").orElseThrow();

		if (plan.isFinished()) {
			return "end";
		}

		return "continue";
	}


}
