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
package com.alibaba.cloud.ai.example.manus.tool.pptGenerator;

import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.ToolPromptManager;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PptGeneratorOperator extends AbstractBaseTool<PptInput> {

	private static final Logger log = LoggerFactory.getLogger(PptGeneratorOperator.class);

	private final PptGeneratorService pptGeneratorService;

	private final ObjectMapper objectMapper;

	private final ToolPromptManager toolPromptManager;

	private final UnifiedDirectoryManager unifiedDirectoryManager;

	private static final String TOOL_NAME = "ppt_generator_operator";

	public PptGeneratorOperator(PptGeneratorService pptGeneratorService, ObjectMapper objectMapper,
			ToolPromptManager toolPromptManager, UnifiedDirectoryManager unifiedDirectoryManager) {
		this.pptGeneratorService = pptGeneratorService;
		this.objectMapper = objectMapper;
		this.toolPromptManager = toolPromptManager;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
	}

	/**
	 * Run the tool (accepts JSON string input)
	 */
	@Override
	public ToolExecuteResult run(PptInput input) {
		log.info("PptGeneratorOperator input: action={}, title={}, path={}, fileName={}", input.getAction(),
				input.getTitle(), input.getPath(), input.getFileName());
		try {
			String planId = this.currentPlanId;

			if ("getTemplateList".equals(input.getAction())) {
				// Handle get template list operation
				String templateList = pptGeneratorService.getTemplateList();
				if (templateList == null || templateList.isEmpty()) {
					return new ToolExecuteResult(
							"No local templates, please check the folder extensions/pptGenerator/template available");
				}
				return new ToolExecuteResult(templateList);
			}
			else if ("getTemplate".equals(input.getAction())) {
				// Handle get template operation
				String templateContent = pptGeneratorService.getTemplate(input.getPath());
				return new ToolExecuteResult(templateContent);
			}
			else if (!"create".equals(input.getAction())) {
				return new ToolExecuteResult("Unsupported operations: " + input.getAction()
						+ ", Only supports the 'create', 'getTemplateList' and 'getTemplate' operations");
			}

			// Update the file state to processing.
			pptGeneratorService.updateFileState(planId, null, "Processing: Generating PPT file");

			String path = pptGeneratorService.createPpt(input);

			// Update the file state to success.
			pptGeneratorService.updateFileState(planId, path, "Success: PPT file generated successfully");

			return new ToolExecuteResult("PPT file generated successfully, save path: " + path);
		}
		catch (IllegalArgumentException e) {
			String planId = this.currentPlanId;
			pptGeneratorService.updateFileState(planId, null, "Error: Parameter validation failed: " + e.getMessage());
			return new ToolExecuteResult("Parameter validation failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("PPT generation failed", e);
			String planId = this.currentPlanId;
			pptGeneratorService.updateFileState(planId, null, "Error: PPT generation failed: " + e.getMessage());
			return new ToolExecuteResult("PPT generation failed: " + e.getMessage());
		}
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return toolPromptManager.getToolDescription("pptGeneratorOperator");
	}

	@Override
	public String getParameters() {
		return toolPromptManager.getToolParameters("pptGeneratorOperator");
	}

	@Override
	public Class<PptInput> getInputType() {
		return PptInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	@Override
	public void cleanup(String planId) {
		// Clean up file state.
		pptGeneratorService.cleanupForPlan(planId);
		log.info("Cleaning up PPT generator resources for plan: {}", planId);
	}

	@Override
	public String getCurrentToolStateString() {
		String planId = this.currentPlanId;
		if (planId != null) {
			return String.format("PPT Generator - Current File: %s, Last Operation: %s",
					pptGeneratorService.getCurrentFilePath(planId), pptGeneratorService.getLastOperationResult(planId));
		}
		return "PPT Generator is ready";
	}

}
