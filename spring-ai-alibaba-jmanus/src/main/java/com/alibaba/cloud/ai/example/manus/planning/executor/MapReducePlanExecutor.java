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
package com.alibaba.cloud.ai.example.manus.planning.executor;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanInterface;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.ExecutionNode;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceNode;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.SequentialNode;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.mapreduce.MapReduceTool;

import java.util.ArrayList;
import java.util.List;
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

	// 线程池用于并行执行
	private final ExecutorService executorService;

	public MapReducePlanExecutor(List<DynamicAgentEntity> agents, PlanExecutionRecorder recorder,
			AgentService agentService, LlmService llmService) {
		super(agents, recorder, agentService, llmService);
		this.executorService = Executors.newCachedThreadPool();
	}

	/**
	 * 执行整个 MapReduce 计划的所有步骤
	 * 
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
			recordPlanExecutionStart(context);
			List<ExecutionNode> steps = mapReducePlan.getSteps();

			if (CollectionUtil.isNotEmpty(steps)) {
				for (Object stepNode : steps) {
					if (stepNode instanceof SequentialNode) {
						lastExecutor = executeSequentialNode((SequentialNode) stepNode, context, lastExecutor);
					} else if (stepNode instanceof MapReduceNode) {
						lastExecutor = executeMapReduceNode((MapReduceNode) stepNode, context, lastExecutor);
					}
				}
			}

			context.setSuccess(true);
		} finally {
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
		logger.info("执行 MapReduce 节点，Data Prepared 步骤: {}, Map 步骤: {}, Reduce 步骤: {}",
				mrNode.getDataPreparedStepCount(), mrNode.getMapStepCount(), mrNode.getReduceStepCount());

		BaseAgent executor = lastExecutor;

		// 1. 串行执行 Data Prepared 阶段
		if (CollectionUtil.isNotEmpty(mrNode.getDataPreparedSteps())) {
			executor = executeDataPreparedPhase(mrNode.getDataPreparedSteps(), context, executor);
		}

		// 2. 并行执行 Map 阶段
		if (CollectionUtil.isNotEmpty(mrNode.getMapSteps())) {
			// 获取 SplitTool 的 ToolCallBackContext
			ToolCallBackContext toolCallBackContext = null;
			if (executor != null) {
				toolCallBackContext = executor.getToolCallBackContext("SplitTool");
			}
			executor = executeMapPhase(mrNode.getMapSteps(), context, toolCallBackContext);
		}

		// 3. 串行执行 Reduce 阶段
		if (CollectionUtil.isNotEmpty(mrNode.getReduceSteps())) {
			executor = executeReducePhase(mrNode.getReduceSteps(), context, executor);
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
	 * 并行执行 Map 阶段
	 */
	private BaseAgent executeMapPhase(List<ExecutionStep> mapSteps, ExecutionContext context,
			ToolCallBackContext toolCallBackContext) {
		logger.info("并行执行 Map 阶段，共 {} 个步骤", mapSteps.size());

		ToolCallBiFunctionDef callFunc = toolCallBackContext.getFunctionInstance();
		if (callFunc == null) {
			logger.error("ToolCallBiFunctionDef is null, cannot execute Map phase");
			return null;
		}
		if (!(callFunc instanceof MapReduceTool)) {
			logger.error("ToolCallBiFunctionDef is not SplitTool, cannot execute Map phase");
			return null;
		}
		MapReduceTool splitTool = (MapReduceTool) callFunc;

		List<CompletableFuture<BaseAgent>> futures = new ArrayList<>();
		BaseAgent lastExecutor = null;

		try {
			// 2. 获取任务目录列表（新的MapReduceTool返回任务目录路径）
			List<String> taskDirectories = splitTool.getSplitResults();
			
			if (taskDirectories.isEmpty()) {
				logger.error("没有找到任务目录，Map 阶段执行失败");
				throw new RuntimeException("没有找到任务目录，Map 阶段无法执行");
			} else {
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
			
		} catch (Exception e) {
			logger.error("执行 Map 阶段时发生错误", e);
			throw new RuntimeException("Map 阶段执行失败", e);
		}

		logger.info("Map 阶段执行完成");
		return lastExecutor;
	}
	/**
	 * 复制 mapSteps 列表，并为特定任务目录调整步骤要求，同时读取并包含文档片段内容
	 */
	private List<ExecutionStep> copyMapSteps(List<ExecutionStep> originalSteps, String taskDirectory) {
		List<ExecutionStep> copiedSteps = new ArrayList<>();

		// 读取任务目录中的 input.md 文件内容
		String documentContent = "";
		try {
			Path inputFile = Paths.get(taskDirectory, "input.md");
			if (Files.exists(inputFile)) {
				documentContent = Files.readString(inputFile);
				logger.debug("成功读取任务文档片段，长度: {} 字符", documentContent.length());
			} else {
				logger.warn("任务目录中不存在 input.md 文件: {}", inputFile);
				documentContent = "文档片段文件不存在";
			}
		} catch (Exception e) {
			logger.error("读取任务文档片段失败: {}", taskDirectory, e);
			documentContent = "读取文档片段时发生错误: " + e.getMessage();
		}

		for (ExecutionStep originalStep : originalSteps) {
			ExecutionStep copiedStep = new ExecutionStep();
			copiedStep.setStepIndex(originalStep.getStepIndex());

			// 调整步骤要求，只包含文档片段内容，不暴露路径和任务ID
			String originalRequirement = originalStep.getStepRequirement();
			StringBuilder modifiedRequirement = new StringBuilder();
			modifiedRequirement.append(originalRequirement);
			modifiedRequirement.append("\n\n=== 任务文档片段内容 ===\n");
			modifiedRequirement.append(documentContent);
			modifiedRequirement.append("\n=== 文档片段内容结束 ===");
			
			copiedStep.setStepRequirement(modifiedRequirement.toString());

			copiedSteps.add(copiedStep);
		}

		return copiedSteps;
	}

	/**
	 * 串行执行 Reduce 阶段
	 */
	private BaseAgent executeReducePhase(List<ExecutionStep> reduceSteps, ExecutionContext context,
			BaseAgent lastExecutor) {
		logger.info("串行执行 Reduce 阶段，共 {} 个步骤", reduceSteps.size());

		BaseAgent executor = lastExecutor;

		for (ExecutionStep step : reduceSteps) {
			BaseAgent stepExecutor = executeStep(step, context);
			if (stepExecutor != null) {
				executor = stepExecutor;
			}
		}

		logger.info("Reduce 阶段执行完成");
		return executor;
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
	 * 执行带有任务上下文参数注入的步骤列表
	 * 
	 * @param steps 要执行的步骤列表
	 * @param context 执行上下文
	 * @param taskDirectory 任务目录路径
	 * @return 最后一个执行的Agent
	 */
	private BaseAgent executeStepsWithTaskContext(List<ExecutionStep> steps, ExecutionContext context, String taskDirectory) {
		BaseAgent fileExecutor = null;
		
		// 1. 根据context.getPlan().getExecutionParams() 拿到一个contextMap
		String originalExecutionParams = context.getPlan().getExecutionParams();
		
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
			} else {
				logger.warn("任务目录中不存在 input.md 文件: {}", inputFile);
				fileContent = "任务文件不存在";
			}
		} catch (Exception e) {
			logger.error("读取任务文件失败: {}", taskDirectory, e);
			fileContent = "读取任务文件时发生错误: " + e.getMessage();
		}
		
		// 3. 把任务ID和文件内容作为参数放到contextMap里面
		StringBuilder enhancedParams = new StringBuilder();
		if (originalExecutionParams != null && !originalExecutionParams.trim().isEmpty()) {
			enhancedParams.append(originalExecutionParams).append("\n\n");
		}
		enhancedParams.append("=== 当前任务上下文 ===\n");
		enhancedParams.append("任务ID: ").append(taskId).append("\n");
		enhancedParams.append("文件内容: ").append(fileContent).append("\n");
		enhancedParams.append("=== 任务上下文结束 ===");
		
		// 临时设置增强的ExecutionParams
		context.getPlan().setExecutionParams(enhancedParams.toString());
		
		try {
			// 4. 执行步骤
			for (ExecutionStep step : steps) {
				BaseAgent stepExecutor = executeStep(step, context);
				if (stepExecutor != null) {
					fileExecutor = stepExecutor;
				}
			}
		} finally {
			// 恢复原始ExecutionParams
			context.getPlan().setExecutionParams(originalExecutionParams);
		}
		
		return fileExecutor;
	}

}
