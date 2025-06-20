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
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		logger.info("执行 MapReduce 节点，Data Prepared 步骤: {}, Map 步骤: {}, Reduce 步骤: {}",
				mrNode.getDataPreparedStepCount(), mrNode.getMapStepCount(), mrNode.getReduceStepCount());

		BaseAgent executor = lastExecutor;

		// 1. 串行执行 Data Prepared 阶段
		if (CollectionUtil.isNotEmpty(mrNode.getDataPreparedSteps())) {
			executor = executeDataPreparedPhase(mrNode.getDataPreparedSteps(), context, executor);
		}

		// 2. 并行执行 Map 阶段
		if (CollectionUtil.isNotEmpty(mrNode.getMapSteps())) {
			executor = executeMapPhase(mrNode.getMapSteps(), context);
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
	private BaseAgent executeMapPhase(List<ExecutionStep> mapSteps, ExecutionContext context) {
		logger.info("并行执行 Map 阶段，共 {} 个步骤", mapSteps.size());

		List<CompletableFuture<BaseAgent>> futures = new ArrayList<>();

		for (ExecutionStep step : mapSteps) {
			CompletableFuture<BaseAgent> future = CompletableFuture.supplyAsync(() -> {
				return executeStep(step, context);
			}, executorService);
			futures.add(future);
		}

		// 等待所有 Map 步骤完成
		BaseAgent lastExecutor = null;
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

		logger.info("Map 阶段执行完成");
		return lastExecutor;
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

}
