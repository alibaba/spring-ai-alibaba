package com.alibaba.cloud.ai.example.manus.workflow;

import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
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
public class SummaryWorkflow {

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
			            "stepRequirement": "[DEFAULT_AGENT] 使用map_reduce_tool，对 %s 进行内容分割",
			            "outputColumns": "文件名"
			        }
			      ],
			      "mapSteps": [
			        {
			            "stepRequirement": "[DEFAULT_AGENT] 分析文件，找到与 %s 相关的关键信息，以 %s 为标题提取为列表后输出到文件，信息要全面，包含所有数据，事实和观点等，全面的信息，不要遗漏",
			            "outputColumns": "文件名"
			        }
			      ],
			      "reduceSteps": [
			        {
			            "stepRequirement": "[DEFAULT_AGENT] 合并该分片的信息到文件中，在保持信息完整性的前提下，合并所有内容，同时也要去掉未找到内容的那些结果",
			            "outputColumns": "文件名"
			        }
			      ]
			    }
			  ]
			}
			""";

	/**
	 * 执行内容总结工作流
	 * @param fileName 文件名
	 * @param content 文件内容
	 * @param queryKey 查询关键词
	 * @param columns 输出列名
	 * @return 总结结果的Future
	 */
	public CompletableFuture<String> executeSummaryWorkflow(String fileName, String content, String queryKey,
			List<String> columns) {

		// 1. 构建MapReduce执行计划
		MapReduceExecutionPlan executionPlan = buildSummaryExecutionPlan(fileName, content, queryKey, columns);

		// 2. 直接执行计划
		return executeMapReducePlan(executionPlan);
	}

	/**
	 * 构建基于MapReduce的总结执行计划
	 */
	private MapReduceExecutionPlan buildSummaryExecutionPlan(String fileName, String content, String queryKey,
			List<String> columns) {

		// 生成新的计划ID
		String planId = planIdDispatcher.generatePlanId();

		try {
			// 格式化列名
			String columnString = String.join(",", columns);

			// 生成计划JSON
			String planJson = String.format(SUMMARY_PLAN_TEMPLATE, planId, fileName, queryKey, columnString);

			// 解析JSON为MapReduceExecutionPlan对象
			MapReduceExecutionPlan plan = objectMapper.readValue(planJson, MapReduceExecutionPlan.class);

			return plan;

		}
		catch (Exception e) {
			logger.error("构建总结执行计划失败", e);
			throw new RuntimeException("构建MapReduce总结执行计划失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 执行MapReduce计划
	 */
	private CompletableFuture<String> executeMapReducePlan(MapReduceExecutionPlan executionPlan) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				// 获取规划协调器
				PlanningCoordinator planningCoordinator = planningFactory
					.createPlanningCoordinator(executionPlan.getPlanId());

				// 创建执行上下文
				ExecutionContext context = new ExecutionContext();
				context.setPlanId(executionPlan.getPlanId());
				context.setPlan(executionPlan);
				context.setNeedSummary(true);
				context.setUserRequest("执行基于MapReduce的内容智能总结");

				// 执行计划（跳过创建计划步骤，直接执行）
				planningCoordinator.executeExistingPlan(context);

				logger.info("MapReduce总结计划执行成功: {}", executionPlan.getPlanId());

				// 返回执行状态和结果摘要
				return String.format("""
						✅ MapReduce内容总结执行完成

						📋 执行计划信息:
						- 计划ID: %s
						- 计划标题: %s
						- 总步骤数: %d
						- 节点数量: %d

						📊 处理结果:
						- 数据准备阶段：内容预处理和结构分析完成
						- Map阶段：并行信息提取和分类完成
						- Reduce阶段：数据汇总和报告生成完成

						💡 建议文件:
						- content_analysis_%s.md - 初步分析结果
						- content_structure_%s.md - 结构分析结果
						- data_preparation_%s.json - 数据准备信息
						- final_summary_report_%s.md - 最终总结报告

						📈 执行参数: %s
						""", executionPlan.getPlanId(), executionPlan.getTitle(), executionPlan.getTotalStepCount(),
						executionPlan.getNodeCount(), executionPlan.getPlanId(), executionPlan.getPlanId(),
						executionPlan.getPlanId(), executionPlan.getPlanId(), executionPlan.getExecutionParams());

			}
			catch (Exception e) {
				logger.error("MapReduce总结计划执行失败", e);
				return "❌ MapReduce内容总结执行失败: " + e.getMessage();
			}
		});
	}

	/**
	 * 创建快速总结工作流（用于小文件）
	 */
	public CompletableFuture<String> executeQuickSummary(String fileName, String content, String queryKey,
			List<String> columns) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				String planId = planIdDispatcher.generatePlanId();

				// 使用简化的JSON模板
				String quickPlanJson = String.format("""
						{
						  "planType": "advanced",
						  "planId": "%s",
						  "title": "快速内容总结 - %s",
						  "steps": [
						    {
						      "type": "SEQUENTIAL",
						      "steps": [
						        {
						          "stepRequirement": "[DEFAULT_AGENT] 使用inner_storage_content_tool执行快速总结",
						          "outputColumns": "%s"
						        }
						      ]
						    }
						  ]
						}
						""", planId, fileName, String.join(",", columns));

				MapReduceExecutionPlan quickPlan = objectMapper.readValue(quickPlanJson, MapReduceExecutionPlan.class);

				// 嵌入工具调用
				ExecutionStep step = quickPlan.getAllSteps().get(0);
				step.setStepRequirement(step.getStepRequirement() + " " + String.format("""
						{
						    "action": "append",
						    "file_name": "quick_summary_%s.md",
						    "content": "# 快速总结\\n\\n文件: %s\\n关键词: %s\\n输出列: %s\\n\\n内容:\\n%s"
						}
						""", planId, fileName, queryKey, String.join(", ", columns), content));

				return executeMapReducePlan(quickPlan).get();

			}
			catch (Exception e) {
				logger.error("快速总结执行失败", e);
				return "❌ 快速总结执行失败: " + e.getMessage();
			}
		});
	}

}
