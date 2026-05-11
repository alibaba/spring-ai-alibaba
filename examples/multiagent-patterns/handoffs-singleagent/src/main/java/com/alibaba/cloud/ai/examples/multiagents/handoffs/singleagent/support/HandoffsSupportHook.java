/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent.support;

import com.alibaba.cloud.ai.examples.multiagents.handoffs.singleagent.tools.SupportTools;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

/**
 * Hook that provides step-based configuration for the customer support handoffs workflow:
 * key strategies for workflow state (current_step, warranty_status, issue_type) and
 * a {@link StepConfigInterceptor} that applies the right prompt and tools per step.
 */
public class HandoffsSupportHook extends ModelHook {

	private static final String WARRANTY_COLLECTOR_PROMPT = """
			You are a customer support agent helping with device issues.

			CURRENT STEP: Warranty verification

			At this step, you need to:
			1. Greet the customer warmly
			2. Ask if their device is under warranty
			3. Use record_warranty_status to record their response and move to the next step

			Be conversational and friendly. Don't ask multiple questions at once.""";

	private static final String ISSUE_CLASSIFIER_PROMPT = """
			You are a customer support agent helping with device issues.

			CURRENT STEP: Issue classification
			CUSTOMER INFO: Warranty status is {warranty_status}

			At this step, you need to:
			1. Ask the customer to describe their issue
			2. Determine if it's a hardware issue (physical damage, broken parts) or software issue (app crashes, performance)
			3. Use record_issue_type to record the classification and move to the next step

			If unclear, ask clarifying questions before classifying.""";

	private static final String RESOLUTION_SPECIALIST_PROMPT = """
			You are a customer support agent helping with device issues.

			CURRENT STEP: Resolution
			CUSTOMER INFO: Warranty status is {warranty_status}, issue type is {issue_type}

			At this step, you need to:
			1. For SOFTWARE issues: provide troubleshooting steps using provide_solution
			2. For HARDWARE issues:
			   - If IN WARRANTY: explain warranty repair process using provide_solution
			   - If OUT OF WARRANTY: escalate_to_human for paid repair options

			Be specific and helpful in your solutions.""";

	private final ModelInterceptor stepConfigInterceptor;

	public HandoffsSupportHook() {
		ToolCallback recordWarranty = SupportTools.recordWarrantyStatusTool();
		ToolCallback recordIssueType = SupportTools.recordIssueTypeTool();
		ToolCallback provideSolution = SupportTools.provideSolutionTool();
		ToolCallback escalateToHuman = SupportTools.escalateToHumanTool();

		this.stepConfigInterceptor = new StepConfigInterceptor(Map.of(
				SupportStateConstants.STEP_WARRANTY_COLLECTOR,
				new StepConfigInterceptor.StepConfig(WARRANTY_COLLECTOR_PROMPT, List.of(recordWarranty), List.of()),
				SupportStateConstants.STEP_ISSUE_CLASSIFIER,
				new StepConfigInterceptor.StepConfig(ISSUE_CLASSIFIER_PROMPT, List.of(recordIssueType),
						List.of(SupportStateConstants.WARRANTY_STATUS)),
				SupportStateConstants.STEP_RESOLUTION_SPECIALIST,
				new StepConfigInterceptor.StepConfig(RESOLUTION_SPECIALIST_PROMPT, List.of(provideSolution, escalateToHuman),
						List.of(SupportStateConstants.WARRANTY_STATUS, SupportStateConstants.ISSUE_TYPE))));
	}

	@Override
	public String getName() {
		return "HandoffsSupport";
	}

	@Override
	public List<ModelInterceptor> getModelInterceptors() {
		return List.of(stepConfigInterceptor);
	}

	@Override
	public Map<String, KeyStrategy> getKeyStrategys() {
		return Map.of(
				SupportStateConstants.CURRENT_STEP, new ReplaceStrategy(),
				SupportStateConstants.WARRANTY_STATUS, new ReplaceStrategy(),
				SupportStateConstants.ISSUE_TYPE, new ReplaceStrategy());
	}
}
