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

import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.tool.support.ToolExecuteResult;
import com.alibaba.fastjson.JSON;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.tool.PlanningTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

public class PlanningFlow extends BaseFlow {

	private static final Logger log = LoggerFactory.getLogger(PlanningFlow.class);

	private PlanningTool planningTool;

	private List<String> executorKeys;

	private String activePlanId;

	private Integer currentStepIndex;

	@Autowired
	private LlmService llmService;

	// shared result state between agents.
	private Map<String, Object> resultState;

	public PlanningFlow(Map<String, BaseAgent> agents, Map<String, Object> data) {
		super(agents, data);

		executorKeys = new ArrayList<>();

		if (data.containsKey("executors")) {
			this.executorKeys = (List<String>) data.remove("executors");
		}

		if (data.containsKey("plan_id")) {
			activePlanId = (String) data.remove("plan_id");
		}
		else {
			activePlanId = "plan_" + System.currentTimeMillis();
		}

		if (!data.containsKey("planning_tool")) {
			this.planningTool = PlanningTool.INSTANCE;
		}
		else {
			this.planningTool = (PlanningTool) data.get("planning_tool");
		}

		if (executorKeys.isEmpty()) {
			executorKeys.addAll(agents.keySet());
		}

		this.resultState = new HashMap<>();
	}

	public BaseAgent getExecutor(String stepType) {
		if (stepType != null && agents.containsKey(stepType)) {
			return agents.get(stepType);
		}

		for (String key : executorKeys) {
			if (agents.containsKey(key)) {
				return agents.get(key);
			}
		}
		throw new RuntimeException("agent not found");
	}

	@Override
	public String execute(String inputText) {
		try {
			if (inputText != null && !inputText.isEmpty()) {
				createInitialPlan(inputText);

				if (!planningTool.getPlans().containsKey(activePlanId)) {
					log.error("Plan creation failed. Plan ID " + activePlanId + " not found in planning tool.");
					return "Failed to create plan for: " + inputText;
				}
			}

			StringBuilder result = new StringBuilder();
			while (true) {
				Map.Entry<Integer, Map<String, String>> stepInfoEntry = getCurrentStepInfo();
				if (stepInfoEntry == null) {
					result.append(finalizePlan());
					break;
				}
				currentStepIndex = stepInfoEntry.getKey();
				Map<String, String> stepInfo = stepInfoEntry.getValue();

				if (currentStepIndex == null) {
					result.append(finalizePlan());
					break;
				}

				String stepType = stepInfo != null ? stepInfo.get("type") : null;
				BaseAgent executor = getExecutor(stepType);
				executor.setConversationId(activePlanId);
				String stepResult = executeStep(executor, stepInfo);
				result.append(stepResult).append("\n");

			}

			return result.toString();
		}
		catch (Exception e) {
			log.error("Error in PlanningFlow", e);
			return "Execution failed: " + e.getMessage();
		}
	}

