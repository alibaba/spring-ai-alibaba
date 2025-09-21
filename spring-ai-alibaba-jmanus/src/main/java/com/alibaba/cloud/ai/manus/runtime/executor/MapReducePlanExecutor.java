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
package com.alibaba.cloud.ai.manus.runtime.executor;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.cloud.ai.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.manus.agent.service.AgentService;
import com.alibaba.cloud.ai.manus.llm.ILlmService;
import com.alibaba.cloud.ai.manus.recorder.service.PlanExecutionRecorder;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionContext;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionPlan;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanExecutionResult;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.PlanInterface;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce.ExecutionNode;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce.MapReduceExecutionPlan;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce.MapReduceNode;
import com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce.SequentialNode;
import com.alibaba.cloud.ai.manus.planning.PlanningFactory.ToolCallBackContext;
import com.alibaba.cloud.ai.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.manus.tool.mapreduce.MapOutputTool;
import com.alibaba.cloud.ai.manus.tool.mapreduce.ReduceOperationTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor responsible for executing MapReduce mode plans, supporting parallel execution
 * of Map phase and serial execution of Reduce phase
 */
public class MapReducePlanExecutor extends AbstractPlanExecutor {

	private final ObjectMapper OBJECT_MAPPER;

	private static final Logger logger = LoggerFactory.getLogger(MapReducePlanExecutor.class);

	// ==================== Configuration Constants ====================

	/**
	 * Default maximum character limit for Reduce phase batch processing, used to control
	 * total character count of Map task results per batch, avoiding overly long context
	 */
	private static final int DEFAULT_REDUCE_BATCH_MAX_CHARACTERS = 2500;

	/**
	 * Maximum retry count for Map task execution, retry mechanism when task execution
	 * fails or is incomplete
	 */
	private static final int MAX_TASK_RETRY_COUNT = 3;

	/**
	 * Base time interval for retry waiting (milliseconds), actual wait time =
	 * BASE_RETRY_WAIT_MILLIS * current retry count
	 */
	private static final long BASE_RETRY_WAIT_MILLIS = 1000;

	/**
	 * Default character count when task character count calculation fails, fallback value
	 * when unable to read task output file, avoiding calculation errors
	 */
	private static final int DEFAULT_TASK_CHARACTER_COUNT = 100;

	/**
	 * Default thread pool thread count for Map task execution, used when configuration is
	 * not set
	 */
	private static final int DEFAULT_MAP_TASK_THREAD_POOL_SIZE = 1;

	/**
	 * Configuration check interval in milliseconds (10 seconds)
	 */
	private static final long CONFIG_CHECK_INTERVAL_MILLIS = 10_000;

	// Thread pool for parallel execution
	private volatile ExecutorService executorService;

	// Thread pool configuration tracking
	private volatile int currentThreadPoolSize;

	private volatile long lastConfigCheckTime;

