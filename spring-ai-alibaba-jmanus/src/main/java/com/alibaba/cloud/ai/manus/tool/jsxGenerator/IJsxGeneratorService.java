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
import java.util.Map;
import java.util.Set;

import com.alibaba.cloud.ai.manus.config.ManusProperties;

/**
 * Interface for JSX/Vue component generation operations. Provides methods for creating,
 * saving, and previewing Vue SFC components with Handlebars template support.
 */
public interface IJsxGeneratorService {

	/**
	 * Generate Vue Single File Component based on component specifications
	 * @param componentType Type of the component (e.g., 'button', 'form', 'chart')
	 * @param componentData Component data including props, data, methods, computed, etc.
	 * @return Generated Vue SFC code
	 */
	String generateVueSFC(String componentType, Map<String, Object> componentData);

	/**
	 * Generate Vue template section
	 * @param componentType Component type
	 * @param templateData Template data
	 * @return Generated template HTML
	 */
	String generateVueTemplate(String componentType, Map<String, Object> templateData);

	/**
	 * Generate Vue script section
	 * @param componentData Component data including data, methods, computed, etc.
	 * @return Generated script section
	 */
	String generateVueScript(Map<String, Object> componentData);

	/**
	 * Generate Vue style section
	 * @param styleData Style specifications
	 * @return Generated style section
	 */
	String generateVueStyle(Map<String, Object> styleData);

	/**
	 * Apply Handlebars template for quick code generation
	 * @param templateName Template name (e.g., 'counter-button', 'data-form')
	 * @param templateData Data to apply to template
	 * @return Generated code from template
	 */
	String applyHandlebarsTemplate(String templateName, Map<String, Object> templateData);

	/**
	 * Register a new Handlebars template
	 * @param templateName Template name
	 * @param templateContent Template content
	 */
	void registerTemplate(String templateName, String templateContent);

	/**
	 * Get available template names
	 * @return Set of available template names
	 */
	Set<String> getAvailableTemplates();

	/**
	 * Save Vue SFC code to a file
	 * @param planId Plan ID
	 * @param filePath File path to save the Vue SFC code
	 * @param vueSfcCode Vue SFC code to save
	 * @return Absolute path of the saved file
	 * @throws IOException if saving fails
	 */
	String saveVueSfcToFile(String planId, String filePath, String vueSfcCode) throws IOException;

	/**
	 * Update existing Vue component file
	 * @param planId Plan ID
	 * @param filePath File path
	 * @param sectionType Section to update ('template', 'script', 'style')
	 * @param newContent New content for the section
	 * @throws IOException if update fails
	 */
	void updateVueComponent(String planId, String filePath, String sectionType, String newContent) throws IOException;

	/**
	 * Generate Sandpack preview configuration
	 * @param planId Plan ID
	 * @param filePath Path to the Vue file
	 * @param dependencies Additional dependencies needed
	 * @return Sandpack configuration JSON
	 */
	String generateSandpackConfig(String planId, String filePath, Map<String, String> dependencies);

	/**
	 * Generate a preview URL for the Vue component in Sandpack
	 * @param planId Plan ID
	 * @param filePath Path to the Vue file
	 * @return Preview URL
	 */
	String generatePreviewUrl(String planId, String filePath);

	/**
	 * Validate Vue SFC syntax
	 * @param vueSfcCode Vue SFC code to validate
	 * @return Validation result with errors if any
	 */
	String validateVueSfc(String vueSfcCode);

	/**
	 * Get component state for specified plan
	 * @param planId Plan ID
	 * @return Component state
	 */
	Object getComponentState(String planId);

	/**
	 * Close components for specified plan
	 * @param planId Plan ID
	 */
	void closeComponentForPlan(String planId);

	/**
	 * Update component state
	 * @param planId Plan ID
	 * @param filePath File path
	 * @param operationResult Operation result
	 */
	void updateComponentState(String planId, String filePath, String operationResult);

	/**
	 * Get current component file path
	 * @param planId Plan ID
	 * @return Current component file path
	 */
	String getCurrentFilePath(String planId);

	/**
	 * Get last operation result for a plan
	 * @param planId Plan ID
	 * @return Last operation result
	 */
	String getLastOperationResult(String planId);

	/**
	 * Get Manus properties
	 * @return Manus properties
	 */
	ManusProperties getManusProperties();

	/**
	 * Clean up resources
	 */
	void cleanup();

}
