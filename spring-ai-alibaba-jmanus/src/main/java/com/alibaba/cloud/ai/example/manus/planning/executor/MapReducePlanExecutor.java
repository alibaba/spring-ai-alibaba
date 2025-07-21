/*
 * Copyright 2025 timport com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;riginal author or authors.
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
package com.alibaba.cloud.ai.example.manus.planning.executor;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.llm.ILlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.ExecutionNode;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceNode;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.SequentialNode;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.mapreduce.MapOutputTool;
import com.alibaba.cloud.ai.example.manus.tool.mapreduce.ReduceOperationTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责执行 MapReduce 模式计划的执行器 支持并行执行 Map 阶段和串行执行 Reduce 阶段
 */
public class MapReducePlanExecutor extends AbstractPlanExecutor {

	private static final Logger logger = LoggerFactory.getLogger(MapReducePlanExecutor.class);

	// ==================== 配置常量 ====================

	/**
	 * Reduce阶段批次处理的默认最大字符数限制 用于控制每个批次处理的Map任务结果总字符数，避免上下文过长
	 */
	private static final int DEFAULT_REDUCE_BATCH_MAX_CHARACTERS = 2500;

	/**
	 * Map任务执行的最大重试次数 当任务执行失败或未完成时的重试机制
	 */
	private static final int MAX_TASK_RETRY_COUNT = 3;

	/**
	 * 重试等待的基础时间间隔（毫秒） 实际等待时间 = BASE_RETRY_WAIT_MILLIS * 当前重试次数
	 */
	private static final long BASE_RETRY_WAIT_MILLIS = 1000;

	/**
	 * 任务字符数计算失败时的默认字符数 当无法读取任务输出文件时的回退值，避免计算错误
	 */
	private static final int DEFAULT_TASK_CHARACTER_COUNT = 100;

	/**
	 * Map任务执行的默认线程池线程数 当配置未设置时使用此默认值
	 */
	private static final int DEFAULT_MAP_TASK_THREAD_POOL_SIZE = 1;

	// 线程池用于并行执行
	private final ExecutorService executorService;