	public MapReducePlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
			AgentService agentService, ILlmService llmService, ManusProperties manusProperties,
			ObjectMapper objectMapper, LevelBasedExecutorPool levelBasedExecutorPool) {
		super(agents, recorder, agentService, llmService, manusProperties, levelBasedExecutorPool);
		OBJECT_MAPPER = objectMapper;

		// Initialize thread pool with current configuration
		this.currentThreadPoolSize = getMapTaskThreadPoolSize();
		this.executorService = Executors.newFixedThreadPool(currentThreadPoolSize);
		this.lastConfigCheckTime = System.currentTimeMillis();

		logger.info("MapReducePlanExecutor initialized with thread pool size: {}", currentThreadPoolSize);
	}

	/**
	 * Execute all steps of the entire MapReduce plan asynchronously
	 * @param context Execution context containing user request and execution process
	 * information
	 * @return CompletableFuture containing PlanExecutionResult with all step results
	 */
	@Override
	public CompletableFuture<PlanExecutionResult> executeAllStepsAsync(ExecutionContext context) {
		return CompletableFuture.supplyAsync(() -> {
			PlanExecutionResult result = new PlanExecutionResult();
			BaseAgent lastExecutor = null;
			PlanInterface plan = context.getPlan();
			plan.setCurrentPlanId(context.getCurrentPlanId());
			plan.setRootPlanId(context.getRootPlanId());

			if (!(plan instanceof MapReduceExecutionPlan)) {
				logger.error("MapReducePlanExecutor can only execute MapReduceExecutionPlan, but got: {}",
						plan.getClass().getSimpleName());
				throw new IllegalArgumentException("MapReducePlanExecutor can only execute MapReduceExecutionPlan");
			}

			MapReduceExecutionPlan mapReducePlan = (MapReduceExecutionPlan) plan;
			plan.updateStepIndices();

			try {
				recorder.recordPlanExecutionStart(context.getCurrentPlanId(), context.getPlan().getTitle(),
						context.getUserRequest(), context.getPlan().getAllSteps(), context.getParentPlanId(),
						context.getRootPlanId(), context.getToolCallId());
				List<ExecutionNode> steps = mapReducePlan.getSteps();

				if (CollectionUtil.isNotEmpty(steps)) {
					for (Object stepNode : steps) {
						if (stepNode instanceof SequentialNode) {
							lastExecutor = executeSequentialNode((SequentialNode) stepNode, context, lastExecutor);
						}
						else if (stepNode instanceof MapReduceNode) {
							lastExecutor = executeMapReduceNode((MapReduceNode) stepNode, context, lastExecutor);
						}
					}
				}

				context.setSuccess(true);
				result.setSuccess(true);
				result.setFinalResult(context.getPlan().getResult());

			}
			catch (Exception e) {
				context.setSuccess(false);
				result.setSuccess(false);
				result.setErrorMessage(e.getMessage());
			}
			finally {
				performCleanup(context, lastExecutor);
			}

			return result;
		});
	}

	/**
	 * Execute sequential node
	 */
	private BaseAgent executeSequentialNode(SequentialNode seqNode, ExecutionContext context, BaseAgent lastExecutor) {
		logger.info("Executing sequential node with {} steps", seqNode.getStepCount());

		BaseAgent executor = lastExecutor;
		List<ExecutionStep> steps = seqNode.getSteps();

		if (CollectionUtil.isNotEmpty(steps)) {
			for (ExecutionStep step : steps) {
				BaseAgent stepExecutor = executeStep(step, context);
				if (stepExecutor != null) {
					executor = stepExecutor;
				}
			}
		}

		return executor;
	}

	/**
	 * Execute MapReduce node
	 */
	private BaseAgent executeMapReduceNode(MapReduceNode mrNode, ExecutionContext context, BaseAgent lastExecutor) {
		logger.info(
				"Executing MapReduce node, Data Prepared steps: {}, Map steps: {}, Reduce steps: {}, Post Process steps: {}",
				mrNode.getDataPreparedStepCount(), mrNode.getMapStepCount(), mrNode.getReduceStepCount(),
				mrNode.getPostProcessStepCount());

		BaseAgent executor = lastExecutor;

		// 1. Serial execution of Data Prepared phase
		if (CollectionUtil.isNotEmpty(mrNode.getDataPreparedSteps())) {
			executor = executeDataPreparedPhase(mrNode.getDataPreparedSteps(), context, executor);
		}

		List<ExecutionStep> mapSteps = mrNode.getMapSteps();
		// 2. Parallel execution of Map phase
		if (CollectionUtil.isNotEmpty(mapSteps)) {
			// Get MapReduceTool's ToolCallBackContext
			ToolCallBackContext toolCallBackContext = null;
			if (executor != null) {
				logger.debug("Attempting to get ToolCallBackContext for map_output_tool, current executor: {}",
						executor.getClass().getSimpleName());
				toolCallBackContext = executor.getToolCallBackContext("map_output_tool");
				if (toolCallBackContext == null) {
					logger.warn(
							"Unable to get ToolCallBackContext for map_output_tool, tool may not be properly registered or name mismatch");
				}
			}
			else {
				logger.error("Executor is null, unable to get ToolCallBackContext for MapOutputTool");
			}
			executor = executeMapPhase(mapSteps, context, toolCallBackContext);
		}

		// 3. Execute Reduce phase in parallel (sharing thread pool with Map phase)
		if (CollectionUtil.isNotEmpty(mrNode.getReduceSteps())) {
			executor = executeReducePhaseParallel(mrNode.getReduceSteps(), context, executor);
		}

		// 4. Execute Post Process phase serially (post-processing phase)
		if (CollectionUtil.isNotEmpty(mrNode.getPostProcessSteps())) {
			executor = executePostProcessPhase(mrNode.getPostProcessSteps(), context, executor);
		}

		return executor;
	}

	/**
	 * Execute Data Prepared phase serially
	 */
	private BaseAgent executeDataPreparedPhase(List<ExecutionStep> dataPreparedSteps, ExecutionContext context,
			BaseAgent lastExecutor) {
		logger.info("Executing Data Prepared phase serially, total {} steps", dataPreparedSteps.size());

		BaseAgent executor = lastExecutor;

		for (ExecutionStep step : dataPreparedSteps) {
			BaseAgent stepExecutor = executeStep(step, context);
			if (stepExecutor != null) {
				executor = stepExecutor;
			}
		}

		logger.info("Data Prepared phase execution completed");
		return executor;
	}

	/**
	 * Execute Post Process phase serially (post-processing phase) Similar to Data
	 * Prepared phase, supports single agent execution, specifically for final processing
	 * tasks after MapReduce workflow completion
	 */
	private BaseAgent executePostProcessPhase(List<ExecutionStep> postProcessSteps, ExecutionContext context,
			BaseAgent lastExecutor) {
		logger.info("Executing Post Process phase serially, total {} steps", postProcessSteps.size());

		BaseAgent executor = lastExecutor;

		for (ExecutionStep step : postProcessSteps) {
			BaseAgent stepExecutor = executeStep(step, context);
			if (stepExecutor != null) {
				executor = stepExecutor;
			}
		}

		// Record Post Process phase completion status - record completion status for each
		// Post Process step
		for (ExecutionStep step : postProcessSteps) {
			step.setAgent(executor);
			recorder.recordStepEnd(step, context.getCurrentPlanId());
		}
		logger.info("Post Process phase execution completed");
		return executor;
	}

	/**
	 * Execute Map phase in parallel
	 */
	private BaseAgent executeMapPhase(List<ExecutionStep> mapSteps, ExecutionContext context,
			ToolCallBackContext toolCallBackContext) {
		logger.info("Executing Map phase in parallel, total {} steps", mapSteps.size());

		// Record Map phase start status - record start status for each Map step
		for (ExecutionStep step : mapSteps) {
			recorder.recordStepStart(step, context.getCurrentPlanId());
		}

		// Add null pointer check
		if (toolCallBackContext == null) {
			logger.error(
					"ToolCallBackContext is null, cannot execute Map phase. Please ensure MapReduceTool context is properly obtained before executing Map phase.");
			throw new RuntimeException("ToolCallBackContext is null, cannot execute Map phase");
		}

		ToolCallBiFunctionDef<?> callFunc = toolCallBackContext.getFunctionInstance();
		if (callFunc == null) {
			logger.error("ToolCallBiFunctionDef is null, cannot execute Map phase");
			return null;
		}
		if (!(callFunc instanceof MapOutputTool)) {
			logger.error("ToolCallBiFunctionDef is not MapOutputTool, cannot execute Map phase. Actual type: {}",
					callFunc.getClass().getSimpleName());
			return null;
		}
		MapOutputTool splitTool = (MapOutputTool) callFunc;

		List<CompletableFuture<BaseAgent>> futures = new ArrayList<>();
		BaseAgent lastExecutor = null;

		try {
			// 2. Get task directory list (new MapReduceTool returns task directory paths)
			List<String> taskDirectories = splitTool.getSplitResults();

			if (taskDirectories.isEmpty()) {
				logger.error("No task directories found, Map phase execution failed");
				throw new RuntimeException("No task directories found, Map phase cannot execute");
			}
			else {
				logger.info("Found {} task directories, will execute Map steps for each task", taskDirectories.size());

				// 3. Create and execute mapSteps copies for each task directory
				for (String taskDirectory : taskDirectories) {
					// 4. Copy a new mapSteps list
					List<ExecutionStep> copiedMapSteps = copyMapSteps(mapSteps, taskDirectory);

					// 5. Use CompletableFuture to execute new mapSteps list for each task
					// directory
					CompletableFuture<BaseAgent> future = CompletableFuture.supplyAsync(() -> {
						BaseAgent fileExecutor = null;
						logger.info("Starting to process task directory: {}", taskDirectory);

						// Execute steps with task context parameter injection
						fileExecutor = executeStepsWithTaskContext(copiedMapSteps, context, taskDirectory);

						logger.info("Completed processing task directory: {}", taskDirectory);
						return fileExecutor;
					}, getUpdatedExecutorService());

					futures.add(future);
				}
			}

			// Wait for all Map steps to complete
			for (CompletableFuture<BaseAgent> future : futures) {
				try {
					BaseAgent executor = future.get();
					if (executor != null) {
						lastExecutor = executor;
					}
				}
				catch (Exception e) {
					logger.error("Map phase step execution failed", e);
				}
			}

		}
		catch (Exception e) {
			logger.error("Error occurred while executing Map phase", e);
			throw new RuntimeException("Map phase execution failed", e);
		}

		// Record Map phase completion status - record completion status for each Map step
		for (ExecutionStep step : mapSteps) {
			step.setAgent(lastExecutor);
			step.setResult("Successfully executed all Map tasks");
			recorder.recordStepEnd(step, context.getCurrentPlanId());
		}

		logger.info("Map phase execution completed");
		return lastExecutor;
	}

	/**
	 * Copy mapSteps list
	 */
	private List<ExecutionStep> copyMapSteps(List<ExecutionStep> originalSteps, String taskDirectory) {
		List<ExecutionStep> copiedSteps = new ArrayList<>();

		for (ExecutionStep originalStep : originalSteps) {
			ExecutionStep copiedStep = new ExecutionStep(originalStep.getStepId());
			// Preserve the original stepId to maintain consistency with pre-created
			// records
			copiedStep.setStepIndex(originalStep.getStepIndex());
			copiedStep.setStepRequirement(originalStep.getStepRequirement());
			copiedStep.setTerminateColumns(originalStep.getTerminateColumns());

			copiedSteps.add(copiedStep);
		}

		return copiedSteps;
	}

	/**
	 * Execute Reduce phase in parallel, sharing thread pool with Map phase Supports batch
	 * processing of Map task outputs, controlling task count per batch based on character
	 * count
	 */
	private BaseAgent executeReducePhaseParallel(List<ExecutionStep> reduceSteps, ExecutionContext context,
			BaseAgent lastExecutor) {
		logger.info("Executing Reduce phase in parallel, total {} steps", reduceSteps.size());

		// Record Reduce phase start status - record start status for each Reduce step
		for (ExecutionStep step : reduceSteps) {
			recorder.recordStepStart(step, context.getCurrentPlanId());
		}

		BaseAgent executor = lastExecutor;

		// Get ReduceOperationTool instance to obtain Map task results
		ToolCallBackContext reduceToolContext = null;
		if (executor != null) {
			reduceToolContext = executor.getToolCallBackContext("reduce_operation_tool");
		}

		if (reduceToolContext == null) {
			logger.error("Unable to get ReduceOperationTool context, Reduce phase cannot obtain Map task results");
			throw new RuntimeException("ReduceOperationTool context is null, cannot execute Reduce phase");
		}

		ToolCallBiFunctionDef<?> reduceToolFunc = reduceToolContext.getFunctionInstance();
		if (!(reduceToolFunc instanceof ReduceOperationTool)) {
			logger.error("Retrieved tool is not ReduceOperationTool instance, cannot execute Reduce phase");
			throw new RuntimeException("Tool type error, cannot execute Reduce phase");
		}

		ReduceOperationTool reduceTool = (ReduceOperationTool) reduceToolFunc;

		List<String> taskDirectories = reduceTool.getSplitResults();
		if (taskDirectories.isEmpty()) {
			logger.warn("No Map task results found, skipping Reduce phase");
			return executor;
		}

		// Configure character limit per batch processing (configurable, mainly limited by
		// context length)
		int maxBatchCharacters = getMaxBatchCharacters(context);
		logger.info(
				"Starting Reduce phase parallel processing, total {} Map tasks, character limit per batch {} characters",
				taskDirectories.size(), maxBatchCharacters);

		// Group Map task results into batches based on character count
		List<List<String>> batches = groupTasksByCharacterCount(taskDirectories, maxBatchCharacters);

		// Execute batches in parallel
		List<CompletableFuture<BaseAgent>> futures = new ArrayList<>();

		for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
			final int batchCounter = batchIndex + 1;
			final List<String> batchTaskDirectories = batches.get(batchIndex);

			logger.info("Preparing to process batch {} in parallel, containing {} tasks", batchCounter,
					batchTaskDirectories.size());

			// Create parallel task for each batch
			CompletableFuture<BaseAgent> future = CompletableFuture.supplyAsync(() -> {
				BaseAgent batchExecutor = null;
				logger.info("Starting to process Reduce batch {}", batchCounter);

				// Execute Reduce steps for current batch
				for (ExecutionStep step : reduceSteps) {
					BaseAgent stepExecutor = executeReduceStepWithBatch(step, context, batchTaskDirectories,
							batchCounter);
					if (stepExecutor != null) {
						batchExecutor = stepExecutor;
					}
				}

				logger.info("Completed processing Reduce batch {}", batchCounter);
				return batchExecutor;
			}, getUpdatedExecutorService());

			futures.add(future);
		}

		// Wait for all Reduce batches to complete
		for (CompletableFuture<BaseAgent> future : futures) {
			try {
				BaseAgent batchExecutor = future.get();
				if (batchExecutor != null) {
					executor = batchExecutor;
				}
			}
			catch (Exception e) {
				logger.error("Reduce phase batch execution failed", e);
			}
		}

		// Record Reduce phase completion status - record completion status for each
		// Reduce step
		for (ExecutionStep step : reduceSteps) {
			recorder.recordStepEnd(step, context.getCurrentPlanId());
		}

		logger.info("Reduce phase parallel execution completed, processed {} batches total", batches.size());
		return executor;
	}

	/**
	 * Get maximum character count per batch processing Can be dynamically adjusted based
	 * on context length limits and configuration
	 */
	private int getMaxBatchCharacters(ExecutionContext context) {
		// Can get configured batch character limit from ExecutionParams
		// String executionParams = context.getPlan().getExecutionParams();
		// if (executionParams != null &&
		// executionParams.contains(REDUCE_BATCH_CHARACTERS_CONFIG_KEY)) {
		// try {
		// String[] lines = executionParams.split("\n");
		// for (String line : lines) {
		// if (line.trim().startsWith(REDUCE_BATCH_CHARACTERS_CONFIG_KEY)) {
		// String charactersStr = line.split(":")[1].trim();
		// int configuredCharacters = Integer.parseInt(charactersStr);
		// if (configuredCharacters > 0 && configuredCharacters <=
		// MAX_REDUCE_BATCH_CHARACTERS_LIMIT) {
		// logger.info("Using configured Reduce batch character limit: {}",
		// configuredCharacters);
		// return configuredCharacters;
		// }
		// }
		// }
		// } catch (Exception e) {
		// logger.warn("Failed to parse reduce_batch_characters configuration, using
		// default value: {}",
		// DEFAULT_REDUCE_BATCH_MAX_CHARACTERS, e);
		// }
		// }

		logger.info("Using default Reduce batch character limit: {}", DEFAULT_REDUCE_BATCH_MAX_CHARACTERS);
		return DEFAULT_REDUCE_BATCH_MAX_CHARACTERS;
	}

	/**
	 * Group tasks into different batches based on character count Ensures total character
	 * count per batch doesn't exceed specified limit while maintaining document integrity
	 */
	private List<List<String>> groupTasksByCharacterCount(List<String> taskDirectories, int maxBatchCharacters) {
		List<List<String>> batches = new ArrayList<>();
		List<String> currentBatch = new ArrayList<>();
		int currentBatchCharacterCount = 0;

		for (String taskDirectory : taskDirectories) {
			// Calculate character count for current task
			int taskCharacterCount = getTaskCharacterCount(taskDirectory);

			// If single task character count exceeds limit, make it a separate batch
			if (taskCharacterCount > maxBatchCharacters) {
				// Save current batch first (if not empty)
				if (!currentBatch.isEmpty()) {
					batches.add(new ArrayList<>(currentBatch));
					currentBatch.clear();
					currentBatchCharacterCount = 0;
				}

				// Single oversized task as independent batch
				List<String> singleTaskBatch = new ArrayList<>();
				singleTaskBatch.add(taskDirectory);
				batches.add(singleTaskBatch);
				logger.warn("Task {} character count {} exceeds batch limit {}, making it a separate batch",
						taskDirectory, taskCharacterCount, maxBatchCharacters);
				continue;
			}

			// Check if adding current task would exceed limit
			if (currentBatchCharacterCount + taskCharacterCount > maxBatchCharacters && !currentBatch.isEmpty()) {
				// Save current batch and start new batch
				batches.add(new ArrayList<>(currentBatch));
				currentBatch.clear();
				currentBatchCharacterCount = 0;
			}

			// Add task to current batch
			currentBatch.add(taskDirectory);
			currentBatchCharacterCount += taskCharacterCount;

			logger.debug("Task {} ({} characters) added to batch, current batch character count: {}", taskDirectory,
					taskCharacterCount, currentBatchCharacterCount);
		}

		// Add last batch (if not empty)
		if (!currentBatch.isEmpty()) {
			batches.add(currentBatch);
		}

		// Log batch grouping results
		for (int i = 0; i < batches.size(); i++) {
			List<String> batch = batches.get(i);
			int batchTotalCharacters = batch.stream().mapToInt(this::getTaskCharacterCount).sum();
			logger.info("Batch {} contains {} tasks, total character count: {}", i + 1, batch.size(),
					batchTotalCharacters);
		}

		return batches;
	}

	/**
	 * Get character count for single task Read output.md file in task directory and
	 * calculate character count
	 */
	private int getTaskCharacterCount(String taskDirectory) {
		try {
			Path taskPath = Paths.get(taskDirectory);
			Path outputFile = taskPath.resolve("output.md");

			if (Files.exists(outputFile)) {
				String content = Files.readString(outputFile);
				return content.length();
			}
			else {
				logger.warn("output.md file does not exist in task directory {}, returning default character count",
						taskDirectory);
				return DEFAULT_TASK_CHARACTER_COUNT;
			}
		}
		catch (Exception e) {
			logger.error("Failed to read character count for task {}", taskDirectory, e);
			return DEFAULT_TASK_CHARACTER_COUNT;
		}
	}

	/**
	 * Execute Reduce step with batch Map results Use InnerStorageTool to aggregate and
	 * store batch context to unified file, avoiding overly long context
	 */
	private BaseAgent executeReduceStepWithBatch(ExecutionStep step, ExecutionContext context,
			List<String> batchTaskDirectories, int batchCounter) {

		// Save original ExecutionParams and temporarily modify

		// Save original ExecutionParams and temporarily modify
		String originalExecutionParams = context.getPlan().getExecutionParams();
		StringBuilder enhancedParams = new StringBuilder();
		if (originalExecutionParams != null && !originalExecutionParams.trim().isEmpty()) {
			enhancedParams.append(originalExecutionParams).append("\n\n");
		}

		// Add simplified batch context information
		enhancedParams.append("=== Reduce Batch ").append(String.format("%03d", batchCounter)).append(" Context : \n");

		// Only include output.md content, not status data
		for (String taskDirectory : batchTaskDirectories) {
			try {
				Path taskPath = Paths.get(taskDirectory);
				String taskId = taskPath.getFileName().toString();

				// Read task's output.md file (Map phase output)
				Path outputFile = taskPath.resolve("output.md");
				if (Files.exists(outputFile)) {
					String outputContent = Files.readString(outputFile);
					enhancedParams.append("=== Task ID: ").append(taskId).append(" ===\n");
					enhancedParams.append(outputContent).append("\n");
					enhancedParams.append("=== Task ID: ").append(taskId).append(" End ===\n\n");
				}
			}
			catch (Exception e) {
				logger.error("Failed to read Map task output: {}", taskDirectory, e);
			}
		}

		// Create modified step
		// ExecutionStep enhancedStep = new ExecutionStep();
		// enhancedStep.setStepIndex(step.getStepIndex());
		// enhancedStep.setStepRequirement(step.getStepRequirement());

		try {
			// Temporarily set enhanced ExecutionParams
			context.getPlan().setExecutionParams(enhancedParams.toString());

			// Execute step
			BaseAgent stepExecutor = executeStep(step, context);

			logger.info("Completed processing Reduce batch {}, containing {} Map tasks", batchCounter,
					batchTaskDirectories.size());
			return stepExecutor;

		}
		finally {
			// Restore original ExecutionParams
			context.getPlan().setExecutionParams(originalExecutionParams);
		}
	}

	/**
	 * Shutdown executor, release thread pool resources
	 */
	public void shutdown() {
		ExecutorService currentExecutor = executorService;
		if (currentExecutor != null && !currentExecutor.isShutdown()) {
			shutdownExecutorGracefully(currentExecutor);
		}
	}

	/**
	 * Override parent class executeStep method, temporarily add task information to
	 * ExecutionParams during map task execution
	 */
	@Override
	protected BaseAgent executeStep(ExecutionStep step, ExecutionContext context) {
		// Directly call parent method, as task context parameter injection is already
		// handled in executeStepsWithTaskContext
		return super.executeStep(step, context);
	}

	/**
	 * Execute step list with task context parameter injection, supporting task completion
	 * status check and retry Use copied ExecutionContext to avoid modifying original
	 * context
	 * @param steps Step list to execute
	 * @param context Execution context
	 * @param taskDirectory Task directory path
	 * @return Last executed Agent
	 */
	private BaseAgent executeStepsWithTaskContext(List<ExecutionStep> steps, ExecutionContext context,
			String taskDirectory) {
		BaseAgent fileExecutor = null;

		// 2. Find input.md fixed file from corresponding directory based on taskDirectory
		String taskId = "";
		String fileContent = "";

		try {
			// Extract task ID (get last directory name from taskDirectory path)
			Path taskPath = Paths.get(taskDirectory);
			taskId = taskPath.getFileName().toString();

			// Read input.md file content
			Path inputFile = taskPath.resolve("input.md");
			if (Files.exists(inputFile)) {
				fileContent = Files.readString(inputFile);
				logger.debug("Successfully read task file content, task ID: {}, content length: {} characters", taskId,
						fileContent.length());
			}
			else {
				logger.warn("input.md file does not exist in task directory: {}", inputFile);
				fileContent = "Task file does not exist";
			}
		}
		catch (Exception e) {
			logger.error("Failed to read task file: {}", taskDirectory, e);
			fileContent = "Error occurred while reading task file: " + e.getMessage();
		}

		// 3. Create ExecutionContext copy with enhanced parameters
		ExecutionContext copiedContext = createContextCopyWithEnhancedParams(context, taskId, fileContent);

		// Execute task with retry mechanism support
		int maxRetries = MAX_TASK_RETRY_COUNT;
		int currentRetry = 0;
		boolean taskCompleted = false;

		while (currentRetry <= maxRetries && !taskCompleted) {
			if (currentRetry > 0) {
				logger.warn("Task {} retry execution attempt {}", taskId, currentRetry);
			}

			try {
				// 4. Execute steps using copied context
				for (ExecutionStep step : steps) {
					BaseAgent stepExecutor = executeStep(step, copiedContext);
					if (stepExecutor != null) {
						fileExecutor = stepExecutor;
					}
				}

				// 5. Check if task is completed
				taskCompleted = checkTaskCompletion(taskDirectory, taskId);

				if (taskCompleted) {
					logger.info("Task {} executed successfully", taskId);
					break;
				}
				else {
					logger.warn(
							"Task {} execution incomplete, detected task status is not completed or missing output file",
							taskId);
					currentRetry++;

					if (currentRetry <= maxRetries) {
						// Wait for some time before retry
						try {
							Thread.sleep(BASE_RETRY_WAIT_MILLIS * currentRetry); // Incremental
																					// wait
																					// time
						}
						catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							logger.error("Retry wait interrupted", ie);
							break;
						}
					}
				}

			}
			catch (Exception e) {
				logger.error("Exception occurred while executing task {}", taskId, e);
				currentRetry++;

				if (currentRetry <= maxRetries) {
					try {
						Thread.sleep(BASE_RETRY_WAIT_MILLIS * currentRetry);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						logger.error("Retry wait interrupted", ie);
						break;
					}
				}
			}
		}

		// 6. Final task status check
		if (!taskCompleted) {
			logger.error("Task {} still incomplete after {} retries", taskId, maxRetries);
			throw new RuntimeException("Task " + taskId + " execution failed, maximum retry count reached");
		}

		return fileExecutor;
	}

	/**
	 * Create ExecutionContext copy and enhance ExecutionParams
	 * @param originalContext Original execution context
	 * @param taskId Task ID
	 * @param fileContent File content
	 * @return Enhanced ExecutionContext copy
	 */
	private ExecutionContext createContextCopyWithEnhancedParams(ExecutionContext originalContext, String taskId,
			String fileContent) {
		// Create ExecutionContext copy
		ExecutionContext copiedContext = new ExecutionContext();

		// Copy basic properties
		copiedContext.setCurrentPlanId(originalContext.getCurrentPlanId());
		copiedContext.setRootPlanId(originalContext.getRootPlanId());
		copiedContext.setUserRequest(originalContext.getUserRequest());
		copiedContext.setNeedSummary(originalContext.isNeedSummary());
		copiedContext.setSuccess(originalContext.isSuccess());
		copiedContext.setUseMemory(originalContext.isUseMemory());

		// Copy tool context
		if (originalContext.getToolsContext() != null) {
			Map<String, String> copiedToolsContext = new HashMap<>(originalContext.getToolsContext());
			copiedContext.setToolsContext(copiedToolsContext);
		}

		// Create Plan copy and enhance ExecutionParams
		PlanInterface copiedPlan = createPlanCopyWithEnhancedParams(originalContext.getPlan(), taskId, fileContent);
		copiedContext.setPlan(copiedPlan);

		return copiedContext;
	}

	/**
	 * Create Plan copy and enhance ExecutionParams
	 * @param originalPlan Original plan
	 * @param taskId Task ID
	 * @param fileContent File content
	 * @return Enhanced Plan copy
	 */
	private PlanInterface createPlanCopyWithEnhancedParams(PlanInterface originalPlan, String taskId,
			String fileContent) {
		// Create copy based on actual Plan type
		PlanInterface copiedPlan;

		if (originalPlan instanceof MapReduceExecutionPlan) {
			MapReduceExecutionPlan originalMapReducePlan = (MapReduceExecutionPlan) originalPlan;
			MapReduceExecutionPlan copiedMapReducePlan = new MapReduceExecutionPlan();

			// Copy all properties of MapReduceExecutionPlan
			copiedMapReducePlan.setCurrentPlanId(originalMapReducePlan.getCurrentPlanId());
			copiedMapReducePlan.setRootPlanId(originalMapReducePlan.getRootPlanId());
			copiedMapReducePlan.setTitle(originalMapReducePlan.getTitle());
			copiedMapReducePlan.setPlanningThinking(originalMapReducePlan.getPlanningThinking());
			copiedMapReducePlan.setUserRequest(originalMapReducePlan.getUserRequest());
			// Copy step structure (Note: copying references here, as steps themselves
			// won't be modified during execution)
			copiedMapReducePlan.setSteps(originalMapReducePlan.getSteps());

			copiedPlan = copiedMapReducePlan;
		}
		else {
			// Handle other Plan types, such as ExecutionPlan
			ExecutionPlan originalExecutionPlan = (ExecutionPlan) originalPlan;
			ExecutionPlan copiedExecutionPlan = new ExecutionPlan();

			// Copy all properties of ExecutionPlan
			copiedExecutionPlan.setCurrentPlanId(originalExecutionPlan.getCurrentPlanId());
			copiedExecutionPlan.setTitle(originalExecutionPlan.getTitle());
			copiedExecutionPlan.setPlanningThinking(originalExecutionPlan.getPlanningThinking());
			copiedExecutionPlan.setUserRequest(originalExecutionPlan.getUserRequest());

			// Copy step list (Note: copying references here, as steps themselves won't be
			// modified during execution)
			copiedExecutionPlan.setSteps(originalExecutionPlan.getSteps());

			copiedPlan = copiedExecutionPlan;
		}

		// Create enhanced ExecutionParams
		String originalExecutionParams = originalPlan.getExecutionParams();
		StringBuilder enhancedParams = new StringBuilder();
		if (originalExecutionParams != null && !originalExecutionParams.trim().isEmpty()) {
			enhancedParams.append(originalExecutionParams).append("\n\n");
		}
		enhancedParams.append("=== Current Task Context ===\n");
		enhancedParams.append("Task ID: ").append(taskId).append("\n");
		enhancedParams.append("File Content: ").append(fileContent).append("\n");
		enhancedParams.append("=== Task Context End ===");

		// Set enhanced ExecutionParams
		copiedPlan.setExecutionParams(enhancedParams.toString());

		return copiedPlan;
	}

	/**
	 * Get Map task thread pool size from configuration
	 * @return configured thread pool size or default value if not configured
	 */
	private int getMapTaskThreadPoolSize() {
		if (manusProperties != null) {
			Integer configuredThreads = manusProperties.getInfiniteContextParallelThreads();
			if (configuredThreads != null && configuredThreads > 0) {
				logger.debug("Using configured Map task thread pool size: {}", configuredThreads);
				return configuredThreads;
			}
		}

		logger.debug("Using default Map task thread pool size: {}", DEFAULT_MAP_TASK_THREAD_POOL_SIZE);
		return DEFAULT_MAP_TASK_THREAD_POOL_SIZE;
	}

	/**
	 * Check and update thread pool configuration if needed. This method is called before
	 * each executor service usage to ensure configuration changes are picked up without
	 * using timers or background threads.
	 * @return the current (potentially updated) executor service
	 */
	private ExecutorService getUpdatedExecutorService() {
		long currentTime = System.currentTimeMillis();

		// Check if enough time has passed since last configuration check
		if (currentTime - lastConfigCheckTime >= CONFIG_CHECK_INTERVAL_MILLIS) {
			lastConfigCheckTime = currentTime;

			// Get current configuration
			int newThreadPoolSize = getMapTaskThreadPoolSize();

			// Check if configuration has changed
			if (newThreadPoolSize != currentThreadPoolSize) {
				logger.info("Thread pool size configuration changed from {} to {}, rebuilding thread pool",
						currentThreadPoolSize, newThreadPoolSize);

				// Gracefully shutdown old executor service
				ExecutorService oldExecutorService = executorService;

				// Create new executor service with updated configuration
				ExecutorService newExecutorService = Executors.newFixedThreadPool(newThreadPoolSize);

				// Update current state atomically
				this.executorService = newExecutorService;
				this.currentThreadPoolSize = newThreadPoolSize;

				// Gracefully shutdown old executor service in background
				// This ensures existing tasks can complete
				shutdownExecutorGracefully(oldExecutorService);

				logger.info("Thread pool successfully updated to size: {}", newThreadPoolSize);
			}
		}

		return executorService;
	}

	/**
	 * Gracefully shutdown an executor service
	 * @param executor the executor service to shutdown
	 */
	private void shutdownExecutorGracefully(ExecutorService executor) {
		if (executor != null && !executor.isShutdown()) {
			try {
				// Initiate graceful shutdown
				executor.shutdown();
				logger.debug("Old thread pool shutdown initiated");
			}
			catch (Exception e) {
				logger.warn("Error during graceful shutdown of old thread pool", e);
				// Force shutdown if graceful shutdown fails
				executor.shutdownNow();
			}
		}
	}

	/**
	 * Check if task is completed
	 * @param taskDirectory Task directory path
	 * @param taskId Task ID
	 * @return Whether task is completed
	 */
	private boolean checkTaskCompletion(String taskDirectory, String taskId) {
		try {
			Path taskPath = Paths.get(taskDirectory);

			// Check status.json file
			Path statusFile = taskPath.resolve("status.json");
			if (Files.exists(statusFile)) {
				String statusContent = Files.readString(statusFile);
				logger.debug("Task {} status file content: {}", taskId, statusContent);

				// Use ObjectMapper to parse statusContent as Map then check status field
				try {
					Map<?, ?> statusMap = OBJECT_MAPPER.readValue(statusContent, Map.class);
					Object statusValue = statusMap.get("status");
					if ("completed".equals(statusValue)) {
						// Also check if output.md file exists
						Path outputFile = taskPath.resolve("output.md");
						if (Files.exists(outputFile)) {
							logger.debug("Task {} completed, status file and output file exist", taskId);
							return true;
						}
						else {
							logger.warn("Task {} status is completed but missing output.md file", taskId);
							return false;
						}
					}
					else {
						logger.debug("Task {} status is not completed", taskId);
						return false;
					}
				}
				catch (Exception jsonEx) {
					logger.error("Failed to parse status.json as Map", jsonEx);
					return false;
				}
			}
			else {
				logger.warn("Task {} missing status.json file", taskId);
				return false;
			}

		}
		catch (Exception e) {
			logger.error("Error occurred while checking task {} completion status", taskId, e);
			return false;
		}
	}

}
