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

import com.alibaba.cloud.ai.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.manus.planning.model.po.PlanTemplateVersion;
import com.alibaba.cloud.ai.manus.planning.repository.PlanTemplateRepository;
import com.alibaba.cloud.ai.manus.planning.repository.PlanTemplateVersionRepository;
import com.alibaba.cloud.ai.manus.runtime.executor.factory.PlanExecutorFactory;
import com.alibaba.cloud.ai.manus.runtime.service.PlanIdDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plan template service class that provides business logic related to plan templates
 */
@Service
public class PlanTemplateService implements IPlanTemplateService {

	private static final Logger logger = LoggerFactory.getLogger(PlanTemplateService.class);

	@Autowired
	private PlanTemplateRepository planTemplateRepository;

	@Autowired
	private PlanTemplateVersionRepository versionRepository;

	@Autowired
	private PlanExecutorFactory planExecutorFactory;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired(required = false)
	private IPlanParameterMappingService parameterMappingService;

	/**
	 * Save plan template and its first version
	 * @param planTemplateId Plan template ID
	 * @param title Plan title
	 * @param userRequest User request
	 * @param planJson Plan JSON data
	 */
	@Transactional
	public void savePlanTemplate(String planTemplateId, String title, String userRequest, String planJson,
			boolean isInternalToolcall) {
		// Save basic plan template information
		PlanTemplate template = new PlanTemplate(planTemplateId, title, userRequest, isInternalToolcall);
		planTemplateRepository.save(template);

		// Save the first version
		saveToVersionHistory(planTemplateId, planJson);

		logger.info("Saved plan template {} and its first version", planTemplateId);
	}

	/**
	 * Update plan template information
	 * @param planTemplateId Plan template ID
	 * @param title Plan title
	 * @param planJson Plan JSON data
	 * @return Whether the update was successful
	 */
	@Transactional
	public boolean updatePlanTemplate(String planTemplateId, String title, String planJson,
			boolean isInternalToolcall) {
		Optional<PlanTemplate> templateOpt = planTemplateRepository.findByPlanTemplateId(planTemplateId);
		if (templateOpt.isPresent()) {
			PlanTemplate template = templateOpt.get();
			if (title != null && !title.isEmpty()) {
				template.setTitle(title);
			}
			template.setInternalToolcall(isInternalToolcall);
			template.setUpdateTime(LocalDateTime.now());
			planTemplateRepository.save(template);

			// Save new version
			saveToVersionHistory(planTemplateId, planJson);

			logger.info("Updated plan template {} and saved new version", planTemplateId);
			return true;
		}
		return false;
	}

	/**
	 * Version save result class
	 */
	public static class VersionSaveResult {

		private final boolean saved;

		private final boolean duplicate;

		private final String message;

		private final int versionIndex;

		public VersionSaveResult(boolean saved, boolean duplicate, String message, int versionIndex) {
			this.saved = saved;
			this.duplicate = duplicate;
			this.message = message;
			this.versionIndex = versionIndex;
		}

		public boolean isSaved() {
			return saved;
		}

		public boolean isDuplicate() {
			return duplicate;
		}

		public String getMessage() {
			return message;
		}

		public int getVersionIndex() {
			return versionIndex;
		}

	}

	/**
	 * Save plan version to history record
	 * @param planTemplateId Plan template ID
	 * @param planJson Plan JSON data
	 * @return Save result information
	 */
	@Transactional
	public VersionSaveResult saveToVersionHistory(String planTemplateId, String planJson) {
		// Check if content is the same as the latest version
		if (isContentSameAsLatestVersion(planTemplateId, planJson)) {
			logger.info("Content of plan {} is the same as latest version, skipping version save", planTemplateId);
			Integer maxVersionIndex = versionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
			return new VersionSaveResult(false, true, "Content same as latest version, no new version created",
					maxVersionIndex != null ? maxVersionIndex : -1);
		}

		// Get maximum version number
		Integer maxVersionIndex = versionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
		int newVersionIndex = (maxVersionIndex == null) ? 0 : maxVersionIndex + 1;

		// Save new version
		PlanTemplateVersion version = new PlanTemplateVersion(planTemplateId, newVersionIndex, planJson);
		versionRepository.save(version);

		logger.info("Saved version {} of plan {}", newVersionIndex, planTemplateId);
		return new VersionSaveResult(true, false, "New version saved", newVersionIndex);
	}

	/**
	 * Save plan version to history record (compatibility method)
	 * @param planTemplateId Plan template ID
	 * @param planJson Plan JSON data
	 */
	@Transactional
	public void saveVersionToHistory(String planTemplateId, String planJson) {
		saveToVersionHistory(planTemplateId, planJson);
	}

