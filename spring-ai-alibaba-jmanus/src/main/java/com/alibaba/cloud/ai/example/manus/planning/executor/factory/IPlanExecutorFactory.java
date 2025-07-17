package com.alibaba.cloud.ai.example.manus.planning.executor.factory;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;

/**
 * Interface for plan executor factory that creates executors for different plan types
 */
public interface IPlanExecutorFactory {

	/**
	 * Create executor for the given plan
	 */
	PlanExecutorInterface createExecutor(PlanInterface plan);

	/**
	 * Get all supported plan types
	 */
	String[] getSupportedPlanTypes();

	/**
	 * Check if a plan type is supported
	 */
	boolean isPlanTypeSupported(String planType);

	/**
	 * Create executor by plan type and ID
	 */
	PlanExecutorInterface createExecutorByType(String planType, String planId);

}
