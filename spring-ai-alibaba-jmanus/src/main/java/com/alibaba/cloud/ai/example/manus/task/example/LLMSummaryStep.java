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
package com.alibaba.cloud.ai.example.manus.task.example;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.example.manus.context.ContextKey;
import com.alibaba.cloud.ai.example.manus.context.JManusExecutionContext;
import com.alibaba.cloud.ai.example.manus.task.StatefulJManusStep;
import com.alibaba.cloud.ai.example.manus.task.TaskExecutionException;
import com.alibaba.cloud.ai.example.manus.task.example.DatabaseQueryStep.User;


public class LLMSummaryStep implements StatefulJManusStep {

	private static final Logger logger = LoggerFactory.getLogger(LLMSummaryStep.class);

	// Define context keys for summary results
	public static final ContextKey<String> EXECUTION_SUMMARY_KEY = ContextKey.of("summary.execution_summary",
			String.class);

	public static final ContextKey<String> USER_ANALYSIS_KEY = ContextKey.of("summary.user_analysis", String.class);

	@Override
	public void execute(JManusExecutionContext context) throws TaskExecutionException {
		logger.info("Executing LLM summary step for plan: {}", context.getPlanId());

		try {
			// Get structured data from previous steps
			List<User> allUsers = getPreviousStepResult(context, DatabaseQueryStep.USER_LIST_KEY,
					Collections.emptyList());
			List<User> activeUsers = getPreviousStepResult(context, UserFilterStep.ACTIVE_USERS_KEY,
					Collections.emptyList());

			// Get execution history for comprehensive context
			String executionHistory = getExecutionHistory(context, true);

			// Create LLM prompt with both structured data and execution history
			String analysisPrompt = buildAnalysisPrompt(allUsers, activeUsers, executionHistory);

			// Simulate LLM analysis (in real implementation, this would call an actual
			// LLM service)
			String executionSummary = simulateLLMAnalysis(analysisPrompt);
			String userAnalysis = simulateUserAnalysis(allUsers, activeUsers);

			// Store results for potential subsequent steps or final output
			setStepResult(context, EXECUTION_SUMMARY_KEY, executionSummary);
			setStepResult(context, USER_ANALYSIS_KEY, userAnalysis);

			// Store analysis metadata
			context.putMetadata("analysis_timestamp", java.time.LocalDateTime.now().toString());
			context.putMetadata("total_users_analyzed", allUsers.size());
			context.putMetadata("active_users_analyzed", activeUsers.size());
			context.putMetadata("llm_model", "simulated-gpt-4");

			logger.info("Successfully completed LLM analysis with {} total users and {} active users", allUsers.size(),
					activeUsers.size());

			// Log combined context for demonstration (this shows the full context
			// available)
			if (logger.isDebugEnabled()) {
				String combinedContext = getCombinedContext(context);
				logger.debug("Full context available to this step:\n{}", combinedContext);
			}

		}
		catch (Exception e) {
			String error = "Failed to perform LLM summary analysis: " + e.getMessage();
			logger.error(error, e);
			throw new TaskExecutionException(error, e);
		}
	}

	@Override
	public String getName() {
		return "LLMSummaryStep";
	}

	@Override
	public String getDescription() {
		return "Performs LLM-based analysis and summary using both structured data and execution history";
	}

	@Override
	public boolean requiresExecutionHistory() {
		return true; // This step specifically needs execution history
	}

	/**
	 * Builds a comprehensive analysis prompt combining structured data and execution
	 * history.
	 */
	private String buildAnalysisPrompt(List<User> allUsers, List<User> activeUsers, String executionHistory) {
		StringBuilder prompt = new StringBuilder();

		prompt.append("Please analyze the following user processing workflow and provide insights:\n\n");

		prompt.append("EXECUTION HISTORY:\n");
		prompt.append(executionHistory);
		prompt.append("\n\n");

		prompt.append("STRUCTURED DATA SUMMARY:\n");
		prompt.append("- Total users retrieved: ").append(allUsers.size()).append("\n");
		prompt.append("- Active users after filtering: ").append(activeUsers.size()).append("\n");
		prompt.append("- Activation rate: ")
			.append(allUsers.size() > 0 ? String.format("%.1f%%", (activeUsers.size() * 100.0 / allUsers.size()))
					: "N/A")
			.append("\n\n");

		prompt.append("Please provide:\n");
		prompt.append("1. A summary of the workflow execution\n");
		prompt.append("2. Analysis of user activation patterns\n");
		prompt.append("3. Recommendations for next steps\n");

		return prompt.toString();
	}

	/**
	 * Simulates LLM analysis. In a real implementation, this would call an actual LLM
	 * service.
	 */
	private String simulateLLMAnalysis(String prompt) {
		// This is a simulation - in real code, you would call your LLM service here
		return "WORKFLOW EXECUTION SUMMARY:\n" + "The workflow successfully executed three main phases:\n"
				+ "1. Database Query: Retrieved user data from the primary user database\n"
				+ "2. User Filtering: Applied active status filter to identify engaged users\n"
				+ "3. Analysis Preparation: Structured data for comprehensive analysis\n\n" + "KEY INSIGHTS:\n"
				+ "- Data retrieval was successful with proper error handling\n"
				+ "- Filtering process effectively identified active user subset\n"
				+ "- Context preservation enabled comprehensive analysis\n\n" + "RECOMMENDATIONS:\n"
				+ "- Consider segmenting users by activity level for targeted actions\n"
				+ "- Implement notification system for active users\n"
				+ "- Monitor activation patterns over time for trend analysis";
	}

	/**
	 * Performs detailed user analysis based on structured data.
	 */
	private String simulateUserAnalysis(List<User> allUsers, List<User> activeUsers) {
		if (allUsers.isEmpty()) {
			return "No users available for analysis.";
		}

		double activationRate = (activeUsers.size() * 100.0) / allUsers.size();

		StringBuilder analysis = new StringBuilder();
		analysis.append("USER POPULATION ANALYSIS:\n");
		analysis.append("- Total Users: ").append(allUsers.size()).append("\n");
		analysis.append("- Active Users: ").append(activeUsers.size()).append("\n");
		analysis.append("- Inactive Users: ").append(allUsers.size() - activeUsers.size()).append("\n");
		analysis.append("- Activation Rate: ").append(String.format("%.1f%%", activationRate)).append("\n\n");

		analysis.append("ACTIVATION STATUS BREAKDOWN:\n");
		if (activationRate >= 70) {
			analysis.append("- Excellent activation rate (≥70%): Strong user engagement\n");
		}
		else if (activationRate >= 50) {
			analysis.append("- Good activation rate (50-69%): Solid user base with room for improvement\n");
		}
		else if (activationRate >= 30) {
			analysis.append("- Moderate activation rate (30-49%): Significant re-engagement opportunity\n");
		}
		else {
			analysis.append("- Low activation rate (<30%): Critical need for user retention strategy\n");
		}

		return analysis.toString();
	}

}
