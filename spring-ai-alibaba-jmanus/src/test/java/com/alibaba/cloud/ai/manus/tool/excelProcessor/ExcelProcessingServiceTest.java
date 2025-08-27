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
package com.alibaba.cloud.ai.manus.tool.excelProcessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Excel Processing Service Test Class
 */
public class ExcelProcessingServiceTest {

	private static final Logger log = LoggerFactory.getLogger(ExcelProcessingServiceTest.class);

	private ExcelProcessingService excelProcessingService;

	@Mock
	private UnifiedDirectoryManager unifiedDirectoryManager;

	// @TempDir
	Path tempDir = Path.of("./extensions");

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		// Mock the UnifiedDirectoryManager to return the temp directory
		when(unifiedDirectoryManager.getRootPlanDirectory(anyString())).thenReturn(tempDir);

		excelProcessingService = new ExcelProcessingService(unifiedDirectoryManager);

		// Ensure temp directory exists
		if (!Files.exists(tempDir)) {
			Files.createDirectories(tempDir);
		}
	}

	@Test
	void testIsSupportedFileType() {
		// Test supported file types
		assertTrue(excelProcessingService.isSupportedFileType(".xlsx"));
		assertTrue(excelProcessingService.isSupportedFileType(".xls"));
		assertTrue(excelProcessingService.isSupportedFileType(".csv"));

		// Test unsupported file types
		assertFalse(excelProcessingService.isSupportedFileType(".txt"));
		assertFalse(excelProcessingService.isSupportedFileType(".pdf"));
		assertFalse(excelProcessingService.isSupportedFileType(".doc"));
	}

	@Test
	void testValidateFilePath() {
		// Test valid file paths
		assertTrue(excelProcessingService.validateFilePath("/path/to/file.xlsx"));
		assertTrue(excelProcessingService.validateFilePath("/path/to/file.xls"));
		assertTrue(excelProcessingService.validateFilePath("/path/to/file.csv"));

		// Test invalid file paths
		assertFalse(excelProcessingService.validateFilePath(null));
		assertFalse(excelProcessingService.validateFilePath(""));
		assertFalse(excelProcessingService.validateFilePath("   "));
		assertFalse(excelProcessingService.validateFilePath("/path/to/file.txt"));
	}

	@Test
	void testCreateExcelFile() throws Exception {
		Path testFile = tempDir.resolve("test.xlsx");
		String filePath = "test.xlsx"; // Use relative filename only

		// Test creating Excel file with headers
		List<String> headers = Arrays.asList("Name", "Age", "Email");
		Map<String, Object> result = excelProcessingService.createExcelFile(filePath, "Sheet1", headers);

		assertNotNull(result);
		assertTrue((Boolean) result.get("success"));
		assertTrue(Files.exists(testFile));
		assertEquals(filePath, excelProcessingService.getCurrentFilePath());
	}

	@Test
	void testGetExcelStructure() throws Exception {
		Path testFile = tempDir.resolve("structure_test.xlsx");
		String filePath = testFile.toString();

		// Create a test Excel file first
		List<String> headers = Arrays.asList("ID", "Name", "Department");
		excelProcessingService.createExcelFile(filePath, "Employees", headers);

		// Test getting structure
		Map<String, Object> structure = excelProcessingService.getExcelStructure(filePath);

		assertNotNull(structure);
		assertTrue(structure.containsKey("sheets"));
		assertTrue(structure.containsKey("totalSheets"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> sheets = (List<Map<String, Object>>) structure.get("sheets");
		assertFalse(sheets.isEmpty());

		Map<String, Object> firstSheet = sheets.get(0);
		assertEquals("Employees", firstSheet.get("name"));
		assertTrue(firstSheet.containsKey("headers"));
	}

	@Test
	void testWriteDataToExcel() throws Exception {
		Path testFile = tempDir.resolve("write_test.xlsx");
		String filePath = testFile.toString();

		// Create Excel file
		List<String> headers = Arrays.asList("ID", "Name", "Age");
		excelProcessingService.createExcelFile(filePath, "TestSheet", headers);

		// Prepare test data
		List<List<Object>> data = Arrays.asList(Arrays.asList(1, "John Doe", 30), Arrays.asList(2, "Jane Smith", 25),
				Arrays.asList(3, "Bob Johnson", 35));

		// Test writing data
		Map<String, Object> result = excelProcessingService.writeDataToExcel(filePath, "TestSheet", data, 2);

		assertNotNull(result);
		assertTrue((Boolean) result.get("success"));
		assertEquals(3, result.get("rowsWritten"));
	}

	@Test
	void testReadDataFromExcel() throws Exception {
		Path testFile = tempDir.resolve("read_test.xlsx");
		String filePath = testFile.toString();

		// Create and populate Excel file
		List<String> headers = Arrays.asList("ID", "Name", "Score");
		excelProcessingService.createExcelFile(filePath, "Data", headers);

		List<List<Object>> testData = Arrays.asList(Arrays.asList(1, "Alice", 95.5), Arrays.asList(2, "Bob", 87.0),
				Arrays.asList(3, "Charlie", 92.3));
		excelProcessingService.writeDataToExcel(filePath, "Data", testData, 2);

		// Test reading data
		Map<String, Object> result = excelProcessingService.readDataFromExcel(filePath, "Data", 1, 10);

		assertNotNull(result);
		assertTrue((Boolean) result.get("success"));
		assertTrue(result.containsKey("data"));
		assertTrue(result.containsKey("totalRows"));

		@SuppressWarnings("unchecked")
		List<List<Object>> readData = (List<List<Object>>) result.get("data");
		assertFalse(readData.isEmpty());
	}

	@Test
	void testSearchDataInExcel() throws Exception {
		Path testFile = tempDir.resolve("search_test.xlsx");
		String filePath = testFile.toString();

		// Create and populate Excel file
		List<String> headers = Arrays.asList("ID", "Name", "Department");
		excelProcessingService.createExcelFile(filePath, "Employees", headers);

		List<List<Object>> testData = Arrays.asList(Arrays.asList(1, "John Doe", "Engineering"),
				Arrays.asList(2, "Jane Smith", "Marketing"), Arrays.asList(3, "Bob Johnson", "Engineering"),
				Arrays.asList(4, "Alice Brown", "Sales"));
		excelProcessingService.writeDataToExcel(filePath, "Employees", testData, 2);

		// Test searching data
		Map<String, Object> searchCriteria = new HashMap<>();
		searchCriteria.put("Department", "Engineering");

		Map<String, Object> result = excelProcessingService.searchDataInExcel(filePath, "Employees", searchCriteria);

		assertNotNull(result);
		assertTrue((Boolean) result.get("success"));
		assertTrue(result.containsKey("matches"));

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> matches = (List<Map<String, Object>>) result.get("matches");
		assertEquals(2, matches.size()); // Should find John Doe and Bob Johnson
	}

	@Test
	void testBatchProcessExcel() throws Exception {
		Path testFile = tempDir.resolve("batch_test.xlsx");
		String filePath = testFile.toString();

		// Create Excel file
		List<String> headers = Arrays.asList("ID", "Value");
		excelProcessingService.createExcelFile(filePath, "BatchData", headers);

		// Prepare large dataset for batch processing
		List<List<Object>> largeData = new ArrayList<>();
		for (int i = 1; i <= 1000; i++) {
			largeData.add(Arrays.asList(i, "Value_" + i));
		}

		// Test batch processing
		Map<String, Object> result = excelProcessingService.batchProcessExcel(filePath, "BatchData", largeData,
				"write");

		assertNotNull(result);
		assertTrue((Boolean) result.get("success"));
		assertTrue(result.containsKey("processedBatches"));
		assertTrue(result.containsKey("totalProcessed"));
		assertEquals(1000, result.get("totalProcessed"));
	}

	@Test
	void testGetProcessingStatus() {
		// Test initial status
		Map<String, Object> status = excelProcessingService.getProcessingStatus();

		assertNotNull(status);
		assertTrue(status.containsKey("isProcessing"));
		assertTrue(status.containsKey("currentOperation"));
		assertTrue(status.containsKey("progress"));
		assertFalse((Boolean) status.get("isProcessing"));
	}

	@Test
	void testCleanupPlanResources() {
		String planId = "test-plan-123";

		// This should not throw any exception
		assertDoesNotThrow(() -> {
			excelProcessingService.cleanupPlanResources(planId);
		});
	}

	@Test
	void testErrorHandling() {
		// Test with non-existent file
		String nonExistentFile = "/non/existent/path/file.xlsx";

		Map<String, Object> result = excelProcessingService.getExcelStructure(nonExistentFile);
		assertNotNull(result);
		assertFalse((Boolean) result.get("success"));
		assertTrue(result.containsKey("error"));

		// Test with invalid file path
		result = excelProcessingService.readDataFromExcel("", "Sheet1", 1, 10);
		assertNotNull(result);
		assertFalse((Boolean) result.get("success"));
		assertTrue(result.containsKey("error"));
	}

	@Test
	void testMemoryOptimization() throws Exception {
		Path testFile = tempDir.resolve("memory_test.xlsx");
		String filePath = testFile.toString();

		// Create Excel file
		List<String> headers = Arrays.asList("ID", "Data");
		excelProcessingService.createExcelFile(filePath, "MemoryTest", headers);

		// Test with large dataset to verify memory optimization
		List<List<Object>> largeData = new ArrayList<>();
		for (int i = 1; i <= 5000; i++) {
			largeData
				.add(Arrays.asList(i, "Large data entry " + i + " with some additional text to increase memory usage"));
		}

		// This should complete without memory issues
		Map<String, Object> result = excelProcessingService.batchProcessExcel(filePath, "MemoryTest", largeData,
				"write");

		assertNotNull(result);
		assertTrue((Boolean) result.get("success"));
		assertEquals(5000, result.get("totalProcessed"));
	}

}
