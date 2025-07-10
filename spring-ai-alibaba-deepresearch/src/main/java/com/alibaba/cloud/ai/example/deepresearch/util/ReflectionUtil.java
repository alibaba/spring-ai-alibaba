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
import com.alibaba.cloud.ai.example.deepresearch.model.dto.ReflectionResult;

import java.util.List;

/**
 * Reflection utilities class providing common reflection-related static methods
 *
 * @author sixiyida
 * @since 2025/7/10
 */
public class ReflectionUtil {

	/**
	 * Check if step should be processed by the specified node
	 * @param step execution step
	 * @param nodeName node name
	 * @return whether to process
	 */
	public static boolean shouldProcessStep(Plan.Step step, String nodeName) {
		String status = step.getExecutionStatus();
		if (status == null) {
			return false;
		}

		// Handle assigned steps
		if (status.equals(StateUtil.EXECUTION_STATUS_ASSIGNED_PREFIX + nodeName)) {
			return true;
		}

		// Handle steps waiting for reprocessing
		if (status.equals(StateUtil.EXECUTION_STATUS_WAITING_PROCESSING + nodeName)) {
			return true;
		}

		// Handle steps waiting for reflection
		if (status.equals(StateUtil.EXECUTION_STATUS_WAITING_REFLECTING + nodeName)) {
			return true;
		}

		return false;
	}

	/**
	 * Build basic reflection history message content (common part)
	 * @param step execution step
	 * @return reflection history Markdown content
	 */
	public static String buildReflectionHistoryContent(Plan.Step step) {
		List<ReflectionResult> reflectionHistory = step.getReflectionHistory();
		if (reflectionHistory == null || reflectionHistory.isEmpty()) {
			return "";
		}

		StringBuilder content = new StringBuilder();
		content.append("## Previous Attempts and Feedback\n\n");
		content.append(
				"**Important Note**: The following are reflection results from previous attempts. Please refer to this feedback to improve your response and avoid repeating the same issues.\n\n");

		for (int i = 0; i < reflectionHistory.size(); i++) {
			ReflectionResult record = reflectionHistory.get(i);
			content.append("### Attempt ").append(i + 1).append("\n\n");

			// Add previous execution result
			if (record.hasExecutionResult()) {
				content.append("**Previous Execution Result**:\n");
				content.append(record.getExecutionResult()).append("\n\n");
			}

			content.append("**Reflection Feedback**:\n");
			content.append(record.getFeedback()).append("\n\n");

			content.append("**Evaluation Result**: ").append(record.isPassed() ? "Passed" : "Failed").append("\n\n");
			content.append("---\n\n");
		}

		return content.toString();
	}

	/**
	 * Check if should continue after reflection
	 */
	public static boolean shouldContinueAfterReflection(ReflectionProcessor.ReflectionHandleResult result) {
		return result != null && result.shouldContinueProcessing();
	}

	/**
	 * Get appropriate status setting (based on whether reflection processor is available)
	 * @param hasReflectionProcessor whether reflection processor is available
	 * @param nodeName node name
	 * @return status to be set
	 */
	public static String getCompletionStatus(boolean hasReflectionProcessor, String nodeName) {
		if (hasReflectionProcessor) {
			return StateUtil.EXECUTION_STATUS_WAITING_REFLECTING + nodeName;
		}
		else {
			return StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX + nodeName;
		}
	}

	/**
	 * Check if step has reflection history
	 */
	public static boolean hasReflectionHistory(Plan.Step step) {
		return step.getReflectionHistory() != null && !step.getReflectionHistory().isEmpty();
	}

}
