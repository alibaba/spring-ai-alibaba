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
import com.alibaba.cloud.ai.example.manus.task.example.DataProcessingTask.ProcessedResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportGenerationTask implements StatefulTask {

	private static final Logger logger = LoggerFactory.getLogger(ReportGenerationTask.class);

	// Context keys for input and output data
	public static final ContextKey<String> REPORT_TEMPLATE = ContextKey.of("report.template", String.class);

	public static final ContextKey<ExecutionReport> FINAL_REPORT = ContextKey.of("final.report", ExecutionReport.class);

	@Override
	public void execute(JManusExecutionContext context) throws TaskExecutionException {
		try {
			logger.info("Starting report generation task for plan: {}", context.getPlanId());

			// Read processed result from previous task
			ProcessedResult processedResult = context.get(DataProcessingTask.PROCESSED_RESULT)
				.orElseThrow(() -> new TaskExecutionException(getName(), context.getPlanId(),
						"Processed result not found in context - DataProcessingTask may have failed"));

			// Read processing steps for detailed reporting
			List<String> processingSteps = context.get(DataProcessingTask.PROCESSING_STEPS)
				.orElse(List.of("No processing steps recorded"));

			// Read optional report template
			String template = context.getOrDefault(REPORT_TEMPLATE, "DEFAULT_TEMPLATE");

			// Generate comprehensive report
			ExecutionReport report = generateReport(processedResult, processingSteps, template, context);

			// Store final report in context
			context.put(FINAL_REPORT, report);

			// Add metadata for debugging
			context.putMetadata("report.generatedAt", System.currentTimeMillis());
			context.putMetadata("report.size", report.getContent().length());
			context.putMetadata("report.template", template);

			logger.info("Report generation completed successfully for plan: {}, generated {} characters",
					context.getPlanId(), report.getContent().length());

		}
		catch (Exception e) {
			if (e instanceof TaskExecutionException) {
				throw e;
			}

			String error = String.format("Unexpected error during report generation: %s", e.getMessage());
			throw new TaskExecutionException(getName(), context.getPlanId(), error, e);
		}
	}

	/**
	 * Generates a comprehensive execution report.
	 * @param processedResult The result from the data processing task
	 * @param processingSteps The steps that were executed
	 * @param template The report template to use
	 * @param context The execution context for additional information
	 * @return The generated execution report
	 */
	private ExecutionReport generateReport(ProcessedResult processedResult, List<String> processingSteps,
			String template, JManusExecutionContext context) {

		StringBuilder reportContent = new StringBuilder();

		// Header
		reportContent.append("=== JManus Execution Report ===\n");
		reportContent.append("Plan ID: ").append(context.getPlanId()).append("\n");
		reportContent.append("Generated At: ").append(context.getCreatedAt()).append("\n");
		reportContent.append("Template: ").append(template).append("\n\n");

		// Processing Summary
		reportContent.append("--- Processing Summary ---\n");
		reportContent.append("Original Data Length: ")
			.append(processedResult.getOriginalLength())
			.append(" characters\n");
		reportContent.append("Processed Data Length: ")
			.append(processedResult.getProcessedData().length())
			.append(" characters\n");
		reportContent.append("Advanced Processing: ")
			.append(processedResult.isAdvancedProcessing() ? "YES" : "NO")
			.append("\n");
		reportContent.append("Processing Timestamp: ").append(processedResult.getTimestamp()).append("\n\n");

		// Processing Steps
		reportContent.append("--- Processing Steps ---\n");
		for (int i = 0; i < processingSteps.size(); i++) {
			reportContent.append(i + 1).append(". ").append(processingSteps.get(i)).append("\n");
		}
		reportContent.append("\n");

		// Context State Summary
		reportContent.append("--- Context State Summary ---\n");
		reportContent.append("Context Size: ").append(context.size()).append(" entries\n");
		reportContent.append("Metadata Count: ").append(context.getAllMetadata().size()).append(" entries\n");

		// Key Context Data
		if (!context.isEmpty()) {
			reportContent.append("\n--- Key Context Data ---\n");
			context.keySet().forEach(key -> {
				reportContent.append("- ")
					.append(key.getName())
					.append(" (")
					.append(key.getType().getSimpleName())
					.append(")")
					.append("\n");
			});
		}

		// Final Result
		reportContent.append("\n--- Final Processed Data ---\n");
		reportContent.append(processedResult.getProcessedData()).append("\n");

		// Footer
		reportContent.append("\n=== End of Report ===");

		return new ExecutionReport(context.getPlanId(), reportContent.toString(), processedResult.getOriginalLength(),
				processedResult.getProcessedData().length(), processingSteps.size(), template);
	}

	@Override
	public String getName() {
		return "ReportGenerationTask";
	}

	@Override
	public String getDescription() {
		return "Generates a comprehensive execution report from processed workflow data";
	}

	@Override
	public boolean validateContext(JManusExecutionContext context) {
		if (context == null) {
			return false;
		}

		// Check if required processed result is available
		return context.containsKey(DataProcessingTask.PROCESSED_RESULT);
	}

	@Override
	public long getExpectedExecutionTimeMs() {
		return 200; // Expected to complete within 200ms
	}

	@Override
	public boolean isRetryable() {
		return true;
	}

	@Override
	public int getMaxRetryAttempts() {
		return 1; // Report generation is typically deterministic
	}

	/**
	 * Final execution report containing comprehensive workflow information.
	 */
	public static class ExecutionReport {

		private final String planId;

		private final String content;

		private final int originalDataLength;

		private final int processedDataLength;

		private final int processingStepsCount;

		private final String template;

		private final long timestamp;

		public ExecutionReport(String planId, String content, int originalDataLength, int processedDataLength,
				int processingStepsCount, String template) {
			this.planId = planId;
			this.content = content;
			this.originalDataLength = originalDataLength;
			this.processedDataLength = processedDataLength;
			this.processingStepsCount = processingStepsCount;
			this.template = template;
			this.timestamp = System.currentTimeMillis();
		}

		public String getPlanId() {
			return planId;
		}

		public String getContent() {
			return content;
		}

		public int getOriginalDataLength() {
			return originalDataLength;
		}

		public int getProcessedDataLength() {
			return processedDataLength;
		}

		public int getProcessingStepsCount() {
			return processingStepsCount;
		}

		public String getTemplate() {
			return template;
		}

		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public String toString() {
			return String.format("ExecutionReport{planId='%s', contentLength=%d, template='%s'}", planId,
					content.length(), template);
		}

	}

}
