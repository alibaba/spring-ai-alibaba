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
package com.alibaba.cloud.ai.manus.planning.service;

import java.util.List;

import com.alibaba.cloud.ai.manus.planning.model.po.PlanTemplate;

/**
 * Plan template service interface providing business logic related to plan templates
 */
public interface IPlanTemplateService {

	/**
	 * Save plan template
	 * @param planTemplateId Template ID
	 * @param title Title
	 * @param userRequest User request
	 * @param planJson Plan JSON
	 * @param isInternalToolcall Whether is internal toolcall
	 */
	void savePlanTemplate(String planTemplateId, String title, String userRequest, String planJson,
			boolean isInternalToolcall);

	/**
	 * Update plan template
	 * @param planTemplateId Template ID
	 * @param title Title
	 * @param planJson Plan JSON
	 * @return Whether update was successful
	 */
	boolean updatePlanTemplate(String planTemplateId, String title, String planJson, boolean isInternalToolcall);

	/**
	 * Save to version history
	 * @param planTemplateId Template ID
	 * @param planJson Plan JSON
	 * @return Version save result
	 */
	PlanTemplateService.VersionSaveResult saveToVersionHistory(String planTemplateId, String planJson);

	/**
	 * Save version to history
	 * @param planTemplateId Template ID
	 * @param planJson Plan JSON
	 */
	void saveVersionToHistory(String planTemplateId, String planJson);

	/**
	 * Get plan template
	 * @param planTemplateId Template ID
	 * @return Plan template
	 */
	PlanTemplate getPlanTemplate(String planTemplateId);

	/**
	 * Get plan version list
	 * @param planTemplateId Template ID
	 * @return Version list
	 */
	List<String> getPlanVersions(String planTemplateId);

	/**
	 * Get plan of specified version
	 * @param planTemplateId Template ID
	 * @param versionIndex Version index
	 * @return Plan JSON
	 */
	String getPlanVersion(String planTemplateId, int versionIndex);

	/**
	 * Get latest version plan
	 * @param planTemplateId Template ID
	 * @return Plan JSON
	 */
	String getLatestPlanVersion(String planTemplateId);

	/**
	 * Check if content is same as latest version
	 * @param planTemplateId Template ID
	 * @param planJson Plan JSON
	 * @return Whether same
	 */
	boolean isContentSameAsLatestVersion(String planTemplateId, String planJson);

	/**
	 * Check if JSON content is equivalent
	 * @param json1 JSON1
	 * @param json2 JSON2
	 * @return Whether equivalent
	 */
	boolean isJsonContentEquivalent(String json1, String json2);

	/**
	 * Extract title from plan
	 * @param planJson Plan JSON
	 * @return Title
	 */
	String extractTitleFromPlan(String planJson);

	/**
	 * Get all plan templates
	 * @return Plan template list
	 */
	List<PlanTemplate> getAllPlanTemplates();

	/**
	 * Delete plan template
	 * @param planTemplateId Template ID
	 * @return Whether deletion was successful
	 */
	boolean deletePlanTemplate(String planTemplateId);

}
