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
package com.alibaba.cloud.ai.manus.runtime.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ID converter, used to handle and convert the relationship between planId and
 * planTemplateId This class helps the system to be compatible with the old and new
 * interfaces, allowing both planId and planTemplateId to be supported
 */
@Component
public class PlanIdDispatcher implements IPlanIdDispatcher {

	private static final Logger logger = LoggerFactory.getLogger(PlanIdDispatcher.class);

	// planId prefix constant
	private static final String PLAN_ID_PREFIX = "plan-";

	// planTemplateId prefix constant
	private static final String PLAN_TEMPLATE_ID_PREFIX = "planTemplate-";

	/**
	 * Check if the ID is in planTemplateId format
	 * @param id ID to check
	 * @return true if the ID is in planTemplateId format, false otherwise
	 */
	public boolean isPlanTemplateId(String id) {
		return id != null && id.startsWith(PLAN_TEMPLATE_ID_PREFIX);
	}

	/**
	 * Check if the ID is in planId format
	 * @param id ID to check
	 * @return true if the ID is in planId format, false otherwise
	 */
	public boolean isPlanId(String id) {
		return id != null && id.startsWith(PLAN_ID_PREFIX);
	}

	/**
	 * Convert planTemplateId to planId
	 * @param planTemplateId planTemplateId to convert
	 * @return converted planId
	 */
	public String toPlanId(String planTemplateId) {
		if (planTemplateId == null) {
			return null;
		}

		if (isPlanId(planTemplateId)) {
			return planTemplateId; // Already in planId format, return directly
		}

		// Generate a new unique planId for both planTemplateId and planId formats
		// Use timestamp and random number to ensure uniqueness
		String uniqueId = PLAN_ID_PREFIX + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);

		if (isPlanTemplateId(planTemplateId)) {
			logger.debug("Generated new unique planId [{}] from planTemplateId [{}]", uniqueId, planTemplateId);
		}
		else {
			logger.warn("Unknown ID format [{}], generated new unique planId [{}]", planTemplateId, uniqueId);
		}

