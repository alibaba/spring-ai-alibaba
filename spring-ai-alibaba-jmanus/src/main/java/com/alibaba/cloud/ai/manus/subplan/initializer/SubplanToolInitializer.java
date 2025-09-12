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
package com.alibaba.cloud.ai.manus.subplan.initializer;

import com.alibaba.cloud.ai.manus.subplan.model.po.SubplanToolDef;
import com.alibaba.cloud.ai.manus.subplan.predefineTools.PredefinedSubplanTools;
import com.alibaba.cloud.ai.manus.subplan.service.ISubplanToolService;
import com.alibaba.cloud.ai.manus.subplan.templates.SubplanPlanTemplates;
import com.alibaba.cloud.ai.manus.planning.service.PlanTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Initializer for subplan tools and plan templates
 *
 * Automatically creates predefined tools and plan templates when the application starts
 */
@Component
public class SubplanToolInitializer {

	private static final Logger logger = LoggerFactory.getLogger(SubplanToolInitializer.class);

	@Autowired
	private ISubplanToolService subplanToolService;

	@Autowired
	private PlanTemplateService planTemplateService;

	/**
	 * Initialize subplan tools and plan templates when application is ready
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void initializeSubplanTools() {
		logger.info("Starting subplan tools initialization...");

		try {
			// Initialize plan templates first
			initializePlanTemplates();

			// Initialize predefined tools
			initializePredefinedTools();

			logger.info("Subplan tools initialization completed successfully");
		}
		catch (Exception e) {
			logger.error("Failed to initialize subplan tools", e);
		}
	}

	/**
	 * Initialize plan templates for subplan tools
	 */
	private void initializePlanTemplates() {
		logger.info("Initializing plan templates...");

		// Get all predefined plan templates
		Map<String, String> templates = SubplanPlanTemplates.getAllPlanTemplates();

		for (Map.Entry<String, String> entry : templates.entrySet()) {
			String templateId = entry.getKey();
			String templateContent = entry.getValue();

			if (planTemplateService.getPlanTemplate(templateId) == null) {
				String title = extractTitleFromTemplate(templateContent);
				planTemplateService.savePlanTemplate(templateId, title, title, templateContent, true);
				logger.info("Created plan template: {} with title: {}", templateId, title);
			}
			else {
				// Check if the template content has changed before updating
				String latestVersion = planTemplateService.getLatestPlanVersion(templateId);
				if (latestVersion == null
						|| !planTemplateService.isJsonContentEquivalent(latestVersion, templateContent)) {
					String title = extractTitleFromTemplate(templateContent);
					boolean updated = planTemplateService.updatePlanTemplate(templateId, title, templateContent, true);
					if (updated) {
						logger.info("Updated plan template: {} with title: {} (content changed)", templateId, title);
					}
					else {
						logger.warn("Failed to update plan template: {}", templateId);
					}
				}
				else {
					logger.debug("Plan template content unchanged, skipping update: {}", templateId);
				}
			}
		}
	}

	/**
	 * Initialize predefined subplan tools
	 */
	private void initializePredefinedTools() {
		logger.info("Initializing predefined subplan tools...");

		// Get all predefined tools
		List<SubplanToolDef> predefinedTools = PredefinedSubplanTools.getAllPredefinedSubplanTools();

		for (SubplanToolDef tool : predefinedTools) {
			if (subplanToolService.existsByToolName(tool.getToolName())) {
				logger.debug("Tool already exists: {}", tool.getToolName());
				continue;
			}

			try {
				// Register the tool
				SubplanToolDef registeredTool = subplanToolService.registerSubplanTool(tool);
				logger.info("Successfully registered tool: {} with ID: {}", registeredTool.getToolName(),
						registeredTool.getId());

			}
			catch (Exception e) {
				logger.error("Failed to initialize tool: {}", tool.getToolName(), e);
			}
		}
	}

	/**
	 * Extract title from plan template JSON
	 * @param templateContent Plan template JSON content
	 * @return Extracted title or default title
	 */
	private String extractTitleFromTemplate(String templateContent) {
		try {
			// Simple JSON parsing to extract title
			if (templateContent.contains("\"title\"")) {
				int titleStart = templateContent.indexOf("\"title\"") + 8;
				int titleEnd = templateContent.indexOf("\"", titleStart);
				if (titleEnd > titleStart) {
					return templateContent.substring(titleStart, titleEnd);
				}
			}
		}
		catch (Exception e) {
			logger.warn("Failed to extract title from template, using default", e);
		}
		return "Untitled Plan Template";
	}

}
