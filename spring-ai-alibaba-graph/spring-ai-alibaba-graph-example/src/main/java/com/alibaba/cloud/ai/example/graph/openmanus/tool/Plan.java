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
package com.alibaba.cloud.ai.example.graph.openmanus.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.prompt.PromptTemplate;

public class Plan {

	private Map<String, String> stepStatus;

	private int currentStep = 0;

	private String task;

	private String plan_id;

	private List<String> steps;

	public Plan(String task, String planId, List<String> steps) {
		this.task = task;
		this.plan_id = planId;
		this.steps = steps;
		this.stepStatus = new HashMap<>();
	}

	public String getCurrentStep() {
		return String.valueOf(currentStep);
	}

	public String getPlan_id() {
		return plan_id;
	}

	public void updateStepStatus(String stepIndex, String status) {
		stepStatus.put(stepIndex, status);
	}

	public String nextStepPrompt() {
		String nextStepDescription = steps.get(currentStep);
		Map<String, Object> context = new HashMap<>();
		context.put("task", task);
		context.put("planWithSteps", steps);
		context.put("stepIndex", currentStep);
		context.put("nextStepDescription", nextStepDescription);
		context.put("stepStatus", stepStatus);

		currentStep++;

		String template = """
				The task is: {task}

				You are asked to follow the following plan with specific sequential steps to complete this task:
				{planWithSteps}

				You are currently at step {stepIndex} of the plan, which is: {nextStepDescription}.

				Below are the result of the previous steps, which you can use as the context to help you complete the current step:
				  {stepStatus}

				""";
		PromptTemplate promptTemplate = new PromptTemplate(template);
		return promptTemplate.render(context);
	}

	public String nextStep() {
		return steps.get(currentStep++);
	}

	public boolean isFinished() {
		return currentStep == steps.size();
	}

	void setPlan_id(String planId) {
		this.plan_id = planId;
	}

	void setSteps(List<String> steps) {
		this.steps = steps;
	}

}
