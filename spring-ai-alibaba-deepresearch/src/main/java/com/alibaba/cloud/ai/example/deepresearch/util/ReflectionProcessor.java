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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

/**
 * Reflection utility class providing quality assessment and state management
 * functionality
 *
 * @author sixiyida
 * @since 2025/7/10
 */
public class ReflectionProcessor {

	private static final Logger logger = LoggerFactory.getLogger(ReflectionProcessor.class);

	private final ChatClient reflectionAgent;

	private final int maxReflectionAttempts;

	private final BeanOutputConverter<ReflectionResult> converter;

	public ReflectionProcessor(ChatClient reflectionAgent, int maxReflectionAttempts) {
		this.reflectionAgent = reflectionAgent;
		this.maxReflectionAttempts = maxReflectionAttempts;
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<ReflectionResult>() {
		});
	}

	/**
	 * Check and handle reflection logic
	 * @param step execution step
	 * @param nodeName node name
	 * @param nodeType node type (researcher/coder)
	 * @return ReflectionHandleResult containing whether to continue execution
	 */
	public ReflectionHandleResult handleReflection(Plan.Step step, String nodeName, String nodeType) {
		String currentStatus = step.getExecutionStatus();

		if (currentStatus != null && currentStatus.startsWith(StateUtil.EXECUTION_STATUS_WAITING_REFLECTING)) {
			return performReflection(step, nodeName, nodeType);
		}

		if (currentStatus != null && currentStatus.startsWith(StateUtil.EXECUTION_STATUS_WAITING_PROCESSING)) {
			step.setExecutionStatus(StateUtil.EXECUTION_STATUS_PROCESSING_PREFIX + nodeName);
			step.setExecutionRes("");
			logger.info("Step {} is ready for reprocessing", step.getTitle());
			return ReflectionHandleResult.continueProcessing();
		}

		return ReflectionHandleResult.continueProcessing();
	}

	/**
	 * Perform reflection evaluation
	 */
	private ReflectionHandleResult performReflection(Plan.Step step, String nodeName, String nodeType) {
		try {
			int attemptCount = getReflectionAttemptCount(step);
			if (attemptCount >= maxReflectionAttempts) {
				logger.warn("Step {} has reached maximum reflection attempts {}, forcing pass", step.getTitle(),
						maxReflectionAttempts);
				step.setExecutionStatus(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX + nodeName);
				return ReflectionHandleResult.skipProcessing();
			}

			boolean qualityGood = evaluateStepQuality(step, nodeType);

			if (qualityGood) {
				step.setExecutionStatus(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX + nodeName);
				logger.info("Step {} reflection passed, quality is acceptable", step.getTitle());
				return ReflectionHandleResult.skipProcessing();
			}
			else {
				incrementReflectionAttemptCount(step);
				step.setExecutionStatus(StateUtil.EXECUTION_STATUS_WAITING_PROCESSING + nodeName);
				logger.info("Step {} reflection failed, marked for reprocessing (attempt {})", step.getTitle(),
						attemptCount + 1);
				return ReflectionHandleResult.skipProcessing();
			}

		}
		catch (Exception e) {
			logger.error("Reflection process failed, defaulting to pass: {}", e.getMessage());
			step.setExecutionStatus(StateUtil.EXECUTION_STATUS_COMPLETED_PREFIX + nodeName);
			return ReflectionHandleResult.skipProcessing();
		}
	}

	/**
	 * Evaluate step quality
	 */
	private boolean evaluateStepQuality(Plan.Step step, String nodeType) {
		String evaluationPrompt = buildEvaluationPrompt(step, nodeType);

		try {
			var response = reflectionAgent.prompt(converter.getFormat()).user(evaluationPrompt).call().chatResponse();

			String responseText = response.getResult().getOutput().getText().trim();
			ReflectionResult reflectionResult = converter.convert(responseText);

			// Add execution result to reflection record
			reflectionResult.setExecutionResult(step.getExecutionRes());
			step.addReflectionRecord(reflectionResult);

			logger.debug("Step {} quality evaluation result: passed={}, feedback={}", step.getTitle(),
					reflectionResult.isPassed(), reflectionResult.getFeedback());

			return reflectionResult.isPassed();

		}
		catch (Exception e) {
			logger.error("Quality evaluation failed, defaulting to pass: {}", e.getMessage());
			// Create a default reflection record
			ReflectionResult defaultResult = new ReflectionResult(true,
					"Evaluation failed, system default pass: " + e.getMessage(), step.getExecutionRes());
			step.addReflectionRecord(defaultResult);
			return true;
		}
	}

	/**
	 * Build evaluation prompt
	 */
	private String buildEvaluationPrompt(Plan.Step step, String nodeType) {
		String taskTypeDescription = switch (nodeType) {
			case "researcher" -> "research task";
			case "coder" -> "coding task";
			default -> "task";
		};

		return String.format("""
				Please evaluate the completion quality of the following %s:

				**Task Title:** %s

				**Task Description:** %s

				**Completion Result:**
				%s
				""", taskTypeDescription, step.getTitle(), step.getDescription(), step.getExecutionRes());
	}

	/**
	 * Get reflection attempt count
	 */
	private int getReflectionAttemptCount(Plan.Step step) {
		if (step.getReflectionHistory() != null) {
			return step.getReflectionHistory().size();
		}

		// Compatible with old status string parsing
		String status = step.getExecutionStatus();
		if (status != null && status.contains("_attempt_")) {
			try {
				String[] parts = status.split("_attempt_");
				if (parts.length > 1) {
					return Integer.parseInt(parts[1].split("_")[0]);
				}
			}
			catch (NumberFormatException e) {
				logger.debug("Failed to parse reflection attempt count: {}", status);
			}
		}
		return 0;
	}

	/**
	 * Increment reflection attempt count
	 */
	private void incrementReflectionAttemptCount(Plan.Step step) {
		int currentCount = getReflectionAttemptCount(step);
		String baseStatus = step.getExecutionStatus().split("_attempt_")[0];
		step.setExecutionStatus(baseStatus + "_attempt_" + (currentCount + 1));
	}

	/**
	 * Reflection handle result class
	 */
	public static class ReflectionHandleResult {

		private final boolean shouldContinueProcessing;

		private ReflectionHandleResult(boolean shouldContinueProcessing) {
			this.shouldContinueProcessing = shouldContinueProcessing;
		}

		public static ReflectionHandleResult continueProcessing() {
			return new ReflectionHandleResult(true);
		}

		public static ReflectionHandleResult skipProcessing() {
			return new ReflectionHandleResult(false);
		}

		public boolean shouldContinueProcessing() {
			return shouldContinueProcessing;
		}

	}

}
