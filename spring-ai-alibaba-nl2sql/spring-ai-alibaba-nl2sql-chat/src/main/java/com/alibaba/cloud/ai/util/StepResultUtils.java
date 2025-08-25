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

package com.alibaba.cloud.ai.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Step result management utility class
 *
 * @author zhangshenghang
 */
public class StepResultUtils {

	private static final String STEP_PREFIX = "step_";

	/**
	 * Add step result
	 * @param existingResults existing result collection
	 * @param stepNumber step number
	 * @param result result content
	 * @return updated result collection
	 */
	public static Map<String, String> addStepResult(Map<String, String> existingResults, Integer stepNumber,
			String result) {
		Map<String, String> updatedResults = new HashMap<>(existingResults);
		updatedResults.put(STEP_PREFIX + stepNumber, result);
		return updatedResults;
	}

	/**
	 * Get step result
	 * @param results result collection
	 * @param stepNumber step number
	 * @return step result, returns null if not exists
	 */
	public static String getStepResult(Map<String, String> results, Integer stepNumber) {
		return results.get(STEP_PREFIX + stepNumber);
	}

	/**
	 * Check if step result exists
	 * @param results result collection
	 * @param stepNumber step number
	 * @return whether exists
	 */
	public static boolean hasStepResult(Map<String, String> results, Integer stepNumber) {
		return results.containsKey(STEP_PREFIX + stepNumber);
	}

	/**
	 * Clear specific step result
	 * @param results result collection
	 * @param stepNumber step number
	 * @return result collection
	 */
	public static Map<String, String> clearStepResult(Map<String, String> results, Integer stepNumber) {
		Map<String, String> updatedResults = new HashMap<>(results);
		updatedResults.remove(STEP_PREFIX + stepNumber);
		return updatedResults;
	}

}
