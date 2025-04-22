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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ID转换器，用于处理和转换planId与planTemplateId之间的关系
 * 这个类帮助系统在新旧接口之间进行兼容，允许同时支持使用planId和planTemplateId
 */
@Component
public class PlanIdDispatcher {

	private static final Logger logger = LoggerFactory.getLogger(PlanIdDispatcher.class);

	// planId前缀常量
	private static final String PLAN_ID_PREFIX = "plan-";

	// planTemplateId前缀常量
	private static final String PLAN_TEMPLATE_ID_PREFIX = "planTemplate-";

	/**
	 * 检查ID是否为planTemplateId格式
	 * @param id 待检查的ID
	 * @return 如果是planTemplateId格式则返回true，否则返回false
	 */
	public boolean isPlanTemplateId(String id) {
		return id != null && id.startsWith(PLAN_TEMPLATE_ID_PREFIX);
	}

	/**
	 * 检查ID是否为planId格式
	 * @param id 待检查的ID
	 * @return 如果是planId格式则返回true，否则返回false
	 */
	public boolean isPlanId(String id) {
		return id != null && id.startsWith(PLAN_ID_PREFIX);
	}

	/**
	 * 将planTemplateId转换为planId 由于一个planTemplate可以生成多个plan，所以每次生成唯一的planId
	 * @param planTemplateId 计划模板ID
	 * @return 转换后的唯一planId
	 */
	public String toPlanId(String planTemplateId) {
		if (planTemplateId == null) {
			return null;
		}

		if (isPlanId(planTemplateId)) {
			return planTemplateId; // 已经是planId格式，直接返回
		}

		// 无论是否为planTemplateId格式，都生成新的唯一planId
		// 使用时间戳和随机数确保唯一性
		String uniqueId = PLAN_ID_PREFIX + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);

		if (isPlanTemplateId(planTemplateId)) {
			logger.debug("从planTemplateId [{}] 生成新的唯一planId [{}]", planTemplateId, uniqueId);
		}
		else {
			logger.warn("未知ID格式 [{}]，生成新的唯一planId [{}]", planTemplateId, uniqueId);
		}

		return uniqueId;
	}

	/**
	 * 将planId转换为planTemplateId 如果输入已经是planTemplateId格式则直接返回
	 * @param planId 计划ID
	 * @return 转换后的planTemplateId
	 */
	public String toPlanTemplateId(String planId) {
		if (planId == null) {
			return null;
		}

		if (isPlanTemplateId(planId)) {
			return planId; // 已经是planTemplateId格式，直接返回
		}

		if (isPlanId(planId)) {
			// 提取ID的数字部分并创建新的planTemplateId
			String numericPart = planId.substring(PLAN_ID_PREFIX.length());
			String planTemplateId = PLAN_TEMPLATE_ID_PREFIX + numericPart;
			logger.debug("转换planId [{}] 到 planTemplateId [{}]", planId, planTemplateId);
			return planTemplateId;
		}

		// 对于不符合任何已知格式的ID，添加planTemplateId前缀
		logger.warn("未知ID格式 [{}]，添加planTemplateId前缀", planId);
		return PLAN_TEMPLATE_ID_PREFIX + planId;
	}

	/**
	 * 生成新的planTemplateId
	 * @return 新生成的planTemplateId
	 */
	public String generatePlanTemplateId() {
		String planTemplateId = PLAN_TEMPLATE_ID_PREFIX + System.currentTimeMillis();
		logger.debug("生成新的planTemplateId: {}", planTemplateId);
		return planTemplateId;
	}

	/**
	 * 生成新的planId
	 * @return 新生成的planId
	 */
	public String generatePlanId() {
		String planId = PLAN_ID_PREFIX + System.currentTimeMillis();
		logger.debug("生成新的planId: {}", planId);
		return planId;
	}

	/**
	 * 根据现有ID生成对应的另一种类型ID 如果是planId，则转换为planTemplateId 如果是planTemplateId，则转换为planId
	 * @param id 现有ID
	 * @return 转换后的ID
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
			// 无法确定ID类型，返回原ID
			logger.warn("无法确定ID类型 [{}]，返回原ID", id);
			return id;
		}
	}

}