	public void createInitialPlan(String request) {
		log.info("Creating initial plan with ID: " + activePlanId);

		String prompt_template = """
				Create a reasonable plan with clear steps to accomplish the task:

				            {query}

				You can use the planning tool to help you create the plan, assign {plan_id} as the plan id.
				""";

		PromptTemplate promptTemplate = new PromptTemplate(prompt_template);
		Prompt userPrompt = promptTemplate.create(Map.of("plan_id", activePlanId, "query", request));
		ChatResponse response = llmService.getPlanningChatClient()
			.prompt(userPrompt)
			.advisors(memoryAdvisor -> memoryAdvisor.param(CHAT_MEMORY_CONVERSATION_ID_KEY, getConversationId())
				.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
			.user(request)
			.call()
			.chatResponse();

		if (response != null && response.getResult() != null) {
			log.info("Plan creation result: " + response.getResult().getOutput().getText());
		}
		else {
			log.warn("Creating default plan");

			Map<String, Object> defaultArgumentMap = new HashMap<>();
			defaultArgumentMap.put("command", "create");
			defaultArgumentMap.put("plan_id", activePlanId);
			defaultArgumentMap.put("title", "Plan for: " + request.substring(0, Math.min(request.length(), 50))
					+ (request.length() > 50 ? "..." : ""));
			defaultArgumentMap.put("steps", Arrays.asList("Analyze request", "Execute task", "Verify results"));
			planningTool.run(JSON.toJSONString(defaultArgumentMap));
		}
	}

	public Map.Entry<Integer, Map<String, String>> getCurrentStepInfo() {
		if (activePlanId == null || !planningTool.getPlans().containsKey(activePlanId)) {
			log.error("Plan with ID " + activePlanId + " not found");
			return null;
		}

		try {
			Map<String, Object> planData = planningTool.getPlans().get(activePlanId);
			List<String> steps = (List<String>) planData.getOrDefault("steps", new ArrayList<String>());
			List<String> stepStatuses = (List<String>) planData.getOrDefault("step_statuses", new ArrayList<String>());

			for (int i = 0; i < steps.size(); i++) {
				String status;
				if (i >= stepStatuses.size()) {
					status = PlanStepStatus.NOT_STARTED.getValue();
				}
				else {
					status = stepStatuses.get(i);
				}

				if (PlanStepStatus.getActiveStatuses().contains(status)) {
					Map<String, String> stepInfo = new HashMap<>();
					stepInfo.put("text", steps.get(i));

					Pattern pattern = Pattern.compile("\\[([A-Z_]+)\\]");
					Matcher matcher = pattern.matcher(steps.get(i));
					if (matcher.find()) {
						stepInfo.put("type", matcher.group(1).toLowerCase());
					}

					try {
						final int index = i;
						Map<String, Object> argsMap = new HashMap<String, Object>() {
							{
								put("command", "mark_step");
								put("plan_id", activePlanId);
								put("step_index", index);
								put("step_status", PlanStepStatus.IN_PROGRESS.getValue());
							}
						};
						planningTool.run(JSON.toJSONString(argsMap));
					}
					catch (Exception e) {
						log.error("Error marking step as in_progress", e);
						if (i < stepStatuses.size()) {
							stepStatuses.set(i, PlanStepStatus.IN_PROGRESS.getValue());
						}
						else {
							while (stepStatuses.size() < i) {
								stepStatuses.add(PlanStepStatus.NOT_STARTED.getValue());
							}
							stepStatuses.add(PlanStepStatus.IN_PROGRESS.getValue());
						}
						planData.put("step_statuses", stepStatuses);
					}

					return new AbstractMap.SimpleEntry<>(i, stepInfo);
				}
			}

			return null;

		}
		catch (Exception e) {
			log.error("Error finding current step index: " + e.getMessage());
			return null;
		}
	}

	public String executeStep(BaseAgent executor, Map<String, String> stepInfo) {
		try {
			String planStatus = getPlanText();
			String stepText = stepInfo.getOrDefault("text", "Step " + currentStepIndex);

			try {

				String stepResult = executor
					.run(Map.of("planStatus", planStatus, "currentStepIndex", currentStepIndex, "stepText", stepText));

				markStepCompleted();

				return stepResult;
			}
			catch (Exception e) {
				log.error("Error executing step " + currentStepIndex + ": " + e.getMessage());
				return "Error executing step " + currentStepIndex + ": " + e.getMessage();
			}
		}
		catch (Exception e) {
			log.error("Error preparing execution context: " + e.getMessage());
			return "Error preparing execution context: " + e.getMessage();
		}
	}

	public void markStepCompleted() {
		if (currentStepIndex == null) {
			return;
		}

		try {
			Map<String, Object> argsMap = new HashMap<String, Object>() {
				{
					put("command", "mark_step");
					put("plan_id", activePlanId);
					put("step_index", currentStepIndex);
					put("step_status", PlanStepStatus.COMPLETED.getValue());
				}
			};
			ToolExecuteResult result = planningTool.run(JSON.toJSONString(argsMap));
			log.info("Marked step " + currentStepIndex + " as completed in plan " + activePlanId);
		}
		catch (Exception e) {
			log.error("Failed to update plan status: " + e.getMessage());

			Map<String, Map<String, Object>> plans = planningTool.getPlans();
			if (plans.containsKey(activePlanId)) {
				Map<String, Object> planData = plans.get(activePlanId);
				List<String> stepStatuses = (List<String>) planData.getOrDefault("step_statuses",
						new ArrayList<String>());

				while (stepStatuses.size() <= currentStepIndex) {
					stepStatuses.add(PlanStepStatus.NOT_STARTED.getValue());
				}

				stepStatuses.set(currentStepIndex, PlanStepStatus.COMPLETED.getValue());
				planData.put("step_statuses", stepStatuses);
			}
		}
	}

	public String getPlanText() {
		try {
			Map<String, Object> argsMap = new HashMap<String, Object>() {
				{
					put("command", "get");
					put("plan_id", activePlanId);
				}
			};
			ToolExecuteResult result = planningTool.run(JSON.toJSONString(argsMap));

			return result.getOutput() != null ? result.getOutput() : result.toString();
		}
		catch (Exception e) {
			log.error("Error getting plan: " + e.getMessage());
			return generatePlanTextFromStorage();
		}
	}

	public String generatePlanTextFromStorage() {
		try {
			Map<String, Map<String, Object>> plans = planningTool.getPlans();
			if (!plans.containsKey(activePlanId)) {
				return "Error: Plan with ID " + activePlanId + " not found";
			}

			Map<String, Object> planData = plans.get(activePlanId);
			String title = (String) planData.getOrDefault("title", "Untitled Plan");
			List<String> steps = (List<String>) planData.getOrDefault("steps", new ArrayList<String>());
			List<String> stepStatuses = (List<String>) planData.getOrDefault("step_statuses", new ArrayList<String>());
			List<String> stepNotes = (List<String>) planData.getOrDefault("step_notes", new ArrayList<String>());

			while (stepStatuses.size() < steps.size()) {
				stepStatuses.add(PlanStepStatus.NOT_STARTED.getValue());
			}
			while (stepNotes.size() < steps.size()) {
				stepNotes.add("");
			}

			Map<String, Integer> statusCounts = new HashMap<>();
			for (String status : PlanStepStatus.getAllStatuses()) {
				statusCounts.put(status, 0);
			}

			for (String status : stepStatuses) {
				statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
			}

			int completed = statusCounts.get(PlanStepStatus.COMPLETED.getValue());
			int total = steps.size();
			double progress = total > 0 ? (completed / (double) total) * 100 : 0;

			StringBuilder planText = new StringBuilder();
			planText.append("Plan: ").append(title).append(" (ID: ").append(activePlanId).append(")\n");

			for (int i = 0; i < planText.length() - 1; i++) {
				planText.append("=");
			}
			planText.append("\n\n");

			planText.append(String.format("Progress: %d/%d steps completed (%.1f%%)\n", completed, total, progress));
			planText.append(String.format("Status: %d completed, %d in progress, ",
					statusCounts.get(PlanStepStatus.COMPLETED.getValue()),
					statusCounts.get(PlanStepStatus.IN_PROGRESS.getValue())));
			planText.append(
					String.format("%d blocked, %d not started\n\n", statusCounts.get(PlanStepStatus.BLOCKED.getValue()),
							statusCounts.get(PlanStepStatus.NOT_STARTED.getValue())));
			planText.append("Steps:\n");

			Map<String, String> statusMarks = PlanStepStatus.getStatusMarks();

			for (int i = 0; i < steps.size(); i++) {
				String step = steps.get(i);
				String status = stepStatuses.get(i);
				String notes = stepNotes.get(i);
				String statusMark = statusMarks.getOrDefault(status,
						statusMarks.get(PlanStepStatus.NOT_STARTED.getValue()));

				planText.append(String.format("%d. %s %s\n", i, statusMark, step));
				if (!notes.isEmpty()) {
					planText.append("   Notes: ").append(notes).append("\n");
				}
			}

			return planText.toString();
		}
		catch (Exception e) {
			log.error("Error generating plan text from storage: " + e.getMessage());
			return "Error: Unable to retrieve plan with ID " + activePlanId;
		}
	}

	public String finalizePlan() {
		String planText = getPlanText();
		try {
			String prompt = "The plan has been completed. Here is the final plan status:\n\n" + planText
					+ "\n\nPlease provide a summary of what was accomplished and any final thoughts.";

			ChatResponse response = llmService.getFinalizeChatClient().prompt().user(prompt).call().chatResponse();
			return "Plan completed:\n\n" + response.getResult().getOutput().getText();
		}
		catch (Exception e) {
			log.error("Error finalizing plan with LLM: " + e.getMessage());
			return "Plan completed. Error generating summary.";
		}
	}

	public void setActivePlanId(String activePlanId) {
		this.activePlanId = activePlanId;
	}

	public String getConversationId() {
		return activePlanId;
	}

}