		return uniqueId;
	}

	/**
	 * Convert planId to planTemplateId
	 * @param planId planId to convert
	 * @return converted planTemplateId
	 */
	public String toPlanTemplateId(String planId) {
		if (planId == null) {
			return null;
		}

		if (isPlanTemplateId(planId)) {
			return planId; // Already in planTemplateId format, return directly
		}

		if (isPlanId(planId)) {
			// Extract the numeric part of the ID and create a new planTemplateId
			String numericPart = planId.substring(PLAN_ID_PREFIX.length());
			String planTemplateId = PLAN_TEMPLATE_ID_PREFIX + numericPart;
			logger.debug("Converted planId [{}] to planTemplateId [{}]", planId, planTemplateId);
			return planTemplateId;
		}

		// For IDs that do not match any known formats, add the planTemplateId prefix
		logger.warn("Unknown ID format [{}], added planTemplateId prefix", planId);
		return PLAN_TEMPLATE_ID_PREFIX + planId;
	}

	/**
	 * Generate a new planTemplateId
	 * @return new planTemplateId
	 */
	public String generatePlanTemplateId() {
		String planTemplateId = PLAN_TEMPLATE_ID_PREFIX + System.currentTimeMillis();
		logger.debug("Generated new planTemplateId: {}", planTemplateId);
		return planTemplateId;
	}

	/**
	 * Generate a new planId
	 * @return new planId
	 */
	public String generatePlanId() {
		String planId = PLAN_ID_PREFIX + System.currentTimeMillis();
		logger.debug("Generated new planId: {}", planId);
		return planId;
	}

	/**
	 * Generate an ID of the other type based on the existing ID If it is a planId,
	 * convert it to planTemplateId If it is a planTemplateId, convert it to planId
	 * @param id existing ID
	 * @return converted ID
	 */
	public String convertId(String id) {
		if (id == null) {
			return null;
		}

		if (isPlanId(id)) {
			return toPlanTemplateId(id);
		}
		else if (isPlanTemplateId(id)) {
			return toPlanId(id);
		}
		else {
			// Unable to determine the ID type, return the original ID
			logger.warn("Unable to determine the ID type [{}], return the original ID", id);
			return id;
		}
	}

	/**
	 * Generate a unique sub-plan ID based on parent plan ID. This method ensures the
	 * sub-plan ID is completely different from parent plan ID to prevent data corruption
	 * and mapping conflicts in the UI
	 * @param parentPlanId the parent plan ID
	 * @return unique sub-plan ID that is guaranteed to be different from parent plan ID
	 */
	public String generateSubPlanId(String parentPlanId) {
		if (parentPlanId == null) {
			throw new IllegalArgumentException("Parent plan ID cannot be null");
		}

		// Use a different prefix to ensure sub-plan ID is never identical to parent plan
		// ID
		String subPlanPrefix = "subplan-";

		// Generate unique sub-plan ID with multiple uniqueness factors:
		// 1. Different prefix ("subplan-" vs "plan-")
		// 2. Current timestamp in nanoseconds for high precision
		// 3. Random component for additional uniqueness
		// 4. Hash of parent plan ID to maintain some relationship while ensuring
		// uniqueness
		long timestamp = System.nanoTime();
		int randomComponent = (int) (Math.random() * 10000);
		int parentIdHash = Math.abs(parentPlanId.hashCode()) % 10000;

		String subPlanId = String.format("%s%d_%d_%d", subPlanPrefix, timestamp, randomComponent, parentIdHash);

		// Double-check that the generated sub-plan ID is different from parent plan ID
		if (subPlanId.equals(parentPlanId)) {
			// This should never happen given our generation logic, but add failsafe
			subPlanId = subPlanPrefix + "failsafe_" + timestamp + "_" + randomComponent;
			logger.warn("Failsafe sub-plan ID generation triggered for parent plan: {}", parentPlanId);
		}

		logger.info("Generated unique sub-plan ID: {} for parent plan: {}", subPlanId, parentPlanId);

		return subPlanId;
	}

	/**
	 * Generate a unique tool call ID for tracking tool executions
	 * @return unique tool call ID
	 */
	public String generateToolCallId() {
		// Use a specific prefix for tool call IDs
		String toolCallPrefix = "toolcall-";

		// Generate unique tool call ID with multiple uniqueness factors:
		// 1. Specific prefix for tool calls
		// 2. Current timestamp in milliseconds
		// 3. Random component for additional uniqueness
		// 4. Thread ID to handle concurrent tool calls
		long timestamp = System.currentTimeMillis();
		int randomComponent = (int) (Math.random() * 10000);
		long threadId = Thread.currentThread().getId();

		String toolCallId = String.format("%s%d_%d_%d", toolCallPrefix, timestamp, randomComponent, threadId);

		logger.debug("Generated unique tool call ID: {}", toolCallId);

		return toolCallId;
	}

	/**
	 * Generate a unique step ID for tracking execution steps
	 * @return unique step ID
	 */
	public String generateStepId() {
		// Use a specific prefix for step IDs
		String stepPrefix = "step-";

		// Generate unique step ID with multiple uniqueness factors:
		// 1. Specific prefix for steps
		// 2. Current timestamp in milliseconds
		// 3. Random component for additional uniqueness
		// 4. Thread ID to handle concurrent step executions
		long timestamp = System.currentTimeMillis();
		int randomComponent = (int) (Math.random() * 10000);
		long threadId = Thread.currentThread().getId();

		String stepId = String.format("%s%d_%d_%d", stepPrefix, timestamp, randomComponent, threadId);

		logger.debug("Generated unique step ID: {}", stepId);

		return stepId;
	}

	/**
	 * Generate a unique thinkAct ID for tracking think-act execution cycles
	 * @return unique thinkAct ID
	 */
	public String generateThinkActId() {
		// Use a specific prefix for thinkAct IDs
		String thinkActPrefix = "thinkact-";

		// Generate unique thinkAct ID with multiple uniqueness factors:
		// 1. Specific prefix for thinkAct operations
		// 2. Current timestamp in milliseconds
		// 3. Random component for additional uniqueness
		// 4. Thread ID to handle concurrent thinkAct executions
		long timestamp = System.currentTimeMillis();
		int randomComponent = (int) (Math.random() * 10000);
		long threadId = Thread.currentThread().getId();

		String thinkActId = String.format("%s%d_%d_%d", thinkActPrefix, timestamp, randomComponent, threadId);

		logger.debug("Generated unique thinkAct ID: {}", thinkActId);

		return thinkActId;
	}

}
