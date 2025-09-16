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
package com.alibaba.cloud.ai.example.manus.task.example;

import com.alibaba.cloud.ai.example.manus.context.ContextKey;
import com.alibaba.cloud.ai.example.manus.context.JManusExecutionContext;
import com.alibaba.cloud.ai.example.manus.task.StatefulTask;
import com.alibaba.cloud.ai.example.manus.task.TaskExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataProcessingTask implements StatefulTask {

	private static final Logger logger = LoggerFactory.getLogger(DataProcessingTask.class);

	// Context keys for input and output data
	public static final ContextKey<String> INPUT_DATA = ContextKey.of("input.data", String.class);

	public static final ContextKey<Map<String, Object>> PROCESSING_CONFIG = ContextKey.ofGeneric("processing.config",
			Map.class);

	public static final ContextKey<ProcessedResult> PROCESSED_RESULT = ContextKey.of("processed.result",
			ProcessedResult.class);

	public static final ContextKey<List<String>> PROCESSING_STEPS = ContextKey.ofGeneric("processing.steps",
			List.class);

	@Override
	public void execute(JManusExecutionContext context) throws TaskExecutionException {
		try {
			logger.info("Starting data processing task for plan: {}", context.getPlanId());

			// Read input data from context
			String inputData = context.getOrDefault(INPUT_DATA, "");
			if (inputData.trim().isEmpty()) {
				throw new TaskExecutionException(getName(), context.getPlanId(), "Input data is empty or not provided");
			}

			// Read processing configuration (optional)
			Map<String, Object> config = context.get(PROCESSING_CONFIG).orElse(null);
			boolean enableAdvancedProcessing = config != null && Boolean.TRUE.equals(config.get("enableAdvanced"));

			// Simulate data processing
			ProcessedResult result = processData(inputData, enableAdvancedProcessing);

			// Store processing steps for observability
			List<String> steps = List.of("Input validation completed",
					enableAdvancedProcessing ? "Advanced processing applied" : "Basic processing applied",
					"Data transformation completed", "Result validation completed");

			// Store results in context for next tasks
			context.put(PROCESSED_RESULT, result);
			context.put(PROCESSING_STEPS, steps);

			// Add metadata for debugging
			context.putMetadata("processing.timestamp", System.currentTimeMillis());
			context.putMetadata("processing.inputLength", inputData.length());
			context.putMetadata("processing.outputSize", result.getProcessedData().length());

			logger.info("Data processing completed successfully for plan: {}, processed {} characters",
					context.getPlanId(), inputData.length());

		}
		catch (Exception e) {
			if (e instanceof TaskExecutionException) {
				throw e;
			}

			String error = String.format("Unexpected error during data processing: %s", e.getMessage());
			throw new TaskExecutionException(getName(), context.getPlanId(), error, e);
		}
	}

	/**
	 * Simulates data processing operations.
	 * @param inputData The input data to process
	 * @param enableAdvanced Whether to enable advanced processing
	 * @return The processed result
	 */
	private ProcessedResult processData(String inputData, boolean enableAdvanced) {
		// Simulate processing delay
		try {
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		String processed;
		if (enableAdvanced) {
			// Advanced processing: uppercase and add metadata
			processed = inputData.toUpperCase() + " [ADVANCED_PROCESSED]";
		}
		else {
			// Basic processing: just add a suffix
			processed = inputData + " [PROCESSED]";
		}

		return new ProcessedResult(processed, inputData.length(), enableAdvanced);
	}

	@Override
	public String getName() {
		return "DataProcessingTask";
	}

	@Override
	public String getDescription() {
		return "Processes input data and stores intermediate results for subsequent workflow steps";
	}

	@Override
	public boolean validateContext(JManusExecutionContext context) {
		if (context == null) {
			return false;
		}

		// Check if required input is available
		String inputData = context.getOrDefault(INPUT_DATA, "");
		return !inputData.trim().isEmpty();
	}

	@Override
	public long getExpectedExecutionTimeMs() {
		return 500; // Expected to complete within 500ms
	}

	@Override
	public boolean isRetryable() {
		return true;
	}

	@Override
	public int getMaxRetryAttempts() {
		return 2;
	}

	/**
	 * Result of data processing operations.
	 */
	public static class ProcessedResult {

		private final String processedData;

		private final int originalLength;

		private final boolean advancedProcessing;

		private final long timestamp;

		public ProcessedResult(String processedData, int originalLength, boolean advancedProcessing) {
			this.processedData = processedData;
			this.originalLength = originalLength;
			this.advancedProcessing = advancedProcessing;
			this.timestamp = System.currentTimeMillis();
		}

		public String getProcessedData() {
			return processedData;
		}

		public int getOriginalLength() {
			return originalLength;
		}

		public boolean isAdvancedProcessing() {
			return advancedProcessing;
		}

		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public String toString() {
			return String.format("ProcessedResult{data='%s', originalLength=%d, advanced=%b}", processedData,
					originalLength, advancedProcessing);
		}

	}

}