	/**
	 * Get plan template
	 * @param planTemplateId Plan template ID
	 * @return Plan template entity, returns null if not exists
	 */
	public PlanTemplate getPlanTemplate(String planTemplateId) {
		return planTemplateRepository.findByPlanTemplateId(planTemplateId).orElse(null);
	}

	/**
	 * Get all version JSON data of the plan
	 * @param planTemplateId Plan template ID
	 * @return List of version JSON data
	 */
	public List<String> getPlanVersions(String planTemplateId) {
		List<PlanTemplateVersion> versions = versionRepository
			.findByPlanTemplateIdOrderByVersionIndexAsc(planTemplateId);
		List<String> jsonVersions = new ArrayList<>();
		for (PlanTemplateVersion version : versions) {
			jsonVersions.add(version.getPlanJson());
		}
		return jsonVersions;
	}

	/**
	 * Get specified version of the plan
	 * @param planTemplateId Plan template ID
	 * @param versionIndex Version index
	 * @return Version JSON data, returns null if version does not exist
	 */
	public String getPlanVersion(String planTemplateId, int versionIndex) {
		PlanTemplateVersion version = versionRepository.findByPlanTemplateIdAndVersionIndex(planTemplateId,
				versionIndex);
		return version != null ? version.getPlanJson() : null;
	}

	/**
	 * Get the latest version of the plan
	 * @param planTemplateId Plan template ID
	 * @return Latest version JSON data, returns null if no version exists
	 */
	public String getLatestPlanVersion(String planTemplateId) {
		Integer maxVersionIndex = versionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
		if (maxVersionIndex == null) {
			return null;
		}
		return getPlanVersion(planTemplateId, maxVersionIndex);
	}

	/**
	 * Check if given content is the same as the latest version
	 * @param planTemplateId Plan template ID
	 * @param planJson Plan JSON data to check
	 * @return Returns true if content is the same, false otherwise
	 */
	public boolean isContentSameAsLatestVersion(String planTemplateId, String planJson) {
		String latestVersion = getLatestPlanVersion(planTemplateId);
		return isJsonContentEquivalent(latestVersion, planJson);
	}

	/**
	 * Intelligently compare if two JSON strings are semantically identical, ignoring
	 * format differences (spaces, line breaks, etc.), only comparing actual content
	 * @param json1 First JSON string
	 * @param json2 Second JSON string
	 * @return Returns true if semantically identical, false otherwise
	 */
	public boolean isJsonContentEquivalent(String json1, String json2) {
		if (json1 == null && json2 == null) {
			return true;
		}
		if (json1 == null || json2 == null) {
			return false;
		}

		// First try simple string comparison
		if (json1.equals(json2)) {
			return true;
		}

		try {
			JsonNode node1 = objectMapper.readTree(json1);
			JsonNode node2 = objectMapper.readTree(json2);
			return node1.equals(node2);
		}
		catch (Exception e) {
			logger.warn("Failed to parse JSON content during comparison, falling back to string comparison", e);
			// If JSON parsing fails, fall back to string comparison
			return json1.equals(json2);
		}
	}

	/**
	 * Extract title from ExecutionPlan object
	 * @param planJson Plan JSON string
	 * @return Plan title, returns default title if extraction fails
	 */
	public String extractTitleFromPlan(String planJson) {
		try {
			JsonNode rootNode = objectMapper.readTree(planJson);
			if (rootNode.has("title")) {
				return rootNode.get("title").asText("Untitled Plan");
			}
		}
		catch (Exception e) {
			logger.warn("Failed to extract title from plan JSON", e);
		}
		return "Untitled Plan";
	}

	/**
	 * Get all plan templates
	 * @return List of all plan templates
	 */
	public List<PlanTemplate> getAllPlanTemplates() {
		return planTemplateRepository.findAll();
	}

	/**
	 * Delete plan template
	 * @param planTemplateId Plan template ID
	 * @return Whether deletion was successful
	 */
	@Transactional
	public boolean deletePlanTemplate(String planTemplateId) {
		try {
			// First delete all related versions
			versionRepository.deleteByPlanTemplateId(planTemplateId);

			// Then delete the template itself
			planTemplateRepository.deleteByPlanTemplateId(planTemplateId);

			logger.info("Deleted plan template {} and all its versions", planTemplateId);
			return true;
		}
		catch (Exception e) {
			logger.error("Failed to delete plan template {}", planTemplateId, e);
			return false;
		}
	}

}
