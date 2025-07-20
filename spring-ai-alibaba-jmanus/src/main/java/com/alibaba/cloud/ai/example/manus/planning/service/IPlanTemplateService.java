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
package com.alibaba.cloud.ai.example.manus.planning.service;

import java.util.List;

import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;

/**
 * 计划模板服务接口，提供计划模板相关的业务逻辑
 */
public interface IPlanTemplateService {

	/**
	 * 保存计划模板
	 * @param planTemplateId 模板ID
	 * @param title 标题
	 * @param userRequest 用户请求
	 * @param planJson 计划JSON
	 */
	void savePlanTemplate(String planTemplateId, String title, String userRequest, String planJson);

	/**
	 * 更新计划模板
	 * @param planTemplateId 模板ID
	 * @param title 标题
	 * @param planJson 计划JSON
	 * @return 是否更新成功
	 */
	boolean updatePlanTemplate(String planTemplateId, String title, String planJson);

	/**
	 * 保存到版本历史
	 * @param planTemplateId 模板ID
	 * @param planJson 计划JSON
	 * @return 版本保存结果
	 */
	PlanTemplateService.VersionSaveResult saveToVersionHistory(String planTemplateId, String planJson);

	/**
	 * 保存版本到历史
	 * @param planTemplateId 模板ID
	 * @param planJson 计划JSON
	 */
	void saveVersionToHistory(String planTemplateId, String planJson);

	/**
	 * 获取计划模板
	 * @param planTemplateId 模板ID
	 * @return 计划模板
	 */
	PlanTemplate getPlanTemplate(String planTemplateId);

	/**
	 * 获取计划版本列表
	 * @param planTemplateId 模板ID
	 * @return 版本列表
	 */
	List<String> getPlanVersions(String planTemplateId);

	/**
	 * 获取指定版本的计划
	 * @param planTemplateId 模板ID
	 * @param versionIndex 版本索引
	 * @return 计划JSON
	 */
	String getPlanVersion(String planTemplateId, int versionIndex);

	/**
	 * 获取最新版本的计划
	 * @param planTemplateId 模板ID
	 * @return 计划JSON
	 */
	String getLatestPlanVersion(String planTemplateId);

	/**
	 * 检查内容是否与最新版本相同
	 * @param planTemplateId 模板ID
	 * @param planJson 计划JSON
	 * @return 是否相同
	 */
	boolean isContentSameAsLatestVersion(String planTemplateId, String planJson);

	/**
	 * 检查JSON内容是否等价
	 * @param json1 JSON1
	 * @param json2 JSON2
	 * @return 是否等价
	 */
	boolean isJsonContentEquivalent(String json1, String json2);

	/**
	 * 从计划中提取标题
	 * @param planJson 计划JSON
	 * @return 标题
	 */
	String extractTitleFromPlan(String planJson);

	/**
	 * 获取所有计划模板
	 * @return 计划模板列表
	 */
	List<PlanTemplate> getAllPlanTemplates();

	/**
	 * 删除计划模板
	 * @param planTemplateId 模板ID
	 * @return 是否删除成功
	 */
	boolean deletePlanTemplate(String planTemplateId);

}
