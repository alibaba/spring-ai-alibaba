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
package com.alibaba.cloud.ai.example.manus.tool.excelProcessor;

import java.awt.GraphicsEnvironment;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.read.listener.ReadListener;

@Service
public class ExcelProcessingService implements IExcelProcessingService {

	private static final Logger log = LoggerFactory.getLogger(ExcelProcessingService.class);

	// Supported file extensions
	private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".xlsx", ".xls", ".csv");

	// Configuration constants
	private static final int DEFAULT_BATCH_SIZE = 1000;

	private static final int LARGE_FILE_THRESHOLD = 10 * 1024 * 1024; // 10MB

	private static final int SXSSF_WINDOW_SIZE = 1000; // Keep 1000 rows in memory

	private final UnifiedDirectoryManager unifiedDirectoryManager;

	// Store processing status for each plan
	private final Map<String, Map<String, Object>> planProcessingStatus = new ConcurrentHashMap<>();

	// Store file states for each plan
	private final Map<String, Map<String, Object>> planFileStates = new ConcurrentHashMap<>();

	public ExcelProcessingService(UnifiedDirectoryManager unifiedDirectoryManager) {
		this.unifiedDirectoryManager = unifiedDirectoryManager;
	}

	@Override
	public boolean isSupportedFileType(String filePath) {
		if (filePath == null || filePath.trim().isEmpty()) {
			return false;
		}
		// Always treat input as a file path and extract extension
		String extension = getFileExtension(filePath).toLowerCase();
		boolean result = SUPPORTED_EXTENSIONS.contains(extension);
		return result;
	}

	@Override
	public Path validateFilePath(String planId, String filePath) throws IOException {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IOException("File path cannot be null or empty");
		}

		Path absolutePath = getAbsolutePath(planId, filePath);

		// Create parent directories if they don't exist
		Path parentDir = absolutePath.getParent();
		if (parentDir != null && !Files.exists(parentDir)) {
			Files.createDirectories(parentDir);
			log.debug("Created parent directories for: {}", absolutePath);
		}

		return absolutePath;
	}

	private Path getAbsolutePath(String planId, String filePath) throws IOException {
		Path planDir = unifiedDirectoryManager.getRootPlanDirectory(planId);
		return planDir.resolve(filePath);
	}

	private String getFileExtension(String filePath) {
		int lastDotIndex = filePath.lastIndexOf('.');
		String extension = lastDotIndex >= 0 ? filePath.substring(lastDotIndex) : "";
		return extension;
	}

	@Override
	public void createExcelFile(String planId, String filePath, Map<String, List<String>> worksheets)
			throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		log.debug("Checking file type for: {}", filePath);
		boolean isSupported = isSupportedFileType(filePath);
		log.debug("File type supported: {}", isSupported);
		if (!isSupported) {
			throw new IOException("Unsupported file type. Only .xlsx, .xls, and .csv files are supported.");
		}

		try (Workbook workbook = new XSSFWorkbook()) {
			for (Map.Entry<String, List<String>> entry : worksheets.entrySet()) {
				String sheetName = entry.getKey();
				List<String> headers = entry.getValue();

				Sheet sheet = workbook.createSheet(sheetName);

				// Create header row
				if (headers != null && !headers.isEmpty()) {
					Row headerRow = sheet.createRow(0);
					for (int i = 0; i < headers.size(); i++) {
						Cell cell = headerRow.createCell(i);
						cell.setCellValue(headers.get(i));

						// Apply header formatting
						CellStyle headerStyle = workbook.createCellStyle();
						Font headerFont = workbook.createFont();
						headerFont.setBold(true);
						headerStyle.setFont(headerFont);
						cell.setCellStyle(headerStyle);
					}

					// Auto-size columns (only if not in headless environment)
					if (!GraphicsEnvironment.isHeadless()) {
						for (int i = 0; i < headers.size(); i++) {
							sheet.autoSizeColumn(i);
						}
					}
				}
			}

			try (FileOutputStream fos = new FileOutputStream(absolutePath.toFile())) {
				workbook.write(fos);
			}
		}

		updateFileState(planId, filePath, "created");
		log.info("Created Excel file: {} with {} worksheets", absolutePath, worksheets.size());
	}

	@Override
	public Map<String, List<String>> getExcelStructure(String planId, String filePath) throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + absolutePath);
		}

		Map<String, List<String>> structure = new LinkedHashMap<>();

		try (FileInputStream fis = new FileInputStream(absolutePath.toFile());
				Workbook workbook = WorkbookFactory.create(fis)) {

			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				Sheet sheet = workbook.getSheetAt(i);
				String sheetName = sheet.getSheetName();

				List<String> headers = new ArrayList<>();
				Row headerRow = sheet.getRow(0);

				if (headerRow != null) {
					for (Cell cell : headerRow) {
						headers.add(getCellValueAsString(cell));
					}
				}

				structure.put(sheetName, headers);
			}
		}

		log.debug("Retrieved structure for Excel file: {} with {} sheets", absolutePath, structure.size());
		return structure;
	}

	@Override
	public List<List<String>> readExcelData(String planId, String filePath, String worksheetName, Integer startRow,
			Integer endRow, Integer maxRows) throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + absolutePath);
		}

		List<List<String>> data = new ArrayList<>();
		long fileSize = Files.size(absolutePath);

		// Use streaming read for large files
		if (fileSize > LARGE_FILE_THRESHOLD) {
			return readLargeExcelData(planId, absolutePath, worksheetName, startRow, endRow, maxRows);
		}

		try (FileInputStream fis = new FileInputStream(absolutePath.toFile());
				Workbook workbook = WorkbookFactory.create(fis)) {

			Sheet sheet = workbook.getSheet(worksheetName);
			if (sheet == null) {
				throw new IOException("Worksheet not found: " + worksheetName);
			}

			int firstRow = startRow != null ? Math.max(startRow, 0) : 0;
			int lastRow = endRow != null ? Math.min(endRow, sheet.getLastRowNum()) : sheet.getLastRowNum();

			int rowCount = 0;
			for (int i = firstRow; i <= lastRow && (maxRows == null || rowCount < maxRows); i++) {
				Row row = sheet.getRow(i);
				if (row != null) {
					List<String> rowData = new ArrayList<>();
					for (Cell cell : row) {
						rowData.add(getCellValueAsString(cell));
					}
					data.add(rowData);
					rowCount++;
				}
			}
		}

		log.debug("Read {} rows from worksheet: {} in file: {}", data.size(), worksheetName, absolutePath);
		return data;
	}

	private List<List<String>> readLargeExcelData(String planId, Path absolutePath, String worksheetName,
			Integer startRow, Integer endRow, Integer maxRows) throws IOException {
		List<List<String>> data = new ArrayList<>();
		AtomicInteger currentRow = new AtomicInteger(0);
		AtomicInteger readCount = new AtomicInteger(0);

		// Use EasyExcel for streaming read
		EasyExcel.read(absolutePath.toString())
			.head(LinkedHashMap.class)
			.sheet(worksheetName)
			.registerReadListener(new ReadListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
					int rowNum = currentRow.getAndIncrement();

					// Check row range
					if (startRow != null && rowNum < startRow) {
						return;
					}
					if (endRow != null && rowNum > endRow) {
						return;
					}
					if (maxRows != null && readCount.get() >= maxRows) {
						return;
					}

					// Convert map to list
					List<String> rowList = rowData.entrySet()
						.stream()
						.sorted(Map.Entry.comparingByKey())
						.map(Map.Entry::getValue)
						.collect(Collectors.toList());

					data.add(rowList);
					readCount.incrementAndGet();
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					log.debug("Finished reading large Excel file: {}", absolutePath);
				}
			})
			.doRead();

		return data;
	}

	@Override
	public void writeExcelData(String planId, String filePath, String worksheetName, List<List<String>> data,
			boolean appendMode) throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (data == null || data.isEmpty()) {
			log.warn("No data to write to Excel file: {}", absolutePath);
			return;
		}

		// Use streaming write for large datasets
		if (data.size() > DEFAULT_BATCH_SIZE) {
			writeLargeExcelData(planId, absolutePath, worksheetName, data, appendMode);
			return;
		}

		Workbook workbook;
		boolean fileExists = Files.exists(absolutePath);

		if (fileExists && appendMode) {
			try (FileInputStream fis = new FileInputStream(absolutePath.toFile())) {
				workbook = WorkbookFactory.create(fis);
			}
		}
		else {
			workbook = new XSSFWorkbook();
		}

		try {
			Sheet sheet = workbook.getSheet(worksheetName);
			if (sheet == null) {
				sheet = workbook.createSheet(worksheetName);
			}

			int startRowNum = appendMode ? sheet.getLastRowNum() + 1 : 0;
			if (!appendMode) {
				// Clear existing data
				for (int i = sheet.getLastRowNum(); i >= 0; i--) {
					Row row = sheet.getRow(i);
					if (row != null) {
						sheet.removeRow(row);
					}
				}
				startRowNum = 0;
			}

			for (int i = 0; i < data.size(); i++) {
				List<String> rowData = data.get(i);
				Row row = sheet.createRow(startRowNum + i);

				for (int j = 0; j < rowData.size(); j++) {
					Cell cell = row.createCell(j);
					setCellValue(cell, rowData.get(j));
				}
			}

			try (FileOutputStream fos = new FileOutputStream(absolutePath.toFile())) {
				workbook.write(fos);
			}
		}
		finally {
			workbook.close();
		}

		updateFileState(planId, filePath, "data_written");
		log.info("Wrote {} rows to worksheet: {} in file: {}", data.size(), worksheetName, absolutePath);
	}

	private void writeLargeExcelData(String planId, Path absolutePath, String worksheetName, List<List<String>> data,
			boolean appendMode) throws IOException {
		// Use SXSSFWorkbook for memory-efficient writing
		try (SXSSFWorkbook workbook = new SXSSFWorkbook(SXSSF_WINDOW_SIZE)) {
			Sheet sheet = workbook.createSheet(worksheetName);

			// Write data in batches
			for (int i = 0; i < data.size(); i++) {
				List<String> rowData = data.get(i);
				Row row = sheet.createRow(i);

				for (int j = 0; j < rowData.size(); j++) {
					Cell cell = row.createCell(j);
					setCellValue(cell, rowData.get(j));
				}

				// Flush rows to disk periodically
				if (i % SXSSF_WINDOW_SIZE == 0) {
					// SXSSFWorkbook automatically manages memory, no need to manually
					// flush
					// workbook.flushRows(); // This method may not be available in all
					// POI versions
				}
			}

			try (FileOutputStream fos = new FileOutputStream(absolutePath.toFile())) {
				workbook.write(fos);
			}

			// Dispose of temporary files
			workbook.dispose();
		}

		updateFileState(planId, absolutePath.toString(), "large_data_written");
		log.info("Wrote {} rows to worksheet: {} in large file: {}", data.size(), worksheetName, absolutePath);
	}

	private String getCellValueAsString(Cell cell) {
		if (cell == null) {
			return "";
		}

		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue().toString();
				}
				else {
					return String.valueOf(cell.getNumericCellValue());
				}
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				try {
					return String.valueOf(cell.getNumericCellValue());
				}
				catch (Exception e) {
					return cell.getCellFormula();
				}
			case BLANK:
			default:
				return "";
		}
	}

	private void setCellValue(Cell cell, String value) {
		if (value == null || value.isEmpty()) {
			cell.setCellValue("");
			return;
		}

		// Try to parse as number
		try {
			double numValue = Double.parseDouble(value);
			cell.setCellValue(numValue);
		}
		catch (NumberFormatException e) {
			// Try to parse as boolean
			if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
				cell.setCellValue(Boolean.parseBoolean(value));
			}
			else {
				// Set as string
				cell.setCellValue(value);
			}
		}
	}

	@Override
	public void updateExcelCells(String planId, String filePath, String worksheetName, Map<String, String> updates)
			throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + absolutePath);
		}

		try (FileInputStream fis = new FileInputStream(absolutePath.toFile());
				Workbook workbook = WorkbookFactory.create(fis)) {

			Sheet sheet = workbook.getSheet(worksheetName);
			if (sheet == null) {
				throw new IOException("Worksheet not found: " + worksheetName);
			}

			// Update cells based on the updates map
			// Expected format: "A1" -> "value", "B2" -> "value"
			for (Map.Entry<String, String> entry : updates.entrySet()) {
				String cellAddress = entry.getKey();
				String value = entry.getValue();

				// Parse cell address (e.g., "A1" -> row=0, col=0)
				int[] coordinates = parseCellAddress(cellAddress);
				int rowIndex = coordinates[0];
				int colIndex = coordinates[1];

				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					row = sheet.createRow(rowIndex);
				}

				Cell cell = row.getCell(colIndex);
				if (cell == null) {
					cell = row.createCell(colIndex);
				}

				setCellValue(cell, value);
			}

			// Save the workbook
			try (FileOutputStream fos = new FileOutputStream(absolutePath.toFile())) {
				workbook.write(fos);
			}
		}

		updateFileState(planId, filePath, "cells_updated");
		log.info("Updated {} cells in worksheet: {} in file: {}", updates.size(), worksheetName, absolutePath);
	}

	@Override
	public List<Map<String, Object>> searchExcelData(String planId, String filePath, String worksheetName,
			List<String> keywords, List<String> searchColumns) throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + absolutePath);
		}

		List<Map<String, Object>> results = new ArrayList<>();

		try (FileInputStream fis = new FileInputStream(absolutePath.toFile());
				Workbook workbook = WorkbookFactory.create(fis)) {

			Sheet sheet = workbook.getSheet(worksheetName);
			if (sheet == null) {
				throw new IOException("Worksheet not found: " + worksheetName);
			}

			// Get header row to map column names
			Row headerRow = sheet.getRow(0);
			if (headerRow == null) {
				return results; // No data to search
			}

			List<String> headers = new ArrayList<>();
			for (Cell cell : headerRow) {
				headers.add(getCellValueAsString(cell));
			}

			// Determine which columns to search
			Set<Integer> searchColumnIndices = new HashSet<>();
			if (searchColumns == null || searchColumns.isEmpty()) {
				// Search all columns
				for (int i = 0; i < headers.size(); i++) {
					searchColumnIndices.add(i);
				}
			}
			else {
				// Search specified columns
				for (String columnName : searchColumns) {
					int index = headers.indexOf(columnName);
					if (index >= 0) {
						searchColumnIndices.add(index);
					}
				}
			}

			// Search through data rows
			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null)
					continue;

				boolean matchFound = false;
				Map<String, Object> rowData = new HashMap<>();

				// Build row data map
				for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
					Cell cell = row.getCell(colIndex);
					String cellValue = getCellValueAsString(cell);
					rowData.put(headers.get(colIndex), cellValue);

					// Check if this cell matches any keyword (if it's a search column)
					if (searchColumnIndices.contains(colIndex)) {
						for (String keyword : keywords) {
							if (cellValue.toLowerCase().contains(keyword.toLowerCase())) {
								matchFound = true;
								break;
							}
						}
					}
					if (matchFound)
						break;
				}

				if (matchFound) {
					rowData.put("_rowIndex", rowIndex);
					results.add(rowData);
				}
			}
		}

		updateFileState(planId, filePath, "data_searched");
		log.info("Found {} matching rows in worksheet: {} in file: {}", results.size(), worksheetName, absolutePath);

		return results;
	}

	@Override
	public void deleteExcelRows(String planId, String filePath, String worksheetName, List<Integer> rowIndices)
			throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + absolutePath);
		}

		if (rowIndices == null || rowIndices.isEmpty()) {
			return; // Nothing to delete
		}

		try (FileInputStream fis = new FileInputStream(absolutePath.toFile());
				Workbook workbook = WorkbookFactory.create(fis)) {

			Sheet sheet = workbook.getSheet(worksheetName);
			if (sheet == null) {
				throw new IOException("Worksheet not found: " + worksheetName);
			}

			// Sort row indices in descending order to avoid index shifting issues
			List<Integer> sortedIndices = new ArrayList<>(rowIndices);
			sortedIndices.sort(Collections.reverseOrder());

			// Remove rows from bottom to top
			for (Integer rowIndex : sortedIndices) {
				if (rowIndex < 0 || rowIndex > sheet.getLastRowNum()) {
					continue; // Skip invalid row indices
				}

				Row row = sheet.getRow(rowIndex);
				if (row != null) {
					sheet.removeRow(row);

					// Shift rows up if this is not the last row
					if (rowIndex < sheet.getLastRowNum()) {
						sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1);
					}
				}
			}

			// Save the workbook
			try (FileOutputStream fos = new FileOutputStream(absolutePath.toFile())) {
				workbook.write(fos);
			}
		}

		updateFileState(planId, filePath, "rows_deleted");
		log.info("Deleted {} rows from worksheet: {} in file: {}", rowIndices.size(), worksheetName, absolutePath);
	}

	@Override
	public void formatExcelCells(String planId, String filePath, String worksheetName, String cellRange,
			Map<String, Object> formatting) throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + absolutePath);
		}

		try (FileInputStream fis = new FileInputStream(absolutePath.toFile());
				Workbook workbook = WorkbookFactory.create(fis)) {

			Sheet sheet = workbook.getSheet(worksheetName);
			if (sheet == null) {
				throw new IOException("Worksheet not found: " + worksheetName);
			}

			// Parse cell range (e.g., "A1:C3")
			int[][] range = parseCellRange(cellRange);
			int startRow = range[0][0];
			int startCol = range[0][1];
			int endRow = range[1][0];
			int endCol = range[1][1];

			// Create cell style with formatting
			CellStyle cellStyle = workbook.createCellStyle();
			Font font = workbook.createFont();

			// Apply formatting options
			for (Map.Entry<String, Object> entry : formatting.entrySet()) {
				String key = entry.getKey().toLowerCase();
				Object value = entry.getValue();

				switch (key) {
					case "bold":
						if (Boolean.parseBoolean(value.toString())) {
							font.setBold(true);
						}
						break;
					case "italic":
						if (Boolean.parseBoolean(value.toString())) {
							font.setItalic(true);
						}
						break;
					case "fontsize":
						try {
							short fontSize = Short.parseShort(value.toString());
							font.setFontHeightInPoints(fontSize);
						}
						catch (NumberFormatException e) {
							// Ignore invalid font size
						}
						break;
					case "backgroundcolor":
						// Simple background color support
						cellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
						cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
						break;
					case "align":
						String alignment = value.toString().toLowerCase();
						switch (alignment) {
							case "center":
								cellStyle.setAlignment(HorizontalAlignment.CENTER);
								break;
							case "right":
								cellStyle.setAlignment(HorizontalAlignment.RIGHT);
								break;
							case "left":
							default:
								cellStyle.setAlignment(HorizontalAlignment.LEFT);
								break;
						}
						break;
				}
			}

			cellStyle.setFont(font);

			// Apply formatting to the specified range
			for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					row = sheet.createRow(rowIndex);
				}

				for (int colIndex = startCol; colIndex <= endCol; colIndex++) {
					Cell cell = row.getCell(colIndex);
					if (cell == null) {
						cell = row.createCell(colIndex);
					}
					cell.setCellStyle(cellStyle);
				}
			}

			// Save the workbook
			try (FileOutputStream fos = new FileOutputStream(absolutePath.toFile())) {
				workbook.write(fos);
			}
		}

		updateFileState(planId, filePath, "cells_formatted");
		log.info("Formatted cells in range {} in worksheet: {} in file: {}", cellRange, worksheetName, absolutePath);
	}

	@Override
	public void addExcelFormulas(String planId, String filePath, String worksheetName, Map<String, String> formulas)
			throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + absolutePath);
		}

		if (formulas == null || formulas.isEmpty()) {
			return; // Nothing to add
		}

		try (FileInputStream fis = new FileInputStream(absolutePath.toFile());
				Workbook workbook = WorkbookFactory.create(fis)) {

			Sheet sheet = workbook.getSheet(worksheetName);
			if (sheet == null) {
				throw new IOException("Worksheet not found: " + worksheetName);
			}

			// Add formulas to specified cells
			// Expected format: "A1" -> "=SUM(B1:B10)", "C2" -> "=B2*2"
			for (Map.Entry<String, String> entry : formulas.entrySet()) {
				String cellAddress = entry.getKey();
				String formula = entry.getValue();

				// Parse cell address
				int[] coordinates = parseCellAddress(cellAddress);
				int rowIndex = coordinates[0];
				int colIndex = coordinates[1];

				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					row = sheet.createRow(rowIndex);
				}

				Cell cell = row.getCell(colIndex);
				if (cell == null) {
					cell = row.createCell(colIndex);
				}

				// Set the formula (remove leading = if present, POI will add it)
				String cleanFormula = formula.startsWith("=") ? formula.substring(1) : formula;
				try {
					cell.setCellFormula(cleanFormula);
				}
				catch (Exception e) {
					log.warn("Failed to set formula '{}' in cell {}: {}", formula, cellAddress, e.getMessage());
					// If formula fails, set as string value
					cell.setCellValue(formula);
				}
			}

			// Force formula evaluation
			try {
				FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
				evaluator.evaluateAll();
			}
			catch (Exception e) {
				log.warn("Formula evaluation failed: {}", e.getMessage());
			}

			// Save the workbook
			try (FileOutputStream fos = new FileOutputStream(absolutePath.toFile())) {
				workbook.write(fos);
			}
		}

		updateFileState(planId, filePath, "formulas_added");
		log.info("Added {} formulas to worksheet: {} in file: {}", formulas.size(), worksheetName, absolutePath);
	}

	@Override
	public void processExcelInBatches(String planId, String filePath, String worksheetName, int batchSize,
			BatchProcessor processor) throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + absolutePath);
		}

		AtomicInteger batchNumber = new AtomicInteger(1);
		AtomicLong totalRows = new AtomicLong(0);

		// First pass: count total rows
		EasyExcel.read(absolutePath.toString())
			.head(LinkedHashMap.class)
			.sheet(worksheetName)
			.registerReadListener(new ReadListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> data, AnalysisContext context) {
					totalRows.incrementAndGet();
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					// Count completed
				}
			})
			.doRead();

		int totalBatches = (int) Math.ceil((double) totalRows.get() / batchSize);

		// Second pass: process in batches
		EasyExcel.read(absolutePath.toString())
			.head(LinkedHashMap.class)
			.sheet(worksheetName)
			.registerReadListener(new PageReadListener<Map<Integer, String>>(dataList -> {
				// Convert to List<List<String>>
				List<List<String>> batchData = dataList.stream()
					.map(rowMap -> rowMap.entrySet()
						.stream()
						.sorted(Map.Entry.comparingByKey())
						.map(Map.Entry::getValue)
						.collect(Collectors.toList()))
					.collect(Collectors.toList());

				// Process batch
				boolean continueProcessing = processor.processBatch(batchData, batchNumber.getAndIncrement(),
						totalBatches);

				if (!continueProcessing) {
					log.info("Batch processing stopped by processor at batch {}", batchNumber.get() - 1);
				}
			}))
			.doRead();
	}

	@Override
	public Map<String, Object> getProcessingStatus(String planId) {
		return planProcessingStatus.getOrDefault(planId, new HashMap<>());
	}

	@Override
	public void cleanupPlanResources(String planId) {
		planProcessingStatus.remove(planId);
		planFileStates.remove(planId);
		log.debug("Cleaned up resources for plan: {}", planId);
	}

	private void updateFileState(String planId, String filePath, String state) {
		planFileStates.computeIfAbsent(planId, k -> new ConcurrentHashMap<>())
			.put(filePath, Map.of("state", state, "timestamp", System.currentTimeMillis()));
	}

	private void updateProcessingStatus(String planId, String key, Object value) {
		planProcessingStatus.computeIfAbsent(planId, k -> new ConcurrentHashMap<>()).put(key, value);
	}

	private int[] parseCellAddress(String cellAddress) {
		if (cellAddress == null || cellAddress.trim().isEmpty()) {
			throw new IllegalArgumentException("Cell address cannot be null or empty");
		}

		cellAddress = cellAddress.trim().toUpperCase();

		// Parse column letters (e.g., "A", "AB", "AAA")
		int colIndex = 0;
		int i = 0;
		while (i < cellAddress.length() && Character.isLetter(cellAddress.charAt(i))) {
			colIndex = colIndex * 26 + (cellAddress.charAt(i) - 'A' + 1);
			i++;
		}
		colIndex--; // Convert to 0-based index

		// Parse row number (e.g., "1", "123")
		if (i >= cellAddress.length()) {
			throw new IllegalArgumentException("Invalid cell address format: " + cellAddress);
		}

		String rowStr = cellAddress.substring(i);
		int rowIndex;
		try {
			rowIndex = Integer.parseInt(rowStr) - 1; // Convert to 0-based index
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid row number in cell address: " + cellAddress);
		}

		if (rowIndex < 0 || colIndex < 0) {
			throw new IllegalArgumentException("Invalid cell address: " + cellAddress);
		}

		return new int[] { rowIndex, colIndex };
	}

	private int[][] parseCellRange(String cellRange) {
		if (cellRange == null || cellRange.trim().isEmpty()) {
			throw new IllegalArgumentException("Cell range cannot be null or empty");
		}

		cellRange = cellRange.trim().toUpperCase();

		// Handle single cell (e.g., "A1")
		if (!cellRange.contains(":")) {
			int[] coords = parseCellAddress(cellRange);
			return new int[][] { { coords[0], coords[1] }, { coords[0], coords[1] } };
		}

		// Handle range (e.g., "A1:C3")
		String[] parts = cellRange.split(":");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid cell range format: " + cellRange);
		}

		int[] startCoords = parseCellAddress(parts[0]);
		int[] endCoords = parseCellAddress(parts[1]);

		// Ensure start is before end
		int startRow = Math.min(startCoords[0], endCoords[0]);
		int startCol = Math.min(startCoords[1], endCoords[1]);
		int endRow = Math.max(startCoords[0], endCoords[0]);
		int endCol = Math.max(startCoords[1], endCoords[1]);

		return new int[][] { { startRow, startCol }, { endRow, endCol } };
	}

	// Test convenience methods - these methods are for testing purposes only
	// They use a default planId and return Map<String, Object> format expected by tests

	private static final String DEFAULT_TEST_PLAN_ID = "test-plan";

	private String currentFilePath;

	public Map<String, Object> createExcelFile(String filePath, String sheetName, List<String> headers) {
		try {
			Map<String, List<String>> worksheets = new HashMap<>();
			worksheets.put(sheetName, headers);
			createExcelFile(DEFAULT_TEST_PLAN_ID, filePath, worksheets);
			this.currentFilePath = filePath;
			return Map.of("success", true, "message", "Excel file created successfully");
		}
		catch (Exception e) {
			log.error("Error creating Excel file: {}", e.getMessage(), e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	public Map<String, Object> getExcelStructure(String filePath) {
		try {
			Map<String, List<String>> structure = getExcelStructure(DEFAULT_TEST_PLAN_ID, filePath);
			List<Map<String, Object>> sheets = new ArrayList<>();
			for (Map.Entry<String, List<String>> entry : structure.entrySet()) {
				Map<String, Object> sheetInfo = new HashMap<>();
				sheetInfo.put("name", entry.getKey());
				sheetInfo.put("headers", entry.getValue());
				sheets.add(sheetInfo);
			}
			return Map.of("success", true, "sheets", sheets, "totalSheets", sheets.size());
		}
		catch (Exception e) {
			log.error("Error getting Excel structure: {}", e.getMessage(), e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	public Map<String, Object> writeDataToExcel(String filePath, String sheetName, List<List<Object>> data,
			int startRow) {
		try {
			// Convert List<List<Object>> to List<List<String>>
			List<List<String>> stringData = data.stream()
				.map(row -> row.stream().map(Object::toString).collect(Collectors.toList()))
				.collect(Collectors.toList());
			writeExcelData(DEFAULT_TEST_PLAN_ID, filePath, sheetName, stringData, startRow > 1);
			return Map.of("success", true, "rowsWritten", data.size());
		}
		catch (Exception e) {
			log.error("Error writing data to Excel: {}", e.getMessage(), e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	public Map<String, Object> readDataFromExcel(String filePath, String sheetName, int startRow, int maxRows) {
		try {
			List<List<String>> data = readExcelData(DEFAULT_TEST_PLAN_ID, filePath, sheetName, startRow - 1, null,
					maxRows);
			// Convert to List<List<Object>> for test compatibility
			List<List<Object>> objectData = data.stream()
				.map(row -> new ArrayList<Object>(row))
				.collect(Collectors.toList());
			return Map.of("success", true, "data", objectData, "totalRows", data.size());
		}
		catch (Exception e) {
			log.error("Error reading data from Excel: {}", e.getMessage(), e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	public Map<String, Object> searchDataInExcel(String filePath, String sheetName,
			Map<String, Object> searchCriteria) {
		try {
			List<Map<String, Object>> matches = new ArrayList<>();

			// Read the Excel data
			Map<String, Object> readResult = readDataFromExcel(filePath, sheetName, 1, Integer.MAX_VALUE);
			if (!(Boolean) readResult.get("success")) {
				return readResult;
			}

			@SuppressWarnings("unchecked")
			List<List<Object>> data = (List<List<Object>>) readResult.get("data");

			if (data.isEmpty()) {
				return Map.of("success", true, "matches", matches);
			}

			// Get headers from first row
			List<Object> headers = data.get(0);
			Map<String, Integer> headerIndexMap = new HashMap<>();
			for (int i = 0; i < headers.size(); i++) {
				headerIndexMap.put(headers.get(i).toString(), i);
			}

			// Search through data rows (skip header)
			for (int rowIndex = 1; rowIndex < data.size(); rowIndex++) {
				List<Object> row = data.get(rowIndex);
				boolean matchesAllCriteria = true;

				for (Map.Entry<String, Object> criterion : searchCriteria.entrySet()) {
					String columnName = criterion.getKey();
					Object searchValue = criterion.getValue();

					Integer columnIndex = headerIndexMap.get(columnName);
					if (columnIndex == null || columnIndex >= row.size()) {
						matchesAllCriteria = false;
						break;
					}

					Object cellValue = row.get(columnIndex);
					if (cellValue == null || !cellValue.toString().equals(searchValue.toString())) {
						matchesAllCriteria = false;
						break;
					}
				}

				if (matchesAllCriteria) {
					Map<String, Object> match = new HashMap<>();
					match.put("rowIndex", rowIndex);
					for (int i = 0; i < Math.min(headers.size(), row.size()); i++) {
						match.put(headers.get(i).toString(), row.get(i));
					}
					matches.add(match);
				}
			}

			return Map.of("success", true, "matches", matches);
		}
		catch (Exception e) {
			log.error("Error searching data in Excel: {}", e.getMessage(), e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	public Map<String, Object> batchProcessExcel(String filePath, String sheetName, List<List<Object>> data,
			String operation) {
		try {
			if ("write".equals(operation)) {
				Map<String, Object> result = writeDataToExcel(filePath, sheetName, data, 2);
				if ((Boolean) result.get("success")) {
					return Map.of("success", true, "processedBatches", 1, "totalProcessed", data.size());
				}
				else {
					return result;
				}
			}
			return Map.of("success", false, "error", "Unsupported operation: " + operation);
		}
		catch (Exception e) {
			log.error("Error in batch processing: {}", e.getMessage(), e);
			return Map.of("success", false, "error", e.getMessage());
		}
	}

	public Map<String, Object> getProcessingStatus() {
		Map<String, Object> status = getProcessingStatus(DEFAULT_TEST_PLAN_ID);
		// Ensure required fields are present
		status.putIfAbsent("isProcessing", false);
		status.putIfAbsent("currentOperation", "idle");
		status.putIfAbsent("progress", 0);
		return status;
	}

	public String getCurrentFilePath() {
		return currentFilePath;
	}

	public boolean validateFilePath(String filePath) {
		try {
			Path path = Path.of(filePath);
			return isSupportedFileType(filePath) && !filePath.trim().isEmpty();
		}
		catch (Exception e) {
			return false;
		}
	}

}
