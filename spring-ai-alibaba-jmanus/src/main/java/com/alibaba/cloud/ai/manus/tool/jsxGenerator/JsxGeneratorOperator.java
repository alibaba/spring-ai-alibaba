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

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsxGeneratorOperator extends AbstractBaseTool<JsxGeneratorTool.JsxInput> {

	private static final Logger log = LoggerFactory.getLogger(JsxGeneratorOperator.class);

	private final IJsxGeneratorService jsxGeneratorService;

	private final ObjectMapper objectMapper;

	private final UnifiedDirectoryManager unifiedDirectoryManager;

	private static final String TOOL_NAME = "jsx_generator_operator";

	public JsxGeneratorOperator(IJsxGeneratorService jsxGeneratorService, ObjectMapper objectMapper,
			UnifiedDirectoryManager unifiedDirectoryManager) {
		this.jsxGeneratorService = jsxGeneratorService;
		this.objectMapper = objectMapper;
		this.unifiedDirectoryManager = unifiedDirectoryManager;
	}

	/**
	 * Helper method to create detailed error messages
	 */
	private String createDetailedErrorMessage(String action, String missingParam, JsxGeneratorTool.JsxInput input) {
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
	 * Run the tool (accepts JsxInput object input)
	 */
	@Override
	public ToolExecuteResult run(JsxGeneratorTool.JsxInput input) {
		log.info("JsxGeneratorOperator input: {}", input);
		try {
			String planId = this.currentPlanId;

			if ("list_templates".equals(input.getAction())) {
				// Handle list templates operation
				var templates = jsxGeneratorService.getAvailableTemplates();
				return new ToolExecuteResult("Available templates: " + String.join(", ", templates));
			}
			else if ("generate_vue".equals(input.getAction())) {
				// Handle generate Vue SFC operation
				String componentType = input.getComponentType();
				var componentData = input.getComponentData();

				if (componentType == null || componentType.trim().isEmpty()) {
					return new ToolExecuteResult(createDetailedErrorMessage("generate_vue", "component_type", input));
				}
				if (componentData == null) {
					componentData = new java.util.HashMap<>();
					log.info("No component_data provided, using empty map for component generation");
				}

				log.info("JsxGeneratorOperator - Generating Vue SFC with componentType: {}, componentData keys: {}",
						componentType, componentData.keySet());
				String vueSfcCode = jsxGeneratorService.generateVueSFC(componentType, componentData);
				jsxGeneratorService.updateComponentState(planId, null, "Vue SFC generated successfully");
				return new ToolExecuteResult(
						"Vue SFC generated successfully for component type '" + componentType + "':\n" + vueSfcCode);
			}
			else if ("apply_template".equals(input.getAction())) {
				// Handle apply template operation
				String templateName = input.getTemplateName();
				var templateData = input.getTemplateData();

				if (templateName == null || templateName.trim().isEmpty()) {
					return new ToolExecuteResult(createDetailedErrorMessage("apply_template", "template_name", input));
				}
				if (templateData == null) {
					templateData = new java.util.HashMap<>();
					log.info("No template_data provided, using empty map for template application");
				}

				log.info("JsxGeneratorOperator - Applying template: {}, with data keys: {}", templateName,
						templateData.keySet());
				String generatedCode = jsxGeneratorService.applyHandlebarsTemplate(templateName, templateData);
				jsxGeneratorService.updateComponentState(planId, null, "Template applied successfully");
				return new ToolExecuteResult("Template '" + templateName + "' applied successfully:\n" + generatedCode);
			}
			else if ("save".equals(input.getAction())) {
				// Handle save operation
				String filePath = input.getFilePath();
				String vueSfcCode = input.getVueSfcCode();

				if (filePath == null || filePath.trim().isEmpty()) {
					return new ToolExecuteResult(createDetailedErrorMessage("save", "file_path", input));
				}
				if (vueSfcCode == null || vueSfcCode.trim().isEmpty()) {
					return new ToolExecuteResult(createDetailedErrorMessage("save", "vue_sfc_code", input));
				}

				log.info("JsxGeneratorOperator - Saving Vue SFC to file: {}, code length: {}", filePath,
						vueSfcCode.length());
				String savedPath = jsxGeneratorService.saveVueSfcToFile(planId, filePath, vueSfcCode);
				return new ToolExecuteResult("Vue SFC file saved successfully to: " + savedPath);
			}
			else if ("validate".equals(input.getAction())) {
				// Handle validate operation
				String vueSfcCode = input.getVueSfcCode();

				if (vueSfcCode == null || vueSfcCode.trim().isEmpty()) {
					return new ToolExecuteResult(createDetailedErrorMessage("validate", "vue_sfc_code", input));
				}

				log.info("JsxGeneratorOperator - Validating Vue SFC code, length: {}", vueSfcCode.length());
				String validationResult = jsxGeneratorService.validateVueSfc(vueSfcCode);
				return new ToolExecuteResult("Validation result: " + validationResult);
			}
			else {
				return new ToolExecuteResult("Unsupported operation: " + input.getAction()
						+ ". Supported operations: generate_vue, apply_template, save, validate, list_templates");
			}
		}
		catch (IllegalArgumentException e) {
			String planId = this.currentPlanId;
			jsxGeneratorService.updateComponentState(planId, null,
					"Error: Parameter validation failed: " + e.getMessage());
			return new ToolExecuteResult("Parameter validation failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("JSX generation failed", e);
			String planId = this.currentPlanId;
			jsxGeneratorService.updateComponentState(planId, null, "Error: JSX generation failed: " + e.getMessage());
			return new ToolExecuteResult("JSX generation failed: " + e.getMessage());
		}
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return "Tool for generating Vue SFC components with Handlebars templates and preview functionality. "
				+ "Supports operations: generate_vue (Generate Vue component), apply_template (Apply Handlebars template), "
				+ "save (Save Vue SFC to file), validate (Validate Vue SFC syntax), list_templates (List available templates). "
				+ "Generated Vue files will be saved in the project directory structure.";
	}

	@Override
	public String getParameters() {
		return """
				{
				    "type": "object",
				    "properties": {
				        "action": {
				            "type": "string",
				            "description": "Action to perform",
				            "enum": ["generate_vue", "apply_template", "save", "validate", "list_templates"]
				        },
				        "component_type": {
				            "type": "string",
				            "description": "Type of component to generate (e.g., button, form, chart)"
				        },
				        "component_data": {
				            "type": "object",
				            "description": "Component data including name, data, methods, computed, template, style"
				        },
				        "template_name": {
				            "type": "string",
				            "description": "Handlebars template name (e.g., counter-button, data-form)"
				        },
				        "template_data": {
				            "type": "object",
				            "description": "Data to apply to Handlebars template"
				        },
				        "file_path": {
				            "type": "string",
				            "description": "File path to save the Vue SFC code"
				        },
				        "vue_sfc_code": {
				            "type": "string",
				            "description": "Vue SFC code to save or validate"
				        }
				    },
				    "required": ["action"]
				}
				""";
	}

	@Override
	public Class<JsxGeneratorTool.JsxInput> getInputType() {
		return JsxGeneratorTool.JsxInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "frontend-tools";
	}

	@Override
	public String getCurrentToolStateString() {
		return "JSX Generator Operator is ready";
	}

	@Override
	public void cleanup(String planId) {
		if (jsxGeneratorService != null) {
			jsxGeneratorService.closeComponentForPlan(planId);
		}
	}

}
