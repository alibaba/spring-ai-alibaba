package com.alibaba.cloud.ai.example.manus.workflow;

import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.SequentialNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 基于MapReduce的内容总结工作流
 * 用于对大量内容进行智能提取和结构化总结
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
          "title": "内容智能总结计划 - %s",
          "steps": [
            {
              "type": "mapreduce",
              "dataPreparedSteps": [
                {
                  "stepRequirement": "[DEFAULT_AGENT] 使用inner_storage_tool将原始内容保存并进行初步分析",
                  "outputColumns": "文件路径,内容长度,关键词匹配度"
                },
                {
                  "stepRequirement": "[DEFAULT_AGENT] 使用inner_storage_tool分析内容结构，识别章节和重要段落",
                  "outputColumns": "章节标题,段落数量,重要性评分"
                },
                {
                  "stepRequirement": "[DEFAULT_AGENT] 使用inner_storage_tool预处理和分割内容，为Map阶段做准备",
                  "outputColumns": "数据块ID,块大小,预处理状态"
                }
              ],
              "mapSteps": [
                {
                  "stepRequirement": "[DEFAULT_AGENT] 使用inner_storage_tool按关键词'%s'提取相关信息片段",
                  "outputColumns": "%s"
                },
                {
                  "stepRequirement": "[DEFAULT_AGENT] 使用inner_storage_tool对提取的信息进行相关性评分和分类",
                  "outputColumns": "信息片段,相关性评分,分类标签"
                }
              ],
              "reduceSteps": [
                {
                  "stepRequirement": "[TEXT_FILE_AGENT] 使用inner_storage_tool将所有提取的信息汇总为结构化数据",
                  "outputColumns": "%s"
                },
                {
                  "stepRequirement": "[DEFAULT_AGENT] 使用inner_storage_tool生成最终的智能总结报告",
                  "outputColumns": "总结报告文件路径,质量评分,建议改进点"
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
    public CompletableFuture<String> executeSummaryWorkflow(String fileName, String content, 
                                                           String queryKey, List<String> columns) {
        
        // 1. 构建MapReduce执行计划
        MapReduceExecutionPlan executionPlan = buildSummaryExecutionPlan(fileName, content, queryKey, columns);
        
        // 2. 直接执行计划
        return executeMapReducePlan(executionPlan);
    }
    
    /**
     * 构建基于MapReduce的总结执行计划
     */
    private MapReduceExecutionPlan buildSummaryExecutionPlan(String fileName, String content, 
                                                           String queryKey, List<String> columns) {
        
        // 生成新的计划ID
        String planId = planIdDispatcher.generatePlanId();
        
        try {
            // 格式化列名
            String columnString = String.join(",", columns);
            
            // 生成计划JSON
            String planJson = String.format(SUMMARY_PLAN_TEMPLATE, 
                planId, 
                fileName,
                queryKey,
                columnString,
                columnString
            );
            
            // 解析JSON为MapReduceExecutionPlan对象
            MapReduceExecutionPlan plan = objectMapper.readValue(planJson, MapReduceExecutionPlan.class);
            
            // 设置额外的执行参数
            plan.setExecutionParams(String.format("""
                原始文件名: %s
                内容长度: %d 字符
                查询关键词: %s
                期望输出列: %s
                执行模式: MapReduce智能总结
                """, fileName, content.length(), queryKey, String.join(", ", columns)));
            
            // 设置计划思路
            plan.setPlanningThinking("""
                智能内容总结执行策略（MapReduce模式）：
                1. 数据准备阶段（DataPrepared）：内容预处理、结构分析、数据分割准备
                2. Map阶段：并行提取关键信息和评分分类
                3. Reduce阶段：汇总数据并生成最终报告
                4. 质量控制：评分和改进建议
                5. 优势：支持大文件处理，提高并行效率
                """);
            
            // 在执行步骤中嵌入实际内容
            embedContentIntoSteps(plan, fileName, content, queryKey, columns);
            
            return plan;
            
        } catch (Exception e) {
            logger.error("构建总结执行计划失败", e);
            // 降级到简单计划
            return buildFallbackPlan(planId, fileName, content, queryKey, columns);
        }
    }
    
    /**
     * 在执行步骤中嵌入实际内容和工具调用
     */
    private void embedContentIntoSteps(MapReduceExecutionPlan plan, String fileName, String content, 
                                     String queryKey, List<String> columns) {
        
        // 获取所有步骤并添加具体的工具调用信息
        List<ExecutionStep> allSteps = plan.getAllSteps();
        
        for (int i = 0; i < allSteps.size(); i++) {
            ExecutionStep step = allSteps.get(i);
            String requirement = step.getStepRequirement();
            
            // 数据准备阶段的步骤处理
            if (requirement.contains("保存并进行初步分析")) {
                step.setStepRequirement(requirement + " " + String.format("""
                    {
                        "action": "append",
                        "file_name": "content_analysis_%s.md",
                        "content": "# 内容分析报告\\n\\n## 文件信息\\n- 原始文件: %s\\n- 内容长度: %d字符\\n- 查询关键词: %s\\n\\n## 内容预览\\n```\\n%s\\n```"
                    }
                    """, plan.getPlanId(), fileName, content.length(), queryKey, 
                    content.length() > 500 ? content.substring(0, 500) + "..." : content));
                    
            } else if (requirement.contains("分析内容结构，识别章节和重要段落")) {
                step.setStepRequirement(requirement + " " + String.format("""
                    {
                        "action": "append",
                        "file_name": "content_structure_%s.md",
                        "content": "# 内容结构分析\\n\\n## 结构识别\\n- 总字符数: %d\\n- 预计章节数: %d\\n- 段落分析: 基于换行符识别\\n\\n## 内容分割策略\\n1. 按段落分割\\n2. 按关键词密度分组\\n3. 重要性评分标准"
                    }
                    """, plan.getPlanId(), content.length(), Math.max(1, content.split("\\n\\n").length)));
                    
            } else if (requirement.contains("预处理和分割内容，为Map阶段做准备")) {
                step.setStepRequirement(requirement + " " + String.format("""
                    {
                        "action": "append",
                        "file_name": "data_preparation_%s.json",
                        "content": "{\\"preparation_info\\": {\\"original_content_length\\": %d, \\"query_key\\": \\"%s\\", \\"target_columns\\": %s, \\"data_blocks\\": %d, \\"status\\": \\"prepared\\"}}"
                    }
                    """, plan.getPlanId(), content.length(), queryKey, 
                    columns.toString().replace("'", "\""), Math.max(1, content.length() / 1000)));
                    
            // Map阶段的步骤处理
            } else if (requirement.contains("按关键词")) {
                step.setStepRequirement(requirement + " " + String.format("""
                    {
                        "action": "get_content",
                        "file_name": "content_analysis_%s.md",
                        "query_key": "%s",
                        "columns": %s
                    }
                    """, plan.getPlanId(), queryKey, columns.toString().replace("'", "\"")));
                    
            } else if (requirement.contains("相关性评分和分类")) {
                step.setStepRequirement(requirement + " " + String.format("""
                    {
                        "action": "get_content",
                        "file_name": "content_structure_%s.md",
                        "query_key": "信息片段的相关性和分类",
                        "columns": ["信息片段", "相关性评分", "分类标签"]
                    }
                    """, plan.getPlanId()));
                    
            // Reduce阶段的步骤处理
            } else if (requirement.contains("汇总为结构化数据")) {
                step.setStepRequirement(requirement + " " + String.format("""
                    {
                        "action": "get_content",
                        "file_name": "data_preparation_%s.json",
                        "query_key": "汇总所有Map阶段的处理结果",
                        "columns": %s
                    }
                    """, plan.getPlanId(), columns.toString().replace("'", "\"")));
                    
            } else if (requirement.contains("生成最终的智能总结报告")) {
                step.setStepRequirement(requirement + " " + String.format("""
                    {
                        "action": "append",
                        "file_name": "final_summary_report_%s.md",
                        "content": "# 最终智能总结报告\\n\\n## 执行摘要\\n- 计划ID: %s\\n- 查询关键词: %s\\n- 目标列: %s\\n\\n## 处理状态\\n✅ 数据准备阶段完成\\n✅ Map阶段信息提取完成\\n✅ Reduce阶段数据汇总完成\\n\\n## 质量评估\\n[待AI分析填充]"
                    }
                    """, plan.getPlanId(), plan.getPlanId(), queryKey, String.join(", ", columns)));
            }
        }
    }
    
    /**
     * 构建降级计划（当JSON解析失败时）
     */
    private MapReduceExecutionPlan buildFallbackPlan(String planId, String fileName, String content, 
                                                    String queryKey, List<String> columns) {
        
        MapReduceExecutionPlan plan = new MapReduceExecutionPlan(planId, "降级内容总结计划");
        
        // 创建简单的顺序节点
        SequentialNode simpleNode = new SequentialNode();
        
        ExecutionStep step = new ExecutionStep();
        step.setStepIndex(0);
        step.setStepRequirement(String.format("""
            [DEFAULT_AGENT] 使用inner_storage_tool执行简单内容总结：
            {
                "action": "append",
                "file_name": "simple_summary_%s.md",
                "content": "# 简单总结\\n\\n文件: %s\\n关键词: %s\\n目标列: %s\\n\\n内容摘要:\\n%s"
            }
            """, planId, fileName, queryKey, String.join(", ", columns),
            content.length() > 1000 ? content.substring(0, 1000) + "..." : content));
        step.setOutputColumns(String.join(",", columns));
        
        simpleNode.addStep(step);
        plan.addSequentialNode(simpleNode);
        
        return plan;
    }
    
    /**
     * 执行MapReduce计划
     */
    private CompletableFuture<String> executeMapReducePlan(MapReduceExecutionPlan executionPlan) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取规划协调器
                PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(
                    executionPlan.getPlanId());
                
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
                    """, 
                    executionPlan.getPlanId(),
                    executionPlan.getTitle(),
                    executionPlan.getTotalStepCount(),
                    executionPlan.getNodeCount(),
                    executionPlan.getPlanId(),
                    executionPlan.getPlanId(),
                    executionPlan.getPlanId(),
                    executionPlan.getPlanId(),
                    executionPlan.getExecutionParams()
                );
                
            } catch (Exception e) {
                logger.error("MapReduce总结计划执行失败", e);
                return "❌ MapReduce内容总结执行失败: " + e.getMessage();
            }
        });
    }
    
    /**
     * 创建快速总结工作流（用于小文件）
     */
    public CompletableFuture<String> executeQuickSummary(String fileName, String content, 
                                                        String queryKey, List<String> columns) {
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
                              "stepRequirement": "[DEFAULT_AGENT] 使用inner_storage_tool执行快速总结",
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
                
            } catch (Exception e) {
                logger.error("快速总结执行失败", e);
                return "❌ 快速总结执行失败: " + e.getMessage();
            }
        });
    }
}
