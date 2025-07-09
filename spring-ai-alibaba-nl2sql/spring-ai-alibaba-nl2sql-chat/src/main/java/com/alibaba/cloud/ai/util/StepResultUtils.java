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
 * 步骤结果工具类
 *
 * @author zhangshenghang
 */
public class StepResultUtils {

	private static final String STEP_RESULT_KEY_TEMPLATE = "步骤%d结果";

	/**
	 * 生成步骤结果键
	 *
	 * @param stepNumber 步骤编号
	 * @return 步骤结果键
	 */
	public static String generateStepResultKey(int stepNumber) {
		return String.format(STEP_RESULT_KEY_TEMPLATE, stepNumber);
	}

	/**
	 * 添加步骤结果到现有的结果Map中
	 *
	 * @param existingResults 现有结果Map
	 * @param stepNumber 步骤编号
	 * @param result 步骤结果
	 * @return 更新后的结果Map
	 */
	public static Map<String, String> addStepResult(Map<String, String> existingResults, int stepNumber, String result) {
		Map<String, String> updatedResults = new HashMap<>(existingResults);
		updatedResults.put(generateStepResultKey(stepNumber), result);
		return updatedResults;
	}

	/**
	 * 创建包含步骤结果的新Map
	 *
	 * @param stepNumber 步骤编号
	 * @param result 步骤结果
	 * @return 包含步骤结果的新Map
	 */
	public static Map<String, String> createStepResultMap(int stepNumber, String result) {
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put(generateStepResultKey(stepNumber), result);
		return resultMap;
	}
}
