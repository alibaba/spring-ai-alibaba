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
