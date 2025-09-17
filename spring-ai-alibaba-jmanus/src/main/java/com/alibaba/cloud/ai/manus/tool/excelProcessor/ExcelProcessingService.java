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

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.poi.ss.usermodel.*;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
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

	// Plan processing status tracking
	private final Map<String, Map<String, Object>> planProcessingStatus = new ConcurrentHashMap<>();

	// Plan file states tracking
	private final Map<String, Map<String, Object>> planFileStates = new ConcurrentHashMap<>();

	// Performance metrics tracking
	private final Map<String, Map<String, Object>> performanceMetrics = new ConcurrentHashMap<>();

	// Thread pool for parallel processing
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	// JSON mapper for export
	private final ObjectMapper jsonMapper = new ObjectMapper();

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
		// Delegate to the new method with null headers
		writeExcelDataWithHeaders(planId, filePath, worksheetName, data, null, appendMode);
	}

	@Override
	public void writeExcelDataWithHeaders(String planId, String filePath, String worksheetName, List<List<String>> data,
			List<String> headers, boolean appendMode) throws IOException {
		Path absolutePath = validateFilePath(planId, filePath);

		if (data == null || data.isEmpty()) {
			log.warn("No data to write to Excel file: {}", absolutePath);
			return;
		}

		// Use streaming write for large datasets
		if (data.size() > DEFAULT_BATCH_SIZE) {
			writeLargeExcelDataWithHeaders(planId, absolutePath, worksheetName, data, headers, appendMode);
			return;
		}

		Workbook workbook;
		boolean fileExists = Files.exists(absolutePath);

		// Always try to load existing workbook if file exists to preserve other sheets
		if (fileExists) {
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

			int startRowNum = 0;
			if (appendMode) {
				// In append mode, start after the last row
				startRowNum = sheet.getLastRowNum() + 1;
				// If sheet is empty, getLastRowNum() returns -1, so we start at 0
				if (startRowNum == 0 && sheet.getPhysicalNumberOfRows() == 0) {
					startRowNum = 0;
				}
			}
			else {
				// In overwrite mode, clear existing data in this sheet only
				for (int i = sheet.getLastRowNum(); i >= 0; i--) {
					Row row = sheet.getRow(i);
					if (row != null) {
						sheet.removeRow(row);
					}
				}
				startRowNum = 0;
			}

			// Write headers if provided and not in append mode or if sheet is empty
			if (headers != null && !headers.isEmpty() && (!appendMode || sheet.getPhysicalNumberOfRows() == 0)) {
				Row headerRow = sheet.createRow(startRowNum);
				for (int j = 0; j < headers.size(); j++) {
					Cell cell = headerRow.createCell(j);
					setCellValue(cell, headers.get(j));
				}
				startRowNum++;
			}

			// Write data rows
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
		// Delegate to the new method with null headers
		writeLargeExcelDataWithHeaders(planId, absolutePath, worksheetName, data, null, appendMode);
	}

	private void writeLargeExcelDataWithHeaders(String planId, Path absolutePath, String worksheetName,
			List<List<String>> data, List<String> headers, boolean appendMode) throws IOException {
		// For large data, we need to handle existing workbooks differently
		// SXSSFWorkbook doesn't support reading existing files, so we need a hybrid
		// approach

		boolean fileExists = Files.exists(absolutePath);
		Workbook existingWorkbook = null;
		Map<String, Sheet> existingSheets = new HashMap<>();

		// If file exists and we need to preserve other sheets, read them first
		if (fileExists) {
			try (FileInputStream fis = new FileInputStream(absolutePath.toFile())) {
				existingWorkbook = WorkbookFactory.create(fis);
				// Store all sheets except the target one
				for (int i = 0; i < existingWorkbook.getNumberOfSheets(); i++) {
					Sheet sheet = existingWorkbook.getSheetAt(i);
					if (!sheet.getSheetName().equals(worksheetName)) {
						existingSheets.put(sheet.getSheetName(), sheet);
					}
				}
			}
		}

		// Use SXSSFWorkbook for memory-efficient writing
		try (SXSSFWorkbook workbook = new SXSSFWorkbook(SXSSF_WINDOW_SIZE)) {
			// Copy existing sheets to new workbook (except target sheet)
			if (existingWorkbook != null) {
				for (Map.Entry<String, Sheet> entry : existingSheets.entrySet()) {
					Sheet newSheet = workbook.createSheet(entry.getKey());
					copySheetData(entry.getValue(), newSheet);
				}
			}

			Sheet sheet = workbook.createSheet(worksheetName);
			int currentRowNum = 0;

			// Write headers if provided
			if (headers != null && !headers.isEmpty()) {
				Row headerRow = sheet.createRow(currentRowNum++);
				for (int j = 0; j < headers.size(); j++) {
					Cell cell = headerRow.createCell(j);
					setCellValue(cell, headers.get(j));
				}
			}

			// Write data in batches
			for (int i = 0; i < data.size(); i++) {
				List<String> rowData = data.get(i);
				Row row = sheet.createRow(currentRowNum + i);

				for (int j = 0; j < rowData.size(); j++) {
					Cell cell = row.createCell(j);
					setCellValue(cell, rowData.get(j));
				}
			}

			try (FileOutputStream fos = new FileOutputStream(absolutePath.toFile())) {
				workbook.write(fos);
			}

			// Dispose of temporary files
			workbook.dispose();
		}

		if (existingWorkbook != null) {
			existingWorkbook.close();
		}

		updateFileState(planId, absolutePath.toString(), "large_data_written");
		log.info("Wrote {} rows to worksheet: {} in large file: {}", data.size(), worksheetName, absolutePath);
	}

	/**
	 * Copy data from source sheet to target sheet (for preserving existing sheets)
	 */
	private void copySheetData(Sheet sourceSheet, Sheet targetSheet) {
		for (int i = 0; i <= sourceSheet.getLastRowNum(); i++) {
			Row sourceRow = sourceSheet.getRow(i);
			if (sourceRow != null) {
				Row targetRow = targetSheet.createRow(i);
				for (int j = 0; j < sourceRow.getLastCellNum(); j++) {
					Cell sourceCell = sourceRow.getCell(j);
					if (sourceCell != null) {
						Cell targetCell = targetRow.createCell(j);
						setCellValue(targetCell, getCellValueAsString(sourceCell));
					}
				}
			}
		}
	}

	@Override
	public void processExcelInParallelBatches(String planId, String filePath, String worksheetName, int batchSize,
			int parallelism, BatchProcessor processor) throws IOException {
		long startTime = System.currentTimeMillis();
		initializePerformanceMetrics(planId);

		try {
			// Use optimized streaming approach with producer-consumer pattern
			ExecutorService readerExecutor = Executors.newSingleThreadExecutor();
			ExecutorService processorExecutor = Executors.newFixedThreadPool(parallelism);

			// Thread-safe queue for batches
			java.util.concurrent.BlockingQueue<List<List<String>>> batchQueue = new java.util.concurrent.LinkedBlockingQueue<>(
					parallelism * 2);

			AtomicLong totalRows = new AtomicLong(0);
			AtomicInteger batchCount = new AtomicInteger(0);
			List<CompletableFuture<Void>> processingFutures = new ArrayList<>();

			// Producer: Read data and create batches
			CompletableFuture<Void> readerFuture = CompletableFuture.runAsync(() -> {
				try {
					List<List<String>> currentBatch = new ArrayList<>();

					EasyExcel.read(filePath, new ReadListener<Map<Integer, String>>() {
						@Override
						public void invoke(Map<Integer, String> data, AnalysisContext context) {
							List<String> row = data.values().stream().collect(Collectors.toList());
							currentBatch.add(row);
							totalRows.incrementAndGet();

							if (currentBatch.size() >= batchSize) {
								try {
									batchQueue.put(new ArrayList<>(currentBatch));
									currentBatch.clear();
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
									throw new RuntimeException("Interrupted while queuing batch", e);
								}
							}
						}

						@Override
						public void doAfterAllAnalysed(AnalysisContext context) {
							// Add remaining data as final batch
							if (!currentBatch.isEmpty()) {
								try {
									batchQueue.put(new ArrayList<>(currentBatch));
								}
								catch (InterruptedException e) {
									Thread.currentThread().interrupt();
									throw new RuntimeException("Interrupted while queuing final batch", e);
								}
							}
							// Signal end of data
							try {
								batchQueue.put(Collections.emptyList());
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								throw new RuntimeException("Interrupted while signaling end", e);
							}
						}
					}).sheet(worksheetName).doRead();
				}
				catch (Exception e) {
					throw new RuntimeException("Error reading Excel file", e);
				}
			}, readerExecutor);

			// Consumers: Process batches in parallel
			for (int i = 0; i < parallelism; i++) {
				CompletableFuture<Void> processingFuture = CompletableFuture.runAsync(() -> {
					try {
						while (true) {
							List<List<String>> batch = batchQueue.take();
							if (batch.isEmpty()) {
								// End of data signal
								batchQueue.put(batch); // Re-queue for other consumers
								break;
							}

							int currentBatchNum = batchCount.incrementAndGet();
							int estimatedTotalBatches = (int) Math.ceil((double) totalRows.get() / batchSize);

							processor.processBatch(batch, currentBatchNum,
									Math.max(estimatedTotalBatches, currentBatchNum));
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("Interrupted while processing batches", e);
					}
					catch (Exception e) {
						throw new RuntimeException("Error processing batch", e);
					}
				}, processorExecutor);

				processingFutures.add(processingFuture);
			}

			// Wait for reader to complete
			readerFuture.join();

			// Wait for all processors to complete
			CompletableFuture.allOf(processingFutures.toArray(new CompletableFuture[0])).join();

			// Cleanup
			readerExecutor.shutdown();
			processorExecutor.shutdown();
			readerExecutor.awaitTermination(10, TimeUnit.SECONDS);
			processorExecutor.awaitTermination(30, TimeUnit.SECONDS);

			updatePerformanceMetrics(planId, "optimized_parallel_batch_processing",
					System.currentTimeMillis() - startTime, totalRows.get(), parallelism);

			log.info("Processed {} rows in {} batches using {} parallel threads", totalRows.get(), batchCount.get(),
					parallelism);

		}
		catch (Exception e) {
			throw new IOException("Failed to process Excel in parallel batches: " + e.getMessage(), e);
		}
	}

	@Override
	public <T, R> R transformAndAggregateExcelData(String planId, String filePath, String worksheetName,
			DataTransformer<T> transformer, DataAggregator<T, R> aggregator) throws IOException {
		long startTime = System.currentTimeMillis();
		initializePerformanceMetrics(planId);

		try {
			List<T> transformedData = new ArrayList<>();
			AtomicInteger rowIndex = new AtomicInteger(0);

			EasyExcel.read(filePath, new ReadListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> data, AnalysisContext context) {
					List<String> rowData = data.values().stream().collect(Collectors.toList());
					T transformed = transformer.transform(rowData, rowIndex.getAndIncrement());
					if (transformed != null) {
						transformedData.add(transformed);
					}
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					// Analysis complete
				}
			}).sheet(worksheetName).doRead();

			R result = aggregator.aggregate(transformedData);
			updatePerformanceMetrics(planId, "transform_aggregate", System.currentTimeMillis() - startTime,
					transformedData.size(), 1);
			return result;
		}
		catch (Exception e) {
			throw new IOException("Failed to transform and aggregate Excel data: " + e.getMessage(), e);
		}
	}

	@Override
	public void streamProcessExcelData(String planId, String filePath, String worksheetName,
			IExcelProcessingService.StreamProcessor streamProcessor) throws IOException {
		long startTime = System.currentTimeMillis();
		initializePerformanceMetrics(planId);

		try {
			AtomicInteger rowIndex = new AtomicInteger(0);
			AtomicLong processedRows = new AtomicLong(0);

			EasyExcel.read(filePath, new ReadListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> data, AnalysisContext context) {
					List<String> rowData = data.values().stream().collect(Collectors.toList());
					boolean continueProcessing = streamProcessor.processRow(rowData, rowIndex.getAndIncrement());
					processedRows.incrementAndGet();

					if (!continueProcessing) {
						context.interrupt();
					}
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					// Analysis complete
				}
			}).sheet(worksheetName).doRead();

			updatePerformanceMetrics(planId, "stream_processing", System.currentTimeMillis() - startTime,
					processedRows.get(), 1);
		}
		catch (Exception e) {
			throw new IOException("Failed to stream process Excel data: " + e.getMessage(), e);
		}
	}

	@Override
	public Map<String, Object> validateAndCleanExcelData(String planId, String filePath, String worksheetName,
			DataValidator validator, DataCleaner cleaner) throws IOException {
		long startTime = System.currentTimeMillis();
		initializePerformanceMetrics(planId);

		try {
			List<ValidationResult> validationResults = new ArrayList<>();
			List<List<String>> cleanedData = new ArrayList<>();
			AtomicInteger rowIndex = new AtomicInteger(0);
			AtomicInteger validRows = new AtomicInteger(0);
			AtomicInteger invalidRows = new AtomicInteger(0);

			EasyExcel.read(filePath, new ReadListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> data, AnalysisContext context) {
					List<String> rowData = data.values().stream().collect(Collectors.toList());
					int currentRowIndex = rowIndex.getAndIncrement();

					// Validate data
					ValidationResult validationResult = validator.validate(rowData, currentRowIndex);
					validationResults.add(validationResult);

					if (validationResult.isValid()) {
						validRows.incrementAndGet();
						// Clean data
						List<String> cleanedRow = cleaner.clean(rowData, currentRowIndex);
						cleanedData.add(cleanedRow);
					}
					else {
						invalidRows.incrementAndGet();
					}
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					// Analysis complete
				}
			}).sheet(worksheetName).doRead();

			// Generate report
			Map<String, Object> report = new HashMap<>();
			report.put("total_rows", rowIndex.get());
			report.put("valid_rows", validRows.get());
			report.put("invalid_rows", invalidRows.get());
			report.put("validation_rate", (double) validRows.get() / rowIndex.get());
			report.put("validation_results", validationResults);
			report.put("cleaned_data", cleanedData);
			report.put("processing_time_ms", System.currentTimeMillis() - startTime);

			updatePerformanceMetrics(planId, "validate_clean", System.currentTimeMillis() - startTime, rowIndex.get(),
					1);
			return report;
		}
		catch (Exception e) {
			throw new IOException("Failed to validate and clean Excel data: " + e.getMessage(), e);
		}
	}

	@Override
	public void exportExcelData(String planId, String filePath, String worksheetName, String outputPath,
			ExportFormat format, Map<String, Object> options) throws IOException {
		long startTime = System.currentTimeMillis();
		initializePerformanceMetrics(planId);

		try {
			List<List<String>> allData = new ArrayList<>();
			AtomicInteger rowCount = new AtomicInteger(0);

			// Read all data first
			EasyExcel.read(filePath, new ReadListener<Map<Integer, String>>() {
				@Override
				public void invoke(Map<Integer, String> data, AnalysisContext context) {
					List<String> rowData = data.values().stream().collect(Collectors.toList());
					allData.add(rowData);
					rowCount.incrementAndGet();
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					// Analysis complete
				}
			}).sheet(worksheetName).doRead();

			// Export based on format
			switch (format) {
				case CSV:
					exportToCSV(allData, outputPath, options);
					break;
				case TSV:
					exportToTSV(allData, outputPath, options);
					break;
				case JSON:
					exportToJSON(allData, outputPath, options);
					break;
				case XML:
					exportToXML(allData, outputPath, options);
					break;
				default:
					throw new IllegalArgumentException("Unsupported export format: " + format);
			}

			updatePerformanceMetrics(planId, "export_" + format.name().toLowerCase(),
					System.currentTimeMillis() - startTime, rowCount.get(), 1);
		}
		catch (Exception e) {
			throw new IOException("Failed to export Excel data: " + e.getMessage(), e);
		}
	}

	@Override
	public Map<String, Object> getPerformanceMetrics(String planId) {
		return performanceMetrics.getOrDefault(planId, new HashMap<>());
	}

	/**
	 * Initialize performance metrics for a plan
	 */
	private void initializePerformanceMetrics(String planId) {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("start_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		metrics.put("operations", new ArrayList<Map<String, Object>>());
		metrics.put("memory_usage_mb", getMemoryUsage());
		performanceMetrics.put(planId, metrics);
	}

	/**
	 * Update performance metrics for an operation
	 */
	@SuppressWarnings("unchecked")
	private void updatePerformanceMetrics(String planId, String operation, long durationMs, long rowsProcessed,
			int parallelism) {
		Map<String, Object> metrics = performanceMetrics.get(planId);
		if (metrics != null) {
			List<Map<String, Object>> operations = (List<Map<String, Object>>) metrics.get("operations");
			Map<String, Object> operationMetrics = new HashMap<>();
			operationMetrics.put("operation", operation);
			operationMetrics.put("duration_ms", durationMs);
			operationMetrics.put("rows_processed", rowsProcessed);
			operationMetrics.put("parallelism", parallelism);
			operationMetrics.put("rows_per_second", rowsProcessed * 1000.0 / durationMs);
			operationMetrics.put("memory_usage_mb", getMemoryUsage());
			operationMetrics.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			operations.add(operationMetrics);
		}
	}

	/**
	 * Get current memory usage in MB
	 */
	private long getMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
	}

	/**
	 * Enhanced batch processing with memory monitoring and adaptive batch sizing
	 */
	private void processExcelWithAdaptiveBatching(String planId, String filePath, String worksheetName,
			int initialBatchSize, int maxParallelism, BatchProcessor processor) throws IOException {
		long startTime = System.currentTimeMillis();
		initializePerformanceMetrics(planId);

		try {
			Runtime runtime = Runtime.getRuntime();
			long maxMemory = runtime.maxMemory() / (1024 * 1024); // Convert to MB
			long memoryThreshold = (long) (maxMemory * 0.8); // Use 80% of max memory

			AtomicInteger currentBatchSize = new AtomicInteger(initialBatchSize);
			AtomicInteger currentParallelism = new AtomicInteger(
					Math.min(maxParallelism, Runtime.getRuntime().availableProcessors()));
			AtomicLong totalRows = new AtomicLong(0);
			AtomicInteger batchCount = new AtomicInteger(0);
			AtomicBoolean memoryPressure = new AtomicBoolean(false);

			// Memory monitoring thread
			ExecutorService memoryMonitor = Executors.newSingleThreadExecutor();
			CompletableFuture<Void> memoryMonitorFuture = CompletableFuture.runAsync(() -> {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						long currentMemory = getMemoryUsage();
						if (currentMemory > memoryThreshold) {
							// Reduce batch size and parallelism
							currentBatchSize.set(Math.max(100, currentBatchSize.get() / 2));
							currentParallelism.set(Math.max(1, currentParallelism.get() - 1));
							memoryPressure.set(true);
							log.warn(
									"High memory usage detected: {} MB. Reducing batch size to {} and parallelism to {}",
									currentMemory, currentBatchSize.get(), currentParallelism.get());

							// Force garbage collection
							System.gc();
						}
						else if (currentMemory < memoryThreshold * 0.5 && !memoryPressure.get()) {
							// Increase batch size if memory usage is low and no pressure
							currentBatchSize.set(Math.min(initialBatchSize * 2, currentBatchSize.get() + 200));
							currentParallelism.set(Math.min(maxParallelism, currentParallelism.get() + 1));
						}
						Thread.sleep(1000); // Check every second
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}, memoryMonitor);

			// Enhanced batch processor with memory monitoring
			BatchProcessor enhancedProcessor = new BatchProcessor() {
				@Override
				public boolean processBatch(List<List<String>> batchData, int batchNumber, int totalBatches) {
					// Monitor memory before processing
					long memoryBefore = getMemoryUsage();

					boolean result = processor.processBatch(batchData, batchNumber, totalBatches);

					// Log memory usage after processing
					long memoryAfter = getMemoryUsage();
					long memoryDelta = memoryAfter - memoryBefore;

					if (batchNumber % 10 == 0) { // Log every 10 batches
						log.info("Batch {}/{}: Memory usage {} MB (delta: {} MB), Batch size: {}, Parallelism: {}",
								batchNumber, totalBatches, memoryAfter, memoryDelta, batchData.size(),
								currentParallelism.get());
					}

					return result;
				}
			};

			// Use the optimized parallel processing with adaptive parameters
			processExcelInParallelBatches(planId, filePath, worksheetName, currentBatchSize.get(),
					currentParallelism.get(), enhancedProcessor);

			// Stop memory monitoring
			memoryMonitorFuture.cancel(true);
			memoryMonitor.shutdown();
			memoryMonitor.awaitTermination(5, TimeUnit.SECONDS);

			updatePerformanceMetrics(planId, "adaptive_batch_processing", System.currentTimeMillis() - startTime,
					totalRows.get(), currentParallelism.get());

			log.info("Adaptive batch processing completed. Final batch size: {}, Final parallelism: {}",
					currentBatchSize.get(), currentParallelism.get());

		}
		catch (Exception e) {
			throw new IOException("Failed to process Excel with adaptive batching: " + e.getMessage(), e);
		}
	}

	/**
	 * Stream processing for very large Excel files with real-time processing
	 */
	private void streamProcessExcel(String planId, String filePath, String worksheetName,
			IExcelProcessingService.StreamProcessor streamProcessor) throws IOException {
		long startTime = System.currentTimeMillis();
		initializePerformanceMetrics(planId);

		try {
			AtomicLong processedRows = new AtomicLong(0);
			AtomicLong totalMemoryUsed = new AtomicLong(0);
			AtomicInteger currentBatchSize = new AtomicInteger(500); // Start with smaller
																		// batches for
																		// streaming

			// Create a streaming listener that processes data in real-time
			ReadListener<List<String>> streamingListener = new ReadListener<List<String>>() {
				private List<List<String>> currentBatch = new ArrayList<>();

				private long lastMemoryCheck = System.currentTimeMillis();

				@Override
				public void invoke(List<String> data, AnalysisContext context) {
					currentBatch.add(new ArrayList<>(data));
					processedRows.incrementAndGet();

					// Process batch when it reaches the current batch size
					if (currentBatch.size() >= currentBatchSize.get()) {
						processBatchInStream();
					}

					// Check memory usage every 1000 rows
					if (processedRows.get() % 1000 == 0) {
						checkAndAdjustMemory();
					}
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					// Process remaining data
					if (!currentBatch.isEmpty()) {
						processBatchInStream();
					}

					log.info("Stream processing completed. Total rows processed: {}", processedRows.get());
				}

				private void processBatchInStream() {
					try {
						long memoryBefore = getMemoryUsage();

						// Process each row in the batch using the provided stream
						// processor
						for (List<String> row : currentBatch) {
							streamProcessor.processRow(row, (int) processedRows.incrementAndGet());
						}

						long memoryAfter = getMemoryUsage();
						totalMemoryUsed.addAndGet(memoryAfter - memoryBefore);

						// Clear the batch to free memory
						currentBatch.clear();

						// Log progress every 50 batches
						if ((processedRows.get() / currentBatchSize.get()) % 50 == 0) {
							log.info("Streaming progress: {} rows processed, current memory: {} MB",
									processedRows.get(), memoryAfter);
						}

					}
					catch (Exception e) {
						log.error("Error processing stream batch: {}", e.getMessage(), e);
						throw new RuntimeException("Stream processing failed", e);
					}
				}

				private void checkAndAdjustMemory() {
					long currentTime = System.currentTimeMillis();
					if (currentTime - lastMemoryCheck > 5000) { // Check every 5 seconds
						long currentMemory = getMemoryUsage();
						Runtime runtime = Runtime.getRuntime();
						long maxMemory = runtime.maxMemory() / (1024 * 1024);

						if (currentMemory > maxMemory * 0.8) {
							// Reduce batch size if memory usage is high
							currentBatchSize.set(Math.max(100, currentBatchSize.get() / 2));
							log.warn("High memory usage detected: {} MB. Reducing batch size to {}", currentMemory,
									currentBatchSize.get());
							System.gc(); // Suggest garbage collection
						}
						else if (currentMemory < maxMemory * 0.4) {
							// Increase batch size if memory usage is low
							currentBatchSize.set(Math.min(2000, currentBatchSize.get() + 100));
						}

						lastMemoryCheck = currentTime;
					}
				}
			};

			// Start streaming processing
			EasyExcel.read(filePath, streamingListener).sheet(worksheetName).doRead();

			updatePerformanceMetrics(planId, "stream_processing", System.currentTimeMillis() - startTime,
					processedRows.get(), 1);

		}
		catch (Exception e) {
			throw new IOException("Failed to stream process Excel file: " + e.getMessage(), e);
		}
	}

	/**
	 * Interface for stream processing callbacks
	 */

	/**
	 * Enhanced memory management for processing very large datasets
	 */
	private void processExcelWithMemoryOptimization(String planId, String filePath, String worksheetName,
			MemoryOptimizedProcessor processor) throws IOException {
		long startTime = System.currentTimeMillis();
		initializePerformanceMetrics(planId);

		try {
			Runtime runtime = Runtime.getRuntime();
			long maxMemory = runtime.maxMemory() / (1024 * 1024); // Convert to MB
			long memoryThreshold = (long) (maxMemory * 0.7); // Use 70% of max memory as
																// threshold

			AtomicLong processedRows = new AtomicLong(0);
			AtomicInteger dynamicBatchSize = new AtomicInteger(200); // Start with small
																		// batch
			AtomicBoolean memoryPressureMode = new AtomicBoolean(false);

			// Memory monitoring and cleanup service
			ExecutorService memoryService = Executors.newSingleThreadExecutor();
			CompletableFuture<Void> memoryMonitorTask = CompletableFuture.runAsync(() -> {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						long currentMemory = getMemoryUsage();

						if (currentMemory > memoryThreshold) {
							memoryPressureMode.set(true);
							dynamicBatchSize.set(Math.max(50, dynamicBatchSize.get() / 2));

							// Aggressive garbage collection
							System.gc();
							Thread.sleep(100); // Give GC time to work
							System.runFinalization();

							log.warn("Memory pressure detected: {} MB / {} MB. Reduced batch size to {}", currentMemory,
									maxMemory, dynamicBatchSize.get());
						}
						else if (currentMemory < memoryThreshold * 0.5 && !memoryPressureMode.get()) {
							// Gradually increase batch size when memory is available
							dynamicBatchSize.set(Math.min(1000, dynamicBatchSize.get() + 50));
						}
						else if (currentMemory < memoryThreshold * 0.6) {
							memoryPressureMode.set(false);
						}

						Thread.sleep(2000); // Check every 2 seconds
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}, memoryService);

			// Memory-optimized data processing
			ReadListener<List<String>> memoryOptimizedListener = new ReadListener<List<String>>() {
				private List<List<String>> currentBatch = new ArrayList<>();

				private long lastGcTime = System.currentTimeMillis();

				private int consecutiveMemoryWarnings = 0;

				@Override
				public void invoke(List<String> data, AnalysisContext context) {
					// Create a defensive copy to avoid memory leaks
					List<String> rowCopy = new ArrayList<>(data.size());
					for (String cell : data) {
						rowCopy.add(cell != null ? cell.intern() : null); // Use string
																			// interning
																			// for memory
																			// efficiency
					}
					currentBatch.add(rowCopy);
					processedRows.incrementAndGet();

					// Process batch when it reaches dynamic size or memory pressure
					if (currentBatch.size() >= dynamicBatchSize.get()
							|| (memoryPressureMode.get() && currentBatch.size() >= 25)) {
						processMemoryOptimizedBatch();
					}

					// Periodic memory check and cleanup
					if (processedRows.get() % 500 == 0) {
						performMemoryMaintenance();
					}
				}

				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
					// Process any remaining data
					if (!currentBatch.isEmpty()) {
						processMemoryOptimizedBatch();
					}

					// Final cleanup
					currentBatch = null;
					System.gc();

					log.info("Memory-optimized processing completed. Total rows: {}, Final batch size: {}",
							processedRows.get(), dynamicBatchSize.get());
				}

				private void processMemoryOptimizedBatch() {
					try {
						long memoryBefore = getMemoryUsage();

						// Process with memory monitoring
						processor.processWithMemoryOptimization(new ArrayList<>(currentBatch), processedRows.get(),
								memoryBefore, memoryPressureMode.get());

						long memoryAfter = getMemoryUsage();

						// Clear batch immediately to free memory
						currentBatch.clear();

						// Log memory usage periodically
						if (processedRows.get() % 5000 == 0) {
							log.info("Memory usage: {} MB -> {} MB, Batch size: {}, Pressure mode: {}", memoryBefore,
									memoryAfter, dynamicBatchSize.get(), memoryPressureMode.get());
						}

						// Check for memory leaks
						if (memoryAfter > memoryBefore + 50) { // Memory increased by more
																// than 50MB
							consecutiveMemoryWarnings++;
							if (consecutiveMemoryWarnings > 3) {
								log.warn("Potential memory leak detected. Forcing aggressive cleanup.");
								System.gc();
								System.runFinalization();
								consecutiveMemoryWarnings = 0;
							}
						}
						else {
							consecutiveMemoryWarnings = 0;
						}

					}
					catch (Exception e) {
						log.error("Error in memory-optimized batch processing: {}", e.getMessage(), e);
						throw new RuntimeException("Memory-optimized processing failed", e);
					}
				}

				private void performMemoryMaintenance() {
					long currentTime = System.currentTimeMillis();
					if (currentTime - lastGcTime > 10000) { // Every 10 seconds
						long memoryBefore = getMemoryUsage();
						System.gc();
						long memoryAfter = getMemoryUsage();

						if (memoryBefore - memoryAfter > 10) { // If GC freed more than
																// 10MB
							log.debug("Memory maintenance: freed {} MB", memoryBefore - memoryAfter);
						}

						lastGcTime = currentTime;
					}
				}
			};

			// Start processing with memory optimization
			EasyExcel.read(filePath, memoryOptimizedListener).sheet(worksheetName).doRead();

			// Stop memory monitoring
			memoryMonitorTask.cancel(true);
			memoryService.shutdown();
			memoryService.awaitTermination(5, TimeUnit.SECONDS);

			updatePerformanceMetrics(planId, "memory_optimized_processing", System.currentTimeMillis() - startTime,
					processedRows.get(), 1);

		}
		catch (Exception e) {
			throw new IOException("Failed to process Excel with memory optimization: " + e.getMessage(), e);
		}
	}

	/**
	 * Interface for memory-optimized processing callbacks
	 */
	public interface MemoryOptimizedProcessor {

		void processWithMemoryOptimization(List<List<String>> batchData, long totalProcessedRows,
				long currentMemoryUsage, boolean memoryPressureMode);

	}

	/**
	 * Export data to CSV format
	 */
	private void exportToCSV(List<List<String>> data, String outputPath, Map<String, Object> options)
			throws IOException {
		String delimiter = (String) options.getOrDefault("delimiter", ",");
		boolean includeHeaders = (Boolean) options.getOrDefault("include_headers", true);

		try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
			for (int i = 0; i < data.size(); i++) {
				if (i == 0 && !includeHeaders) {
					continue;
				}
				List<String> row = data.get(i);
				writer.println(String.join(delimiter, row));
			}
		}
	}

	/**
	 * Export data to TSV format
	 */
	private void exportToTSV(List<List<String>> data, String outputPath, Map<String, Object> options)
			throws IOException {
		Map<String, Object> tsvOptions = new HashMap<>(options);
		tsvOptions.put("delimiter", "\t");
		exportToCSV(data, outputPath, tsvOptions);
	}

	/**
	 * Export data to JSON format
	 */
	private void exportToJSON(List<List<String>> data, String outputPath, Map<String, Object> options)
			throws IOException {
		boolean includeHeaders = (Boolean) options.getOrDefault("include_headers", true);
		String arrayName = (String) options.getOrDefault("array_name", "data");

		List<String> headers = null;
		List<List<String>> dataRows = data;

		if (includeHeaders && !data.isEmpty()) {
			headers = data.get(0);
			dataRows = data.subList(1, data.size());
		}

		List<Map<String, String>> jsonData = new ArrayList<>();
		for (List<String> row : dataRows) {
			Map<String, String> rowMap = new HashMap<>();
			for (int i = 0; i < row.size(); i++) {
				String key = (headers != null && i < headers.size()) ? headers.get(i) : "column_" + i;
				rowMap.put(key, row.get(i));
			}
			jsonData.add(rowMap);
		}

		Map<String, Object> result = new HashMap<>();
		result.put(arrayName, jsonData);

		jsonMapper.writeValue(new File(outputPath), result);
	}

	/**
	 * Export data to XML format
	 */
	private void exportToXML(List<List<String>> data, String outputPath, Map<String, Object> options)
			throws IOException {
		boolean includeHeaders = (Boolean) options.getOrDefault("include_headers", true);
		String rootElement = (String) options.getOrDefault("root_element", "data");
		String rowElement = (String) options.getOrDefault("row_element", "row");

		List<String> headers = null;
		List<List<String>> dataRows = data;

		if (includeHeaders && !data.isEmpty()) {
			headers = data.get(0);
			dataRows = data.subList(1, data.size());
		}

		List<Map<String, String>> xmlData = new ArrayList<>();
		for (List<String> row : dataRows) {
			Map<String, String> rowMap = new HashMap<>();
			for (int i = 0; i < row.size(); i++) {
				String key = (headers != null && i < headers.size()) ? headers.get(i) : "column_" + i;
				rowMap.put(key, row.get(i));
			}
			xmlData.add(rowMap);
		}

		try (FileWriter writer = new FileWriter(outputPath)) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<" + rootElement + ">\n");
			for (Map<String, String> row : xmlData) {
				writer.write("  <" + rowElement + ">\n");
				for (Map.Entry<String, String> entry : row.entrySet()) {
					String key = entry.getKey().replaceAll("[^a-zA-Z0-9_]", "_");
					String value = entry.getValue() != null
							? entry.getValue().replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
							: "";
					writer.write("    <" + key + ">" + value + "</" + key + ">\n");
				}
				writer.write("  </" + rowElement + ">\n");
			}
			writer.write("</" + rootElement + ">\n");
		}
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
