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
package com.alibaba.cloud.ai.example.manus.workflow;

import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 基于MapReduce的内容总结工作流 用于对大量内容进行智能提取和结构化总结
 */
@Component
public class SummaryWorkflow implements ISummaryWorkflow {

	private static final Logger logger = LoggerFactory.getLogger(SummaryWorkflow.class);

	@Autowired
	private PlanningFactory planningFactory;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 内容总结执行计划模板
	 */
	private static final String SUMMARY_PLAN_TEMPLATE = """
			{
			  "planType": "advanced",
			  "planId": "%s",
			  "title": "内容智能的对大文件进行汇总，最后总结时需要把合并后的文件名在总结时输出出来",
			  "steps": [
			    {
			      "type": "mapreduce",
			      "dataPreparedSteps": [
			        {
			          "stepRequirement": "[MAPREDUCE_DATA_PREPARE_AGENT] 使用map_reduce_tool，对 %s 进行内容分割",
			          "terminateColumns": "%s"
			        }
			      ],
			      "mapSteps": [
			        {
			          "stepRequirement": "[MAPREDUCE_MAP_TASK_AGENT] 分析文件，找到与 %s 相关的关键信息，信息要全面，包含所有数据，事实和观点等，全面的信息，不要遗漏",
			          "terminateColumns": "%s"
			        }
			      ],
			      "reduceSteps": [
			        {
			          "stepRequirement": "[MAPREDUCE_REDUCE_TASK_AGENT] 合并该分片的信息到文件中，在保持信息完整性的前提下，合并所有内容，同时也要去掉未找到内容的那些结果",
			          "terminateColumns": "%s"
			        }
			      ],
				  "postProcessSteps": [
					{
					  "stepRequirement": "[MAPREDUCE_FIN_AGENT] 当导出完成后，读取导出的结果后，完整输出所有导出的内容",
					  "terminateColumns": "file_path"
					}
				  ]

			    }
			  ]
			}
			""";

	/**
	 * 执行内容总结工作流
	 * @param planId 调用者的计划ID，确保子进程能找到对应的目录
	 * @param fileName 文件名
	 * @param content 文件内容
	 * @param queryKey 查询关键词
	 * @param thinkActRecordId Think-act记录ID，用于子计划执行追踪
	 * @return 总结结果的Future
	 */
	public CompletableFuture<String> executeSummaryWorkflow(String parentPlanId, String fileName, String content,
			String queryKey, Long thinkActRecordId, String terminateColumnsString) {

		// 1. 构建MapReduce执行计划，使用调用者的planId
		MapReduceExecutionPlan executionPlan = buildSummaryExecutionPlan(parentPlanId, fileName, content, queryKey,
				terminateColumnsString);

		// 2. 直接执行计划，传递thinkActRecordId
		return executeMapReducePlanWithContext(parentPlanId, executionPlan, thinkActRecordId);
	}

	/**
	 * 构建基于MapReduce的总结执行计划
	 * @param planId 使用调用者提供的计划ID，确保子进程能找到对应的目录
	 * @param fileName 文件名
	 * @param content 文件内容（暂未直接使用，但保留为扩展参数）
	 * @param queryKey 查询关键词
	 */
	private MapReduceExecutionPlan buildSummaryExecutionPlan(String parentPlanId, String fileName, String content,
			String queryKey, String terminateColumnsString) {

		try {
			// 使用调用者提供的planId，而不是生成新的
			logger.info("Building summary execution plan with provided planId: {}", parentPlanId);

			// 生成计划JSON，使用传入的planId
			String planJson = String.format(SUMMARY_PLAN_TEMPLATE, parentPlanId, // 计划ID
					fileName, // dataPreparedSteps 文件名
					terminateColumnsString, // dataPreparedSteps terminateColumns
					queryKey, // mapSteps 查询关键词
					terminateColumnsString, // mapSteps terminateColumns
					terminateColumnsString, // reduceSteps terminateColumns
					terminateColumnsString // postProcessSteps terminateColumns（会自动加上 ,
											// fileURL）
			);

			// 解析JSON为MapReduceExecutionPlan对象
			MapReduceExecutionPlan plan = objectMapper.readValue(planJson, MapReduceExecutionPlan.class);
			// terminateColumns 直接在 JSON 模板中配置，无需在此处设置

			return plan;

		}
		catch (Exception e) {
			logger.error("构建总结执行计划失败，planId: {}", parentPlanId, e);
			throw new RuntimeException("构建MapReduce总结执行计划失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 执行MapReduce计划 - 支持子计划上下文
	 */
	private CompletableFuture<String> executeMapReducePlanWithContext(String rootPlanId,
			MapReduceExecutionPlan executionPlan, Long thinkActRecordId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				// Generate a unique sub-plan ID using PlanIdDispatcher, similar to
				// generatePlan method

				String subPlanId = planIdDispatcher.generateSubPlanId(rootPlanId, thinkActRecordId);

				logger.info("Generated sub-plan ID: {} for parent plan: {}, think-act record: {}", subPlanId,
						rootPlanId, thinkActRecordId);

				// 获取规划协调器，使用生成的子计划ID
				PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(subPlanId);

				// 创建执行上下文
				ExecutionContext context = new ExecutionContext();
				context.setCurrentPlanId(subPlanId);
				context.setRootPlanId(rootPlanId);
				context.setThinkActRecordId(thinkActRecordId);

				// 更新执行计划的ID为子计划ID
				executionPlan.setCurrentPlanId(subPlanId);
				executionPlan.setRootPlanId(rootPlanId);
				context.setPlan(executionPlan);
				context.setNeedSummary(false);
				context.setUserRequest("执行基于MapReduce的内容智能总结");

				// 设置think-act记录ID以支持子计划执行
				if (thinkActRecordId != null) {
					context.setThinkActRecordId(thinkActRecordId);
				}

				// 执行计划（跳过创建计划步骤，直接执行）
				planningCoordinator.executeExistingPlan(context);

				logger.info("MapReduce总结计划执行成功，子计划ID: {}, 父计划ID: {}", subPlanId, rootPlanId);

				List<ExecutionStep> allSteps = context.getPlan().getAllSteps();
				ExecutionStep lastStep = allSteps.get(allSteps.size() - 1);
				return "getContent 执行成功 ， 执行的结果日志： " + lastStep.getResult();
			}
			catch (Exception e) {
				logger.error("MapReduce总结计划执行失败", e);
				return "❌ MapReduce内容总结执行失败: " + e.getMessage();
			}
		});
	}

}
