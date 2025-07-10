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
 * 步骤结果管理工具类
 *
 * @author zhangshenghang
 */
public class StepResultUtils {

	private static final String STEP_PREFIX = "step_";

	/**
	 * 添加步骤结果
	 * @param existingResults 现有结果集合
	 * @param stepNumber 步骤编号
	 * @param result 结果内容
	 * @return 更新后的结果集合
	 */
	public static Map<String, String> addStepResult(Map<String, String> existingResults, Integer stepNumber,
			String result) {
		Map<String, String> updatedResults = new HashMap<>(existingResults);
		updatedResults.put(STEP_PREFIX + stepNumber, result);
		return updatedResults;
	}

	/**
	 * 获取步骤结果
	 * @param results 结果集合
	 * @param stepNumber 步骤编号
	 * @return 步骤结果，如果不存在则返回null
	 */
	public static String getStepResult(Map<String, String> results, Integer stepNumber) {
		return results.get(STEP_PREFIX + stepNumber);
	}

	/**
	 * 检查步骤结果是否存在
	 * @param results 结果集合
	 * @param stepNumber 步骤编号
	 * @return 是否存在
	 */
	public static boolean hasStepResult(Map<String, String> results, Integer stepNumber) {
		return results.containsKey(STEP_PREFIX + stepNumber);
	}

	/**
	 * 清空特定步骤的结果
	 * @param results 结果集合
	 * @param stepNumber 步骤编号
	 * @return 结果集合
	 */
	public static Map<String, String> clearStepResult(Map<String, String> results, Integer stepNumber) {
		Map<String, String> updatedResults = new HashMap<>(results);
		updatedResults.remove(STEP_PREFIX + stepNumber);
		return updatedResults;
	}

}
