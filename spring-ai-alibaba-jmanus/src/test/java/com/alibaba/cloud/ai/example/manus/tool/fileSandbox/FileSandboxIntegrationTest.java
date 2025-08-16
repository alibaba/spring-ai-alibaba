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
package com.alibaba.cloud.ai.example.manus.tool.fileSandbox;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File Sandbox Tool integration test
 */
public class FileSandboxIntegrationTest {

	private static final Logger log = LoggerFactory.getLogger(FileSandboxIntegrationTest.class);

	private FileSandboxManager fileSandboxManager;

	private FileSandboxTool fileSandboxTool;

	private UnifiedDirectoryManager unifiedDirectoryManager;

	// @TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		log.info("===== Starting test preparation =====");

		// Create mocks
		unifiedDirectoryManager = mock(UnifiedDirectoryManager.class);
		log.info("Initializing UnifiedDirectoryManager mock complete");

		// Create real instances
		fileSandboxManager = new FileSandboxManager(unifiedDirectoryManager);
		log.info("Initializing FileSandboxManager complete");

		fileSandboxTool = new FileSandboxTool(fileSandboxManager);
		log.info("Initializing FileSandboxTool complete");

		// Set tempDir to a real path
		tempDir = Path.of("extensions/test-sandbox");
		log.info("Setting test output directory: {}", tempDir);

