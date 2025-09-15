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
package com.alibaba.cloud.ai.example.manus.planning.coordinator;

/**
 * Interface for plan ID dispatcher that manages plan and template ID conversions
 */
public interface IPlanIdDispatcher {

	/**
	 * Check if ID is a plan template ID
	 */
	boolean isPlanTemplateId(String id);

	/**
	 * Check if ID is a plan ID
	 */
	boolean isPlanId(String id);

	/**
	 * Convert plan template ID to plan ID
	 */
	String toPlanId(String planTemplateId);

	/**
	 * Convert plan ID to plan template ID
	 */
	String toPlanTemplateId(String planId);

	/**
	 * Generate new plan template ID
	 */
	String generatePlanTemplateId();

	/**
	 * Generate new plan ID
	 */
	String generatePlanId();

	/**
	 * Convert between ID types
	 */
	String convertId(String id);

	/**
	 * Generate sub-plan ID
	 */
	String generateSubPlanId(String parentPlanId, Long thinkActRecordId);

}