	public MapReducePlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
			AgentService agentService, ILlmService llmService, ManusProperties manusProperties) {
		super(agents, recorder, agentService, llmService, manusProperties);

		// Get thread pool size from configuration
		int threadPoolSize = getMapTaskThreadPoolSize();
		this.executorService = Executors.newFixedThreadPool(threadPoolSize);

		logger.info("MapReducePlanExecutor initialized with thread pool size: {}", threadPoolSize);
	}

	/**
	 * 执行整个 MapReduce 计划的所有步骤
	 * @param context 执行上下文，包含用户请求和执行的过程信息
	 */
	@Override
	public void executeAllSteps(ExecutionContext context) {
		BaseAgent lastExecutor = null;
		PlanInterface plan = context.getPlan();

		if (!(plan instanceof MapReduceExecutionPlan)) {
			logger.error("MapReducePlanExecutor can only execute MapReduceExecutionPlan, but got: {}",
					plan.getClass().getSimpleName());
			throw new IllegalArgumentException("MapReducePlanExecutor can only execute MapReduceExecutionPlan");
		}

		MapReduceExecutionPlan mapReducePlan = (MapReduceExecutionPlan) plan;
		plan.updateStepIndices();

		try {
			recorder.recordPlanExecutionStart(context);
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
		}
		finally {
			performCleanup(context, lastExecutor);
		}
	}

	/**
	 * 执行顺序节点
	 */
	private BaseAgent executeSequentialNode(SequentialNode seqNode, ExecutionContext context, BaseAgent lastExecutor) {
		logger.info("执行顺序节点，包含 {} 个步骤", seqNode.getStepCount());

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
	 * 执行 MapReduce 节点
	 */
	private BaseAgent executeMapReduceNode(MapReduceNode mrNode, ExecutionContext context, BaseAgent lastExecutor) {
		logger.info("执行 MapReduce 节点，Data Prepared 步骤: {}, Map 步骤: {}, Reduce 步骤: {}, Post Process 步骤: {}",
				mrNode.getDataPreparedStepCount(), mrNode.getMapStepCount(), mrNode.getReduceStepCount(),
				mrNode.getPostProcessStepCount());

		BaseAgent executor = lastExecutor;

		// 1. 串行执行 Data Prepared 阶段
		if (CollectionUtil.isNotEmpty(mrNode.getDataPreparedSteps())) {
			executor = executeDataPreparedPhase(mrNode.getDataPreparedSteps(), context, executor);
		}

		List<ExecutionStep> mapSteps = mrNode.getMapSteps();
		// 2. 并行执行 Map 阶段
		if (CollectionUtil.isNotEmpty(mapSteps)) {
			// 获取 MapReduceTool 的 ToolCallBackContext
			ToolCallBackContext toolCallBackContext = null;
			if (executor != null) {
				logger.debug("尝试获取 map_output_tool 的 ToolCallBackContext，当前executor: {}",
						executor.getClass().getSimpleName());
				toolCallBackContext = executor.getToolCallBackContext("map_output_tool");
				if (toolCallBackContext == null) {
					logger.warn("无法获取 map_output_tool 的 ToolCallBackContext，工具可能未正确注册或名称不匹配");
				}
			}
			else {
				logger.error("executor 为空，无法获取 MapOutputTool 的 ToolCallBackContext");
			}
			executor = executeMapPhase(mapSteps, context, toolCallBackContext);
		}

		// 3. 并行执行 Reduce 阶段（与Map阶段共享线程池）
		if (CollectionUtil.isNotEmpty(mrNode.getReduceSteps())) {
			executor = executeReducePhaseParallel(mrNode.getReduceSteps(), context, executor);
		}

		// 4. 串行执行 Post Process 阶段（后处理阶段）
		if (CollectionUtil.isNotEmpty(mrNode.getPostProcessSteps())) {
			executor = executePostProcessPhase(mrNode.getPostProcessSteps(), context, executor);
		}

		return executor;
	}

	/**
	 * 串行执行 Data Prepared 阶段
	 */
	private BaseAgent executeDataPreparedPhase(List<ExecutionStep> dataPreparedSteps, ExecutionContext context,
			BaseAgent lastExecutor) {
		logger.info("串行执行 Data Prepared 阶段，共 {} 个步骤", dataPreparedSteps.size());

		BaseAgent executor = lastExecutor;

		for (ExecutionStep step : dataPreparedSteps) {
			BaseAgent stepExecutor = executeStep(step, context);
			if (stepExecutor != null) {
				executor = stepExecutor;
			}
		}

		logger.info("Data Prepared 阶段执行完成");
		return executor;
	}

	/**
	 * 串行执行 Post Process 阶段（后处理阶段） 类似于 Data Prepared 阶段，支持单个代理执行，专门用于 MapReduce
	 * 流程完成后的最终处理任务
	 */
	private BaseAgent executePostProcessPhase(List<ExecutionStep> postProcessSteps, ExecutionContext context,
			BaseAgent lastExecutor) {
		logger.info("串行执行 Post Process 阶段，共 {} 个步骤", postProcessSteps.size());

		BaseAgent executor = lastExecutor;

		for (ExecutionStep step : postProcessSteps) {
			BaseAgent stepExecutor = executeStep(step, context);
			if (stepExecutor != null) {
				executor = stepExecutor;
			}
		}

		// 记录Reduce阶段完成状态 - 为每个Reduce步骤记录完成状态
		for (ExecutionStep step : postProcessSteps) {
			step.setAgent(executor);
			recorder.recordStepEnd(step, context);
		}
		logger.info("Post Process 阶段执行完成");
		return executor;
	}

	/**
	 * 并行执行 Map 阶段
	 */
	private BaseAgent executeMapPhase(List<ExecutionStep> mapSteps, ExecutionContext context,
			ToolCallBackContext toolCallBackContext) {
		logger.info("并行执行 Map 阶段，共 {} 个步骤", mapSteps.size());

		// 记录Map阶段开始状态 - 为每个Map步骤记录开始状态
		for (ExecutionStep step : mapSteps) {
			recorder.recordStepStart(step, context);
		}

		// 添加空指针检查
		if (toolCallBackContext == null) {
			logger.error("ToolCallBackContext is null, cannot execute Map phase. 请确保在执行Map阶段之前已正确获取MapReduceTool的上下文。");
			throw new RuntimeException("ToolCallBackContext为空，无法执行Map阶段");
		}

		ToolCallBiFunctionDef<?> callFunc = toolCallBackContext.getFunctionInstance();
		if (callFunc == null) {
			logger.error("ToolCallBiFunctionDef is null, cannot execute Map phase");
			return null;
		}
		if (!(callFunc instanceof MapOutputTool)) {
			logger.error("ToolCallBiFunctionDef is not MapOutputTool, cannot execute Map phase. 实际类型: {}",
					callFunc.getClass().getSimpleName());
			return null;
		}
		MapOutputTool splitTool = (MapOutputTool) callFunc;

		List<CompletableFuture<BaseAgent>> futures = new ArrayList<>();
		BaseAgent lastExecutor = null;

		try {
			// 2. 获取任务目录列表（新的MapReduceTool返回任务目录路径）
			List<String> taskDirectories = splitTool.getSplitResults();

			if (taskDirectories.isEmpty()) {
				logger.error("没有找到任务目录，Map 阶段执行失败");
				throw new RuntimeException("没有找到任务目录，Map 阶段无法执行");
			}
			else {
				logger.info("找到 {} 个任务目录，将为每个任务执行 Map 步骤", taskDirectories.size());

				// 3. 为每个任务目录创建并执行 mapSteps 副本
				for (String taskDirectory : taskDirectories) {
					// 4. 复制一个新的 mapSteps 列表
					List<ExecutionStep> copiedMapSteps = copyMapSteps(mapSteps, taskDirectory);

					// 5. 使用 CompletableFuture 为每个任务目录执行新的 mapSteps 列表
					CompletableFuture<BaseAgent> future = CompletableFuture.supplyAsync(() -> {
						BaseAgent fileExecutor = null;
						logger.info("开始处理任务目录: {}", taskDirectory);

						// 执行带有任务上下文参数注入的步骤
						fileExecutor = executeStepsWithTaskContext(copiedMapSteps, context, taskDirectory);

						logger.info("完成处理任务目录: {}", taskDirectory);
						return fileExecutor;
					}, executorService);

					futures.add(future);
				}
			}

			// 等待所有 Map 步骤完成
			for (CompletableFuture<BaseAgent> future : futures) {
				try {
					BaseAgent executor = future.get();
					if (executor != null) {
						lastExecutor = executor;
					}
				}
				catch (Exception e) {
					logger.error("Map 阶段步骤执行失败", e);
				}
			}

		}
		catch (Exception e) {
			logger.error("执行 Map 阶段时发生错误", e);
			throw new RuntimeException("Map 阶段执行失败", e);
		}

		// 记录Map阶段完成状态 - 为每个Map步骤记录完成状态
		for (ExecutionStep step : mapSteps) {
			step.setAgent(lastExecutor);
			step.setResult("已经成功的执行了所有的Map任务");
			recorder.recordStepEnd(step, context);
		}

		logger.info("Map 阶段执行完成");
		return lastExecutor;
	}

	/**
	 * 复制 mapSteps 列表
	 */
	private List<ExecutionStep> copyMapSteps(List<ExecutionStep> originalSteps, String taskDirectory) {
		List<ExecutionStep> copiedSteps = new ArrayList<>();

		for (ExecutionStep originalStep : originalSteps) {
			ExecutionStep copiedStep = new ExecutionStep();
			copiedStep.setStepIndex(originalStep.getStepIndex());
			copiedStep.setStepRequirement(originalStep.getStepRequirement());
			copiedStep.setTerminateColumns(originalStep.getTerminateColumns());

			copiedSteps.add(copiedStep);
		}

		return copiedSteps;
	}

	/**
	 * 并行执行 Reduce 阶段，与Map阶段共享线程池 支持批量处理Map任务输出，基于字符数控制每批次处理的任务数量
	 */
	private BaseAgent executeReducePhaseParallel(List<ExecutionStep> reduceSteps, ExecutionContext context,
			BaseAgent lastExecutor) {
		logger.info("并行执行 Reduce 阶段，共 {} 个步骤", reduceSteps.size());

		// 记录Reduce阶段开始状态 - 为每个Reduce步骤记录开始状态
		for (ExecutionStep step : reduceSteps) {
			recorder.recordStepStart(step, context);
		}

		BaseAgent executor = lastExecutor;

		// 获取ReduceOperationTool实例以获取Map任务结果
		ToolCallBackContext reduceToolContext = null;
		if (executor != null) {
			reduceToolContext = executor.getToolCallBackContext("reduce_operation_tool");
		}

		if (reduceToolContext == null) {
			logger.error("无法获取ReduceOperationTool上下文，Reduce阶段无法获取Map任务结果");
			throw new RuntimeException("ReduceOperationTool上下文为空，无法执行Reduce阶段");
		}

		ToolCallBiFunctionDef<?> reduceToolFunc = reduceToolContext.getFunctionInstance();
		if (!(reduceToolFunc instanceof ReduceOperationTool)) {
			logger.error("获取的工具不是ReduceOperationTool实例，无法执行Reduce阶段");
			throw new RuntimeException("工具类型错误，无法执行Reduce阶段");
		}

		ReduceOperationTool reduceTool = (ReduceOperationTool) reduceToolFunc;

		List<String> taskDirectories = reduceTool.getSplitResults();
		if (taskDirectories.isEmpty()) {
			logger.warn("没有找到Map任务结果，Reduce阶段跳过");
			return executor;
		}

		// 配置每批次处理的字符数限制（可配置，主要受制于上下文长度限制）
		int maxBatchCharacters = getMaxBatchCharacters(context);
		logger.info("开始Reduce阶段并行处理，共 {} 个Map任务，每批次字符数限制 {} 字符", taskDirectories.size(), maxBatchCharacters);

		// 基于字符数分批次处理Map任务结果
		List<List<String>> batches = groupTasksByCharacterCount(taskDirectories, maxBatchCharacters);

		// 并行执行各个批次
		List<CompletableFuture<BaseAgent>> futures = new ArrayList<>();

		for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
			final int batchCounter = batchIndex + 1;
			final List<String> batchTaskDirectories = batches.get(batchIndex);

			logger.info("准备并行处理第 {} 批次，包含 {} 个任务", batchCounter, batchTaskDirectories.size());

			// 为每个批次创建并行任务
			CompletableFuture<BaseAgent> future = CompletableFuture.supplyAsync(() -> {
				BaseAgent batchExecutor = null;
				logger.info("开始处理Reduce批次 {}", batchCounter);

				// 为当前批次执行Reduce步骤
				for (ExecutionStep step : reduceSteps) {
					BaseAgent stepExecutor = executeReduceStepWithBatch(step, context, batchTaskDirectories,
							batchCounter);
					if (stepExecutor != null) {
						batchExecutor = stepExecutor;
					}
				}

				logger.info("完成处理Reduce批次 {}", batchCounter);
				return batchExecutor;
			}, executorService);

			futures.add(future);
		}

		// 等待所有 Reduce 批次完成
		for (CompletableFuture<BaseAgent> future : futures) {
			try {
				BaseAgent batchExecutor = future.get();
				if (batchExecutor != null) {
					executor = batchExecutor;
				}
			}
			catch (Exception e) {
				logger.error("Reduce 阶段批次执行失败", e);
			}
		}

		// 记录Reduce阶段完成状态 - 为每个Reduce步骤记录完成状态
		for (ExecutionStep step : reduceSteps) {
			recorder.recordStepEnd(step, context);
		}

		logger.info("Reduce 阶段并行执行完成，共处理 {} 个批次", batches.size());
		return executor;
	}

	/**
	 * 获取每批次处理的最大字符数 可以根据上下文长度限制和配置来动态调整
	 */
	private int getMaxBatchCharacters(ExecutionContext context) {
		// 可以从ExecutionParams中获取配置的批次字符数限制
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
		// logger.info("使用配置的Reduce批次字符数限制: {}", configuredCharacters);
		// return configuredCharacters;
		// }
		// }
		// }
		// } catch (Exception e) {
		// logger.warn("解析reduce_batch_characters配置失败，使用默认值: {}",
		// DEFAULT_REDUCE_BATCH_MAX_CHARACTERS, e);
		// }
		// }

		logger.info("使用默认的Reduce批次字符数限制: {}", DEFAULT_REDUCE_BATCH_MAX_CHARACTERS);
		return DEFAULT_REDUCE_BATCH_MAX_CHARACTERS;
	}

	/**
	 * 根据字符数将任务分组到不同批次 确保每个批次的总字符数不超过指定限制，且保持文档完整性
	 */
	private List<List<String>> groupTasksByCharacterCount(List<String> taskDirectories, int maxBatchCharacters) {
		List<List<String>> batches = new ArrayList<>();
		List<String> currentBatch = new ArrayList<>();
		int currentBatchCharacterCount = 0;

		for (String taskDirectory : taskDirectories) {
			// 计算当前任务的字符数
			int taskCharacterCount = getTaskCharacterCount(taskDirectory);

			// 如果单个任务的字符数已经超过限制，单独作为一个批次
			if (taskCharacterCount > maxBatchCharacters) {
				// 先保存当前批次（如果不为空）
				if (!currentBatch.isEmpty()) {
					batches.add(new ArrayList<>(currentBatch));
					currentBatch.clear();
					currentBatchCharacterCount = 0;
				}

				// 单个超大任务作为独立批次
				List<String> singleTaskBatch = new ArrayList<>();
				singleTaskBatch.add(taskDirectory);
				batches.add(singleTaskBatch);
				logger.warn("任务 {} 字符数 {} 超过批次限制 {}，单独作为一个批次", taskDirectory, taskCharacterCount, maxBatchCharacters);
				continue;
			}

			// 检查加入当前任务后是否会超过限制
			if (currentBatchCharacterCount + taskCharacterCount > maxBatchCharacters && !currentBatch.isEmpty()) {
				// 保存当前批次并开始新批次
				batches.add(new ArrayList<>(currentBatch));
				currentBatch.clear();
				currentBatchCharacterCount = 0;
			}

			// 将任务添加到当前批次
			currentBatch.add(taskDirectory);
			currentBatchCharacterCount += taskCharacterCount;

			logger.debug("任务 {} ({} 字符) 添加到批次，当前批次字符数: {}", taskDirectory, taskCharacterCount,
					currentBatchCharacterCount);
		}

		// 添加最后一个批次（如果不为空）
		if (!currentBatch.isEmpty()) {
			batches.add(currentBatch);
		}

		// 记录批次分组结果
		for (int i = 0; i < batches.size(); i++) {
			List<String> batch = batches.get(i);
			int batchTotalCharacters = batch.stream().mapToInt(this::getTaskCharacterCount).sum();
			logger.info("批次 {} 包含 {} 个任务，总字符数: {}", i + 1, batch.size(), batchTotalCharacters);
		}

		return batches;
	}

	/**
	 * 获取单个任务的字符数 读取任务目录中的output.md文件并计算字符数
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
				logger.warn("任务目录 {} 的output.md文件不存在，返回默认字符数", taskDirectory);
				return DEFAULT_TASK_CHARACTER_COUNT;
			}
		}
		catch (Exception e) {
			logger.error("读取任务 {} 的字符数失败", taskDirectory, e);
			return DEFAULT_TASK_CHARACTER_COUNT;
		}
	}

	/**
	 * 执行带有批次Map结果的Reduce步骤 利用InnerStorageTool将批次上下文聚合存储到统一文件，避免上下文过长
	 */
	private BaseAgent executeReduceStepWithBatch(ExecutionStep step, ExecutionContext context,
			List<String> batchTaskDirectories, int batchCounter) {

		// 保存原始ExecutionParams并临时修改

		// 保存原始ExecutionParams并临时修改
		String originalExecutionParams = context.getPlan().getExecutionParams();
		StringBuilder enhancedParams = new StringBuilder();
		if (originalExecutionParams != null && !originalExecutionParams.trim().isEmpty()) {
			enhancedParams.append(originalExecutionParams).append("\n\n");
		}

		// 添加简化的批次上下文信息
		enhancedParams.append("=== Reduce批次 ").append(String.format("%03d", batchCounter)).append(" 上下文 : \n");

		// 只包含output.md的内容，不包含状态数据
		for (String taskDirectory : batchTaskDirectories) {
			try {
				Path taskPath = Paths.get(taskDirectory);
				String taskId = taskPath.getFileName().toString();

				// 读取任务的output.md文件（Map阶段的输出）
				Path outputFile = taskPath.resolve("output.md");
				if (Files.exists(outputFile)) {
					String outputContent = Files.readString(outputFile);
					enhancedParams.append("=== 任务ID: ").append(taskId).append(" ===\n");
					enhancedParams.append(outputContent).append("\n");
					enhancedParams.append("=== 任务ID: ").append(taskId).append(" 结束 ===\n\n");
				}
			}
			catch (Exception e) {
				logger.error("读取Map任务输出失败: {}", taskDirectory, e);
			}
		}

		// 创建修改后的步骤
		// ExecutionStep enhancedStep = new ExecutionStep();
		// enhancedStep.setStepIndex(step.getStepIndex());
		// enhancedStep.setStepRequirement(step.getStepRequirement());

		try {
			// 临时设置增强的ExecutionParams
			context.getPlan().setExecutionParams(enhancedParams.toString());

			// 执行步骤
			BaseAgent stepExecutor = executeStep(step, context);

			logger.info("完成Reduce批次 {} 的处理，包含 {} 个Map任务", batchCounter, batchTaskDirectories.size());
			return stepExecutor;

		}
		finally {
			// 恢复原始ExecutionParams
			context.getPlan().setExecutionParams(originalExecutionParams);
		}
	}

	/**
	 * 关闭执行器，释放线程池资源
	 */
	public void shutdown() {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown();
		}
	}

	/**
	 * 重写父类的executeStep方法，为map任务执行时临时添加任务信息到ExecutionParams
	 */
	@Override
	protected BaseAgent executeStep(ExecutionStep step, ExecutionContext context) {
		// 直接调用父类方法，因为任务上下文参数注入已经在executeStepsWithTaskContext中处理
		return super.executeStep(step, context);
	}

	/**
	 * 执行带有任务上下文参数注入的步骤列表，并支持任务完成状态检查和重试 使用复制的ExecutionContext，避免修改原始上下文
	 * @param steps 要执行的步骤列表
	 * @param context 执行上下文
	 * @param taskDirectory 任务目录路径
	 * @return 最后一个执行的Agent
	 */
	private BaseAgent executeStepsWithTaskContext(List<ExecutionStep> steps, ExecutionContext context,
			String taskDirectory) {
		BaseAgent fileExecutor = null;

		// 2. 根据taskDirectory从对应目录找到input.md这个固定文件
		String taskId = "";
		String fileContent = "";

		try {
			// 提取任务ID (从taskDirectory路径中获取最后一个目录名)
			Path taskPath = Paths.get(taskDirectory);
			taskId = taskPath.getFileName().toString();

			// 读取input.md文件内容
			Path inputFile = taskPath.resolve("input.md");
			if (Files.exists(inputFile)) {
				fileContent = Files.readString(inputFile);
				logger.debug("成功读取任务文件内容，任务ID: {}, 内容长度: {} 字符", taskId, fileContent.length());
			}
			else {
				logger.warn("任务目录中不存在 input.md 文件: {}", inputFile);
				fileContent = "任务文件不存在";
			}
		}
		catch (Exception e) {
			logger.error("读取任务文件失败: {}", taskDirectory, e);
			fileContent = "读取任务文件时发生错误: " + e.getMessage();
		}

		// 3. 创建带有增强参数的ExecutionContext副本
		ExecutionContext copiedContext = createContextCopyWithEnhancedParams(context, taskId, fileContent);

		// 执行任务，支持重试机制
		int maxRetries = MAX_TASK_RETRY_COUNT;
		int currentRetry = 0;
		boolean taskCompleted = false;

		while (currentRetry <= maxRetries && !taskCompleted) {
			if (currentRetry > 0) {
				logger.warn("任务 {} 第 {} 次重试执行", taskId, currentRetry);
			}

			try {
				// 4. 使用复制的上下文执行步骤
				for (ExecutionStep step : steps) {
					BaseAgent stepExecutor = executeStep(step, copiedContext);
					if (stepExecutor != null) {
						fileExecutor = stepExecutor;
					}
				}

				// 5. 检查任务是否完成
				taskCompleted = checkTaskCompletion(taskDirectory, taskId);

				if (taskCompleted) {
					logger.info("任务 {} 执行成功", taskId);
					break;
				}
				else {
					logger.warn("任务 {} 执行未完成，检查到任务状态不是completed或缺少输出文件", taskId);
					currentRetry++;

					if (currentRetry <= maxRetries) {
						// 等待一段时间后重试
						try {
							Thread.sleep(BASE_RETRY_WAIT_MILLIS * currentRetry); // 递增等待时间
						}
						catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							logger.error("重试等待被中断", ie);
							break;
						}
					}
				}

			}
			catch (Exception e) {
				logger.error("执行任务 {} 时发生异常", taskId, e);
				currentRetry++;

				if (currentRetry <= maxRetries) {
					try {
						Thread.sleep(BASE_RETRY_WAIT_MILLIS * currentRetry);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						logger.error("重试等待被中断", ie);
						break;
					}
				}
			}
		}

		// 6. 最终检查任务状态
		if (!taskCompleted) {
			logger.error("任务 {} 在 {} 次重试后仍未完成", taskId, maxRetries);
			throw new RuntimeException("任务 " + taskId + " 执行失败，已达到最大重试次数");
		}

		return fileExecutor;
	}

	/**
	 * 创建ExecutionContext的副本并增强ExecutionParams
	 * @param originalContext 原始执行上下文
	 * @param taskId 任务ID
	 * @param fileContent 文件内容
	 * @return 增强后的ExecutionContext副本
	 */
	private ExecutionContext createContextCopyWithEnhancedParams(ExecutionContext originalContext, String taskId,
			String fileContent) {
		// 创建ExecutionContext副本
		ExecutionContext copiedContext = new ExecutionContext();

		// 复制基本属性
		copiedContext.setCurrentPlanId(originalContext.getCurrentPlanId());
		copiedContext.setRootPlanId(originalContext.getRootPlanId());
		copiedContext.setUserRequest(originalContext.getUserRequest());
		copiedContext.setResultSummary(originalContext.getResultSummary());
		copiedContext.setNeedSummary(originalContext.isNeedSummary());
		copiedContext.setSuccess(originalContext.isSuccess());
		copiedContext.setUseMemory(originalContext.isUseMemory());
		copiedContext.setThinkActRecordId(originalContext.getThinkActRecordId());

		// 复制工具上下文
		if (originalContext.getToolsContext() != null) {
			Map<String, String> copiedToolsContext = new HashMap<>(originalContext.getToolsContext());
			copiedContext.setToolsContext(copiedToolsContext);
		}

		// 创建Plan的副本并增强ExecutionParams
		PlanInterface copiedPlan = createPlanCopyWithEnhancedParams(originalContext.getPlan(), taskId, fileContent);
		copiedContext.setPlan(copiedPlan);

		return copiedContext;
	}

	/**
	 * 创建Plan的副本并增强ExecutionParams
	 * @param originalPlan 原始计划
	 * @param taskId 任务ID
	 * @param fileContent 文件内容
	 * @return 增强后的Plan副本
	 */
	private PlanInterface createPlanCopyWithEnhancedParams(PlanInterface originalPlan, String taskId,
			String fileContent) {
		// 根据Plan的实际类型创建副本
		PlanInterface copiedPlan;

		if (originalPlan instanceof MapReduceExecutionPlan) {
			MapReduceExecutionPlan originalMapReducePlan = (MapReduceExecutionPlan) originalPlan;
			MapReduceExecutionPlan copiedMapReducePlan = new MapReduceExecutionPlan();

			// 复制MapReduceExecutionPlan的所有属性
			copiedMapReducePlan.setCurrentPlanId(originalMapReducePlan.getCurrentPlanId());
			copiedMapReducePlan.setRootPlanId(originalMapReducePlan.getRootPlanId());
			copiedMapReducePlan.setTitle(originalMapReducePlan.getTitle());
			copiedMapReducePlan.setPlanningThinking(originalMapReducePlan.getPlanningThinking());
			copiedMapReducePlan.setUserRequest(originalMapReducePlan.getUserRequest());
			// 复制步骤结构（注意：这里复制的是引用，因为步骤本身在执行过程中不会被修改）
			copiedMapReducePlan.setSteps(originalMapReducePlan.getSteps());

			copiedPlan = copiedMapReducePlan;
		}
		else {
			// 处理其他类型的Plan，如ExecutionPlan
			ExecutionPlan originalExecutionPlan = (ExecutionPlan) originalPlan;
			ExecutionPlan copiedExecutionPlan = new ExecutionPlan();

			// 复制ExecutionPlan的所有属性
			copiedExecutionPlan.setCurrentPlanId(originalExecutionPlan.getCurrentPlanId());
			copiedExecutionPlan.setTitle(originalExecutionPlan.getTitle());
			copiedExecutionPlan.setPlanningThinking(originalExecutionPlan.getPlanningThinking());
			copiedExecutionPlan.setUserRequest(originalExecutionPlan.getUserRequest());

			// 复制步骤列表（注意：这里复制的是引用，因为步骤本身在执行过程中不会被修改）
			copiedExecutionPlan.setSteps(originalExecutionPlan.getSteps());

			copiedPlan = copiedExecutionPlan;
		}

		// 创建增强的ExecutionParams
		String originalExecutionParams = originalPlan.getExecutionParams();
		StringBuilder enhancedParams = new StringBuilder();
		if (originalExecutionParams != null && !originalExecutionParams.trim().isEmpty()) {
			enhancedParams.append(originalExecutionParams).append("\n\n");
		}
		enhancedParams.append("=== 当前任务上下文 ===\n");
		enhancedParams.append("任务ID: ").append(taskId).append("\n");
		enhancedParams.append("文件内容: ").append(fileContent).append("\n");
		enhancedParams.append("=== 任务上下文结束 ===");

		// 设置增强的ExecutionParams
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
	 * 检查任务是否完成
	 * @param taskDirectory 任务目录路径
	 * @param taskId 任务ID
	 * @return 任务是否完成
	 */
	private boolean checkTaskCompletion(String taskDirectory, String taskId) {
		try {
			Path taskPath = Paths.get(taskDirectory);

			// 检查status.json文件
			Path statusFile = taskPath.resolve("status.json");
			if (Files.exists(statusFile)) {
				String statusContent = Files.readString(statusFile);
				logger.debug("任务 {} 状态文件内容: {}", taskId, statusContent);

				// 检查状态是否为completed
				if (statusContent.contains("\"status\":\"completed\"")
						|| statusContent.contains("\"status\": \"completed\"")) {

					// 同时检查是否存在output.md文件
					Path outputFile = taskPath.resolve("output.md");
					if (Files.exists(outputFile)) {
						logger.debug("任务 {} 已完成，存在状态文件和输出文件", taskId);
						return true;
					}
					else {
						logger.warn("任务 {} 状态为completed但缺少output.md文件", taskId);
						return false;
					}
				}
				else {
					logger.debug("任务 {} 状态不是completed", taskId);
					return false;
				}
			}
			else {
				logger.warn("任务 {} 缺少status.json文件", taskId);
				return false;
			}

		}
		catch (Exception e) {
			logger.error("检查任务 {} 完成状态时发生错误", taskId, e);
			return false;
		}
	}

}
