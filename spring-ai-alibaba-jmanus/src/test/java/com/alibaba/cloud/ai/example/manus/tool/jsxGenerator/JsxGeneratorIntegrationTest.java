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

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSX Generator Integration Test Class
 */
public class JsxGeneratorIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(JsxGeneratorIntegrationTest.class);

	private JsxGeneratorService jsxGeneratorService;

	private ObjectMapper objectMapper;

	private UnifiedDirectoryManager unifiedDirectoryManager;

	private ManusProperties manusProperties;

	private JsxGeneratorOperator jsxGeneratorOperator;

	Path tempDir;

	@BeforeEach
	void setUp() {
		log.info("===== Starting Test Setup =====");

		// Initialize mock dependencies
		manusProperties = mock(ManusProperties.class);
		unifiedDirectoryManager = mock(UnifiedDirectoryManager.class);
		objectMapper = new ObjectMapper();

		// Create JsxGeneratorService instance
		jsxGeneratorService = new JsxGeneratorService();
		log.info("JsxGeneratorService initialization completed");

		// Inject dependencies using reflection
		try {
			Field manusField = JsxGeneratorService.class.getDeclaredField("manusProperties");
			manusField.setAccessible(true);
			manusField.set(jsxGeneratorService, manusProperties);

			Field unifiedDirField = JsxGeneratorService.class.getDeclaredField("unifiedDirectoryManager");
			unifiedDirField.setAccessible(true);
			unifiedDirField.set(jsxGeneratorService, unifiedDirectoryManager);

			log.info("Successfully injected dependencies to JsxGeneratorService");
		}
		catch (Exception e) {
			log.error("Failed to inject dependencies", e);
			throw new RuntimeException("Failed to inject dependencies", e);
		}

		// Manually call run method to initialize default templates
		try {
			jsxGeneratorService.run(null);
			log.info("Successfully initialized default templates");
		}
		catch (Exception e) {
			log.error("Failed to initialize default templates", e);
			throw new RuntimeException("Failed to initialize default templates", e);
		}

		// Initialize operator
		jsxGeneratorOperator = new JsxGeneratorOperator((IJsxGeneratorService) jsxGeneratorService, objectMapper,
				unifiedDirectoryManager);
		log.info("JsxGeneratorOperator initialization completed");

		// Set test directory
		tempDir = Path.of("./extensions");
		log.info("Set test output directory: {}", tempDir);

		// Set mock behavior
		try {
			when(unifiedDirectoryManager.getRootPlanDirectory(anyString())).thenAnswer(invocation -> {
				String planId = invocation.getArgument(0);
				return tempDir.resolve("test-jsx-output").resolve(planId);
			});
			log.info("UnifiedDirectoryManager mock behavior setup completed");
		}
		catch (Exception e) {
			log.error("Error occurred while setting UnifiedDirectoryManager mock", e);
			throw new RuntimeException(e);
		}

		log.info("===== Test Setup Completed =====");
	}

	@Test
	void testGenerateBasicVueComponent() throws Exception {
		log.info("\n===== Starting Test: Generate Basic Vue Component =====");

		String planId = "test-plan-vue";
		log.info("Prepare test data: planId={}", planId);

		// Prepare input data
		JsxGeneratorTool.JsxInput input = new JsxGeneratorTool.JsxInput();
		input.setAction("generate_vue");
		input.setComponentType("button");

		Map<String, Object> componentData = new HashMap<>();
		componentData.put("name", "TestButton");
		Map<String, Object> data = new HashMap<>();
		data.put("count", 0);
		componentData.put("data", data);
		input.setComponentData(componentData);

		log.info("Vue component data setup completed: componentType={}, name={}", input.getComponentType(),
				componentData.get("name"));

		// Set current plan ID
		jsxGeneratorOperator.setCurrentPlanId(planId);
		log.info("Set current plan ID: {}", planId);

		// Execute test
		log.info("Starting Vue component generation operation...");
		ToolExecuteResult result = jsxGeneratorOperator.run(input);
		log.info("Vue component generation operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(result);
		log.info("Verify result is not null: success");
		assertTrue(result.getOutput().contains("successfully"));
		log.info("Verify result contains success message: success");
		assertTrue(result.getOutput().contains("<template>"));
		log.info("Verify result contains template tag: success");
		assertTrue(result.getOutput().contains("<script>"));
		log.info("Verify result contains script tag: success");
		assertTrue(result.getOutput().contains("<style"));
		log.info("Verify result contains style tag: success");

		log.info("===== Test Completed: Generate Basic Vue Component =====");
	}

	@Test
	void testApplyHandlebarsTemplate() throws Exception {
		log.info("\n===== Starting Test: Apply Handlebars Template =====");

		String planId = "test-plan-template";
		log.info("Prepare test data: planId={}", planId);

		// Prepare input data
		JsxGeneratorTool.JsxInput input = new JsxGeneratorTool.JsxInput();
		input.setAction("apply_template");
		input.setTemplateName("counter-button");

		Map<String, Object> templateData = new HashMap<>();
		templateData.put("componentName", "TestCounterButton");
		templateData.put("buttonText", "Click Me");
		templateData.put("initialCount", "0");
		templateData.put("threshold", "5");
		input.setTemplateData(templateData);

		log.info("Template data setup completed: templateName={}, componentName={}", input.getTemplateName(),
				templateData.get("componentName"));

		// Set current plan ID
		jsxGeneratorOperator.setCurrentPlanId(planId);
		log.info("Set current plan ID: {}", planId);

		// Execute test
		log.info("Starting apply template operation...");
		ToolExecuteResult result = jsxGeneratorOperator.run(input);
		log.info("Apply template operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(result);
		log.info("Verify result is not null: success");
		assertTrue(result.getOutput().contains("applied successfully"));
		log.info("Verify result contains success message: success");
		assertTrue(result.getOutput().contains("TestCounterButton"));
		log.info("Verify result contains component name: success");
		assertTrue(result.getOutput().contains("Click Me"));
		log.info("Verify result contains button text: success");

		log.info("===== Test Completed: Apply Handlebars Template =====");
	}

	@Test
	void testSaveVueSfcToFile() throws Exception {
		log.info("\n===== Starting Test: Save Vue SFC File =====");

		String planId = "test-plan-save";
		String fileName = "TestComponent.vue";
		log.info("Prepare test data: planId={}, fileName={}", planId, fileName);

		// Prepare Vue SFC code
		String vueSfcCode = """
				<template>
				  <div class="test-component">
				    <h1>Test Component</h1>
				    <button @click="handleClick">Click Me</button>
				  </div>
				</template>

				<script>
				export default {
				  name: 'TestComponent',
				  methods: {
				    handleClick() {
				      console.log('Button clicked');
				    }
				  }
				};
				</script>

				<style scoped>
				.test-component {
				  padding: 20px;
				}
				</style>
				""";

		// Prepare input data
		JsxGeneratorTool.JsxInput input = new JsxGeneratorTool.JsxInput();
		input.setAction("save");
		input.setFilePath(fileName);
		input.setVueSfcCode(vueSfcCode);

		log.info("Save data setup completed: filePath={}", input.getFilePath());

		// Set current plan ID
		jsxGeneratorOperator.setCurrentPlanId(planId);
		log.info("Set current plan ID: {}", planId);

		// Execute test
		log.info("Starting save file operation...");
		ToolExecuteResult result = jsxGeneratorOperator.run(input);
		log.info("Save file operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(result);
		log.info("Verify result is not null: success");
		assertTrue(result.getOutput().contains("successfully"));
		log.info("Verify result contains success message: success");
		assertTrue(result.getOutput().contains(fileName));
		log.info("Verify result contains file name: success");

		// Verify file creation
		Path expectedPath = tempDir.resolve("test-jsx-output").resolve(planId).resolve(fileName);
		log.info("Verify file creation: {}", expectedPath);
		assertTrue(Files.exists(expectedPath));
		log.info("Verify file exists: success");

		String savedContent = Files.readString(expectedPath);
		assertTrue(savedContent.contains("TestComponent"));
		log.info("Verify file content is correct: success");

		log.info("===== Test Completed: Save Vue SFC File =====");
	}

	@Test
	void testValidateVueSfcCode() throws Exception {
		log.info("\n===== Starting Test: Validate Vue SFC Code =====");

		String planId = "test-plan-validate";
		log.info("Prepare test data: planId={}", planId);

		// Test valid Vue SFC code
		String validVueSfcCode = """
				<template>
				  <div>Valid component</div>
				</template>

				<script>
				export default {
				  name: 'ValidComponent'
				};
				</script>

				<style>
				div { color: blue; }
				</style>
				""";

		// Prepare input data
		JsxGeneratorTool.JsxInput input = new JsxGeneratorTool.JsxInput();
		input.setAction("validate");
		input.setVueSfcCode(validVueSfcCode);

		log.info("Validation data setup completed");

		// Set current plan ID
		jsxGeneratorOperator.setCurrentPlanId(planId);
		log.info("Set current plan ID: {}", planId);

		// Execute test
		log.info("Starting validation operation...");
		ToolExecuteResult result = jsxGeneratorOperator.run(input);
		log.info("Validation operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(result);
		log.info("Verify result is not null: success");
		assertTrue(result.getOutput().contains("Valid Vue SFC"));
		log.info("Verify result contains valid message: success");

		// Test invalid Vue SFC code
		log.info("Starting test for invalid code validation...");
		String invalidVueSfcCode = "<div>Missing template tags</div>";

		input.setVueSfcCode(invalidVueSfcCode);
		ToolExecuteResult invalidResult = jsxGeneratorOperator.run(input);

		assertNotNull(invalidResult);
		assertTrue(invalidResult.getOutput().contains("Error"));
		log.info("Verify invalid code detection: success");

		log.info("===== Test Completed: Validate Vue SFC Code =====");
	}

	@Test
	void testListAvailableTemplates() throws Exception {
		log.info("\n===== Starting Test: List Available Templates =====");

		String planId = "test-plan-list";
		log.info("Prepare test data: planId={}", planId);

		// Prepare input data
		JsxGeneratorTool.JsxInput input = new JsxGeneratorTool.JsxInput();
		input.setAction("list_templates");

		log.info("List operation data setup completed");

		// Set current plan ID
		jsxGeneratorOperator.setCurrentPlanId(planId);
		log.info("Set current plan ID: {}", planId);

		// Execute test
		log.info("Starting list templates operation...");
		ToolExecuteResult result = jsxGeneratorOperator.run(input);
		log.info("List templates operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(result);
		log.info("Verify result is not null: success");
		assertTrue(result.getOutput().contains("Available templates"));
		log.info("Verify result contains available templates info: success");
		assertTrue(result.getOutput().contains("counter-button"));
		log.info("Verify result contains counter-button template: success");
		assertTrue(result.getOutput().contains("data-form"));
		log.info("Verify result contains data-form template: success");

		log.info("===== Test Completed: List Available Templates =====");
	}

	@Test
	void testMinimalInput() throws Exception {
		log.info("\n===== Starting Test: Generate Component with Minimal Input =====");

		String planId = "test-plan-minimal";
		log.info("Prepare test data: planId={}", planId);

		// Prepare minimal input data
		JsxGeneratorTool.JsxInput input = new JsxGeneratorTool.JsxInput();
		input.setAction("generate_vue");
		input.setComponentType("button");
		// Don't set componentData, should use default values

		log.info("Minimal component data setup completed: componentType={}", input.getComponentType());

		// Set current plan ID
		jsxGeneratorOperator.setCurrentPlanId(planId);
		log.info("Set current plan ID: {}", planId);

		// Execute test
		log.info("Starting minimal input component generation operation...");
		ToolExecuteResult result = jsxGeneratorOperator.run(input);
		log.info("Minimal input component generation operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(result);
		log.info("Verify result is not null: success");
		assertTrue(result.getOutput().contains("successfully"));
		log.info("Verify result contains success message: success");

		log.info("===== Test Completed: Generate Component with Minimal Input =====");
	}

	@Test
	void testInvalidOperations() throws Exception {
		log.info("\n===== Starting Test: Invalid Operation Handling =====");

		String planId = "test-plan-invalid";
		log.info("Prepare test data: planId={}", planId);

		// Test invalid action
		JsxGeneratorTool.JsxInput input = new JsxGeneratorTool.JsxInput();
		input.setAction("invalid_action");

		log.info("Invalid operation data setup completed: action={}", input.getAction());

		// Set current plan ID
		jsxGeneratorOperator.setCurrentPlanId(planId);
		log.info("Set current plan ID: {}", planId);

		// Execute test
		log.info("Starting invalid operation test...");
		ToolExecuteResult result = jsxGeneratorOperator.run(input);
		log.info("Invalid operation test completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(result);
		log.info("Verify result is not null: success");
		assertTrue(result.getOutput().contains("Unsupported operation"));
		log.info("Verify result contains unsupported operation message: success");

		log.info("===== Test Completed: Invalid Operation Handling =====");
	}

}