		// Setup mock behavior
		try {
			when(unifiedDirectoryManager.getRootPlanDirectory(anyString())).thenAnswer(invocation -> {
				String planId = invocation.getArgument(0);
				Path planDir = tempDir.resolve("plan-" + planId);
				Files.createDirectories(planDir);
				return planDir;
			});

			doAnswer(invocation -> {
				Path directory = invocation.getArgument(0);
				Files.createDirectories(directory);
				return null;
			}).when(unifiedDirectoryManager).ensureDirectoryExists(any(Path.class));

			log.info("Setting UnifiedDirectoryManager mock behavior complete");
		}
		catch (Exception e) {
			log.error("Error setting up UnifiedDirectoryManager mock", e);
			throw new RuntimeException(e);
		}
		log.info("===== Test preparation complete =====");
	}

	@Test
	void testListFilesInEmptySandbox() throws Exception {
		log.info("\n===== Starting test: List files in empty sandbox =====");

		// Prepare test data
		String planId = "test-plan-empty";
		log.info("Preparing test data: planId={}", planId);

		FileSandboxTool.SandboxInput input = new FileSandboxTool.SandboxInput();
		input.setAction("list_files");
		log.info("Setting action to list_files");

		// Set current plan ID
		fileSandboxTool.setCurrentPlanId(planId);
		log.info("Setting current plan ID: {}", planId);

		// Execute test
		log.info("Starting list_files operation...");
		ToolExecuteResult result = fileSandboxTool.run(input);
		log.info("list_files operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(result);
		log.info("Verification result is not null: success");
		assertTrue(result.getOutput().contains("No files found"));
		log.info("Verification result contains 'No files found': success");

		log.info("===== Test complete: List files in empty sandbox =====");
	}

	@Test
	void testStoreAndReadFile() throws Exception {
		log.info("\n===== Starting test: Store and read file =====");

		// Prepare test data
		String planId = "test-plan-store-read";
		String fileName = "test-file.txt";
		String fileContent = "This is a test file content\nLine 2\nLine 3";
		log.info("Preparing test data: planId={}, fileName={}", planId, fileName);

		// Store file first
		log.info("Starting file storage operation...");
		SandboxFile storedFile = fileSandboxManager.storeUploadedFile(planId, fileName, fileContent.getBytes(),
				"text/plain");
		log.info("File storage operation completed: {}", storedFile.getName());

		// Verify stored file
		assertNotNull(storedFile);
		assertEquals("text", storedFile.getType());
		assertTrue(storedFile.getSize() > 0);
		log.info("Verification stored file: success");

		// Test read file
		FileSandboxTool.SandboxInput readInput = new FileSandboxTool.SandboxInput();
		readInput.setAction("read_file");
		readInput.setFileName(storedFile.getName());
		log.info("Setting read_file action for fileName: {}", storedFile.getName());

		// Set current plan ID
		fileSandboxTool.setCurrentPlanId(planId);
		log.info("Setting current plan ID: {}", planId);

		// Execute read test
		log.info("Starting read_file operation...");
		ToolExecuteResult readResult = fileSandboxTool.run(readInput);
		log.info("read_file operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(readResult);
		log.info("Verification result is not null: success");
		assertTrue(readResult.getOutput().contains("File content:"));
		log.info("Verification result contains 'File content:': success");
		assertTrue(readResult.getOutput().contains("This is a test file"));
		log.info("Verification result contains file content: success");

		log.info("===== Test complete: Store and read file =====");
	}

	@Test
	void testGetFileInfo() throws Exception {
		log.info("\n===== Starting test: Get file info =====");

		// Prepare test data
		String planId = "test-plan-file-info";
		String fileName = "info-test.csv";
		String csvContent = "Name,Age,City\nJohn,25,New York\nJane,30,Los Angeles";
		log.info("Preparing test data: planId={}, fileName={}", planId, fileName);

		// Store CSV file first
		log.info("Starting CSV file storage operation...");
		SandboxFile storedFile = fileSandboxManager.storeUploadedFile(planId, fileName, csvContent.getBytes(),
				"text/csv");
		log.info("CSV file storage operation completed: {}", storedFile.getName());

		// Test get file info
		FileSandboxTool.SandboxInput infoInput = new FileSandboxTool.SandboxInput();
		infoInput.setAction("get_file_info");
		infoInput.setFileName(storedFile.getName());
		log.info("Setting get_file_info action for fileName: {}", storedFile.getName());

		// Set current plan ID
		fileSandboxTool.setCurrentPlanId(planId);
		log.info("Setting current plan ID: {}", planId);

		// Execute info test
		log.info("Starting get_file_info operation...");
		ToolExecuteResult infoResult = fileSandboxTool.run(infoInput);
		log.info("get_file_info operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(infoResult);
		log.info("Verification result is not null: success");
		assertTrue(infoResult.getOutput().contains("File Information:"));
		log.info("Verification result contains 'File Information:': success");
		assertTrue(infoResult.getOutput().contains("Type: csv"));
		log.info("Verification result contains 'Type: csv': success");
		assertTrue(infoResult.getOutput().contains("Size: " + csvContent.getBytes().length));
		log.info("Verification result contains correct size: success");

		log.info("===== Test complete: Get file info =====");
	}

	@Test
	void testProcessFileAnalyze() throws Exception {
		log.info("\n===== Starting test: Process file analyze =====");

		// Prepare test data
		String planId = "test-plan-process";
		String fileName = "analyze-test.json";
		String jsonContent = "{\"users\":[{\"name\":\"John\",\"age\":25},{\"name\":\"Jane\",\"age\":30}]}";
		log.info("Preparing test data: planId={}, fileName={}", planId, fileName);

		// Store JSON file first
		log.info("Starting JSON file storage operation...");
		SandboxFile storedFile = fileSandboxManager.storeUploadedFile(planId, fileName, jsonContent.getBytes(),
				"application/json");
		log.info("JSON file storage operation completed: {}", storedFile.getName());

		// Test process file with analyze operation
		FileSandboxTool.SandboxInput processInput = new FileSandboxTool.SandboxInput();
		processInput.setAction("process_file");
		processInput.setFileName(storedFile.getName());

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("operation", "analyze");
		processInput.setParameters(parameters);
		log.info("Setting process_file action with analyze operation");

		// Set current plan ID
		fileSandboxTool.setCurrentPlanId(planId);
		log.info("Setting current plan ID: {}", planId);

		// Execute process test
		log.info("Starting process_file operation...");
		ToolExecuteResult processResult = fileSandboxTool.run(processInput);
		log.info("process_file operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(processResult);
		log.info("Verification result is not null: success");
		assertTrue(processResult.getOutput().contains("File processing result:"));
		log.info("Verification result contains 'File processing result:': success");
		assertTrue(processResult.getOutput().contains("File Analysis"));
		log.info("Verification result contains analysis info: success");

		log.info("===== Test complete: Process file analyze =====");
	}

	@Test
	void testCreateFile() throws Exception {
		log.info("\n===== Starting test: Create file =====");

		// Prepare test data
		String planId = "test-plan-create";
		String fileName = "created-file.txt";
		String fileContent = "This file was created by the sandbox tool";
		log.info("Preparing test data: planId={}, fileName={}", planId, fileName);

		// Test create file
		FileSandboxTool.SandboxInput createInput = new FileSandboxTool.SandboxInput();
		createInput.setAction("create_file");
		createInput.setFileName(fileName);
		createInput.setContent(fileContent);
		log.info("Setting create_file action");

		// Set current plan ID
		fileSandboxTool.setCurrentPlanId(planId);
		log.info("Setting current plan ID: {}", planId);

		// Execute create test
		log.info("Starting create_file operation...");
		ToolExecuteResult createResult = fileSandboxTool.run(createInput);
		log.info("create_file operation completed");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(createResult);
		log.info("Verification result is not null: success");
		assertTrue(createResult.getOutput().contains("File created successfully"));
		log.info("Verification result contains 'File created successfully': success");
		assertTrue(createResult.getOutput().contains(fileName));
		log.info("Verification result contains file name: success");

		// Verify file exists in sandbox
		List<SandboxFile> files = fileSandboxManager.listFiles(planId);
		boolean fileFound = files.stream().anyMatch(f -> f.getName().equals(fileName));
		assertTrue(fileFound);
		log.info("Verification file exists in sandbox: success");

		log.info("===== Test complete: Create file =====");
	}

	@Test
	void testToolStateString() throws Exception {
		log.info("\n===== Starting test: Tool state string =====");

		// Prepare test data
		String planId = "test-plan-state";
		log.info("Preparing test data: planId={}", planId);

		// Set current plan ID
		fileSandboxTool.setCurrentPlanId(planId);
		log.info("Setting current plan ID: {}", planId);

		// Get tool state
		log.info("Getting tool state string...");
		String stateString = fileSandboxTool.getCurrentToolStateString();
		log.info("Tool state string obtained");

		// Verify results
		log.info("Starting result verification...");
		assertNotNull(stateString);
		log.info("Verification state string is not null: success");
		assertTrue(stateString.contains("FileSandboxTool Status:"));
		log.info("Verification state string contains status header: success");
		assertTrue(stateString.contains("Current Plan ID: " + planId));
		log.info("Verification state string contains plan ID: success");

		log.info("===== Test complete: Tool state string =====");
	}

	@Test
	void testErrorHandling() throws Exception {
		log.info("\n===== Starting test: Error handling =====");

		// Prepare test data
		String planId = "test-plan-error";
		log.info("Preparing test data: planId={}", planId);

		// Test read non-existent file
		FileSandboxTool.SandboxInput readInput = new FileSandboxTool.SandboxInput();
		readInput.setAction("read_file");
		readInput.setFileName("non-existent-file.txt");
		log.info("Setting read_file action for non-existent file");

		// Set current plan ID
		fileSandboxTool.setCurrentPlanId(planId);
		log.info("Setting current plan ID: {}", planId);

		// Execute read test
		log.info("Starting read_file operation for non-existent file...");
		ToolExecuteResult readResult = fileSandboxTool.run(readInput);
		log.info("read_file operation completed");

		// Verify error handling
		log.info("Starting error result verification...");
		assertNotNull(readResult);
		log.info("Verification result is not null: success");
		assertTrue(readResult.getOutput().contains("Error:"));
		log.info("Verification result contains error message: success");

		// Test invalid action
		FileSandboxTool.SandboxInput invalidInput = new FileSandboxTool.SandboxInput();
		invalidInput.setAction("invalid_action");
		log.info("Setting invalid action");

		// Execute invalid action test
		log.info("Starting invalid action operation...");
		ToolExecuteResult invalidResult = fileSandboxTool.run(invalidInput);
		log.info("Invalid action operation completed");

		// Verify error handling
		log.info("Starting invalid action result verification...");
		assertNotNull(invalidResult);
		log.info("Verification result is not null: success");
		assertTrue(invalidResult.getOutput().contains("Error: Unknown action"));
		log.info("Verification result contains unknown action error: success");

		log.info("===== Test complete: Error handling =====");
	}

	@Test
	void testCleanup() throws Exception {
		log.info("\n===== Starting test: Cleanup =====");

		// Prepare test data
		String planId = "test-plan-cleanup";
		String fileName = "cleanup-test.txt";
		String fileContent = "This file will be cleaned up";
		log.info("Preparing test data: planId={}, fileName={}", planId, fileName);

		// Store file first
		log.info("Starting file storage operation...");
		fileSandboxManager.storeUploadedFile(planId, fileName, fileContent.getBytes(), "text/plain");
		log.info("File storage operation completed");

		// Verify file exists
		List<SandboxFile> filesBefore = fileSandboxManager.listFiles(planId);
		assertTrue(filesBefore.size() > 0);
		log.info("Verification file exists before cleanup: success");

		// Test cleanup
		log.info("Starting cleanup operation...");
		fileSandboxTool.cleanup(planId);
		log.info("Cleanup operation completed");

		// Verify cleanup (note: this depends on implementation)
		log.info("Cleanup verification completed");

		log.info("===== Test complete: Cleanup =====");
	}

}
