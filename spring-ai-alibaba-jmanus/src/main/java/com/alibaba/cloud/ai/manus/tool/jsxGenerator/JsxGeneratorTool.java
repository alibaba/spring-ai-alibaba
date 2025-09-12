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
package com.alibaba.cloud.ai.manus.tool.jsxGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsxGeneratorTool extends AbstractBaseTool<JsxGeneratorTool.JsxInput> {

	private static final Logger log = LoggerFactory.getLogger(JsxGeneratorTool.class);

	private static final String TOOL_NAME = "vue_component_generator";

	private static final String TOOL_DESCRIPTION = "Tool for generating Vue SFC components with Handlebars templates and Sandpack preview";

	private static final String PARAMETERS = "{\"type\":\"object\",\"properties\":{\"action\":{\"type\":\"string\",\"description\":\"Action to perform: generate_vue, apply_template, save, update, preview, validate\"},\"component_type\":{\"type\":\"string\",\"description\":\"Type of component to generate (e.g., button, form, chart)\"},\"component_data\":{\"type\":\"object\",\"description\":\"Component data including name, data, methods, computed, template, style\"},\"template_name\":{\"type\":\"string\",\"description\":\"Handlebars template name (e.g., counter-button, data-form)\"},\"template_data\":{\"type\":\"object\",\"description\":\"Data to apply to Handlebars template\"},\"file_path\":{\"type\":\"string\",\"description\":\"File path to save the Vue SFC code\"},\"vue_sfc_code\":{\"type\":\"string\",\"description\":\"Vue SFC code to save or validate\"},\"section_type\":{\"type\":\"string\",\"description\":\"Section type for update operation (template, script, style)\"},\"new_content\":{\"type\":\"string\",\"description\":\"New content for update operation\"},\"dependencies\":{\"type\":\"object\",\"description\":\"Additional dependencies for Sandpack preview\"}},\"required\":[\"action\"]}";

	private final IJsxGeneratorService vueGeneratorService;

	/**
	 * Internal input class for defining input parameters of Vue component generator tool
	 */
	public static class JsxInput {

		private String action;

		@JsonProperty("component_type")
		private String componentType;

		@JsonProperty("component_data")
		private Map<String, Object> componentData;

		@JsonProperty("template_name")
		private String templateName;

		@JsonProperty("template_data")
		private Map<String, Object> templateData;

		@JsonProperty("file_path")
		private String filePath;

		@JsonProperty("vue_sfc_code")
		private String vueSfcCode;

		@JsonProperty("section_type")
		private String sectionType;

		@JsonProperty("new_content")
		private String newContent;

		private Map<String, String> dependencies;

		// Getters and setters
		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getComponentType() {
			return componentType;
		}

		public void setComponentType(String componentType) {
			this.componentType = componentType;
		}

		public Map<String, Object> getComponentData() {
			return componentData;
		}

		public void setComponentData(Map<String, Object> componentData) {
			this.componentData = componentData;
		}

		public String getTemplateName() {
			return templateName;
		}

		public void setTemplateName(String templateName) {
			this.templateName = templateName;
		}

		public Map<String, Object> getTemplateData() {
			return templateData;
		}

		public void setTemplateData(Map<String, Object> templateData) {
			this.templateData = templateData;
		}

		public String getFilePath() {
			return filePath;
		}

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

		public String getVueSfcCode() {
			return vueSfcCode;
		}

		public void setVueSfcCode(String vueSfcCode) {
			this.vueSfcCode = vueSfcCode;
		}

		public String getSectionType() {
			return sectionType;
		}

		public void setSectionType(String sectionType) {
			this.sectionType = sectionType;
		}

		public String getNewContent() {
			return newContent;
		}

		public void setNewContent(String newContent) {
			this.newContent = newContent;
		}

		public Map<String, String> getDependencies() {
			return dependencies;
		}

		public void setDependencies(Map<String, String> dependencies) {
			this.dependencies = dependencies;
		}

		@Override
		public String toString() {
			return "JsxInput{" + "action='" + action + '\'' + ", componentType='" + componentType + '\''
					+ ", componentData=" + componentData + ", templateName='" + templateName + '\'' + ", templateData="
					+ templateData + ", filePath='" + filePath + '\'' + ", vueSfcCode='"
					+ (vueSfcCode != null ? vueSfcCode.substring(0, Math.min(100, vueSfcCode.length())) + "..." : null)
					+ '\'' + ", sectionType='" + sectionType + '\'' + ", newContent='" + newContent + '\''
					+ ", dependencies=" + dependencies + '}';
		}

	}

	public JsxGeneratorTool(IJsxGeneratorService vueGeneratorService) {
		this.vueGeneratorService = vueGeneratorService;
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return TOOL_DESCRIPTION;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<JsxInput> getInputType() {
		return JsxInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "frontend-tools";
	}

	@Override
	public String getCurrentToolStateString() {
		return "Vue Component Generator Tool is ready";
	}

	@Override
	public void cleanup(String planId) {
		// Cleanup logic if needed
	}

	/**
	 * Helper method to create detailed error messages
	 */
	private String createDetailedErrorMessage(String action, String missingParam, JsxInput input) {
		StringBuilder sb = new StringBuilder();
		sb.append("Error: ")
			.append(missingParam)
			.append(" parameter is required for ")
			.append(action)
			.append(" operation.\n");
		sb.append("Received parameters: ").append(input.toString()).append("\n");
		sb.append("Expected format for ").append(action).append(":\n");

		switch (action) {
			case "generate_vue":
				sb.append("{\n");
				sb.append("  \"action\": \"generate_vue\",\n");
				sb.append("  \"component_type\": \"<string>\",  // Required: button, form, chart, counter, etc.\n");
				sb.append("  \"component_data\": {             // Optional: component specifications\n");
				sb.append("    \"name\": \"<string>\",\n");
				sb.append("    \"data\": {},\n");
				sb.append("    \"methods\": {},\n");
				sb.append("    \"template\": \"<string>\",\n");
				sb.append("    \"style\": \"<string>\"\n");
				sb.append("  }\n");
				sb.append("}");
				break;
			case "apply_template":
				sb.append("{\n");
				sb.append("  \"action\": \"apply_template\",\n");
				sb.append("  \"template_name\": \"<string>\",   // Required: template name\n");
				sb.append("  \"template_data\": {}             // Optional: data for template\n");
				sb.append("}");
				break;
			case "save":
				sb.append("{\n");
				sb.append("  \"action\": \"save\",\n");
				sb.append("  \"file_path\": \"<string>\",       // Required: path to save file\n");
				sb.append("  \"vue_sfc_code\": \"<string>\"    // Required: Vue SFC code\n");
				sb.append("}");
				break;
			case "update":
				sb.append("{\n");
				sb.append("  \"action\": \"update\",\n");
				sb.append("  \"file_path\": \"<string>\",       // Required: path to file\n");
				sb.append("  \"section_type\": \"<string>\",    // Required: template, script, or style\n");
				sb.append("  \"new_content\": \"<string>\"     // Required: new content\n");
				sb.append("}");
				break;
			case "preview":
				sb.append("{\n");
				sb.append("  \"action\": \"preview\",\n");
				sb.append("  \"file_path\": \"<string>\",       // Required: path to Vue file\n");
				sb.append("  \"dependencies\": {}             // Optional: additional dependencies\n");
				sb.append("}");
				break;
			case "validate":
				sb.append("{\n");
				sb.append("  \"action\": \"validate\",\n");
				sb.append("  \"vue_sfc_code\": \"<string>\"    // Required: Vue SFC code to validate\n");
				sb.append("}");
				break;
		}

		return sb.toString();
	}

	/**
	 * Execute Vue component generation operations with strongly typed input object
	 */
	@Override
	public ToolExecuteResult run(JsxInput input) {
		log.info("VueGeneratorTool input: {}", input);
		try {
			String planId = this.currentPlanId;
			String action = input.getAction();

			// Basic parameter validation
			if (action == null) {
				return new ToolExecuteResult(createDetailedErrorMessage("unknown", "action", input));
			}

			return switch (action) {
				case "generate_vue" -> {
					String componentType = input.getComponentType();
					Map<String, Object> componentData = input.getComponentData();

					if (componentType == null || componentType.trim().isEmpty()) {
						yield new ToolExecuteResult(
								createDetailedErrorMessage("generate_vue", "component_type", input));
					}
					if (componentData == null) {
						componentData = new HashMap<>();
						log.info("No component_data provided, using empty map for component generation");
					}

					log.info("Generating Vue SFC with componentType: {}, componentData keys: {}", componentType,
							componentData.keySet());
					String vueSfcCode = vueGeneratorService.generateVueSFC(componentType, componentData);
					yield new ToolExecuteResult("Successfully generated Vue SFC code for component type '"
							+ componentType + "':\n" + vueSfcCode);
				}
				case "apply_template" -> {
					String templateName = input.getTemplateName();
					Map<String, Object> templateData = input.getTemplateData();

					if (templateName == null || templateName.trim().isEmpty()) {
						yield new ToolExecuteResult(
								createDetailedErrorMessage("apply_template", "template_name", input));
					}
					if (templateData == null) {
						templateData = new HashMap<>();
						log.info("No template_data provided, using empty map for template application");
					}

					log.info("Applying template: {}, with data keys: {}", templateName, templateData.keySet());
					String generatedCode = vueGeneratorService.applyHandlebarsTemplate(templateName, templateData);
					yield new ToolExecuteResult(
							"Successfully applied template '" + templateName + "':\n" + generatedCode);
				}
				case "save" -> {
					String filePath = input.getFilePath();
					String vueSfcCode = input.getVueSfcCode();

					if (filePath == null || filePath.trim().isEmpty()) {
						yield new ToolExecuteResult(createDetailedErrorMessage("save", "file_path", input));
					}
					if (vueSfcCode == null || vueSfcCode.trim().isEmpty()) {
						yield new ToolExecuteResult(createDetailedErrorMessage("save", "vue_sfc_code", input));
					}

					log.info("Saving Vue SFC to file: {}, code length: {}", filePath, vueSfcCode.length());
					String savedPath = vueGeneratorService.saveVueSfcToFile(planId, filePath, vueSfcCode);
					yield new ToolExecuteResult("Successfully saved Vue SFC to file: " + savedPath);
				}
				case "update" -> {
					String filePath = input.getFilePath();
					String sectionType = input.getSectionType();
					String newContent = input.getNewContent();

					if (filePath == null || filePath.trim().isEmpty()) {
						yield new ToolExecuteResult(createDetailedErrorMessage("update", "file_path", input));
					}
					if (sectionType == null || sectionType.trim().isEmpty()) {
						yield new ToolExecuteResult(createDetailedErrorMessage("update", "section_type", input));
					}
					if (newContent == null) {
						yield new ToolExecuteResult(createDetailedErrorMessage("update", "new_content", input));
					}

					log.info("Updating Vue component: {}, section: {}, new content length: {}", filePath, sectionType,
							newContent.length());
					vueGeneratorService.updateVueComponent(planId, filePath, sectionType, newContent);
					yield new ToolExecuteResult("Successfully updated " + sectionType + " section in: " + filePath);
				}
				case "preview" -> {
					String filePath = input.getFilePath();
					Map<String, String> dependencies = input.getDependencies();

					if (filePath == null || filePath.trim().isEmpty()) {
						yield new ToolExecuteResult(createDetailedErrorMessage("preview", "file_path", input));
					}

					log.info("Generating preview for Vue component: {}, dependencies: {}", filePath, dependencies);
					String sandpackConfig = vueGeneratorService.generateSandpackConfig(planId, filePath, dependencies);
					String previewUrl = vueGeneratorService.generatePreviewUrl(planId, filePath);
					yield new ToolExecuteResult("Successfully generated Sandpack preview:\nURL: " + previewUrl
							+ "\nConfig: " + sandpackConfig);
				}
				case "validate" -> {
					String vueSfcCode = input.getVueSfcCode();

					if (vueSfcCode == null || vueSfcCode.trim().isEmpty()) {
						yield new ToolExecuteResult(createDetailedErrorMessage("validate", "vue_sfc_code", input));
					}

					log.info("Validating Vue SFC code, length: {}", vueSfcCode.length());
					String validationResult = vueGeneratorService.validateVueSfc(vueSfcCode);
					yield new ToolExecuteResult("Validation result: " + validationResult);
				}
				case "list_templates" -> {
					Set<String> templates = vueGeneratorService.getAvailableTemplates();
					yield new ToolExecuteResult("Available templates: " + String.join(", ", templates));
				}
				default -> new ToolExecuteResult("Unknown operation: " + action
						+ ". Supported operations: generate_vue, apply_template, save, update, preview, validate, list_templates\n"
						+ "Received input: " + input);
			};

		}
		catch (IOException e) {
			log.error("VueGeneratorTool execution failed", e);
			return new ToolExecuteResult("Tool execution failed: " + e.getMessage() + "\nInput was: " + input);
		}
		catch (Exception e) {
			log.error("Unexpected error in VueGeneratorTool", e);
			return new ToolExecuteResult("Unexpected error: " + e.getMessage() + "\nInput was: " + input);
		}
	}

}
