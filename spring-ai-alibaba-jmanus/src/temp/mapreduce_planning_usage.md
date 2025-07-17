# MapReducePlanningTool 使用说明

## 概述

`MapReducePlanningTool` 是基于现有 `PlanningTool` 扩展的 MapReduce 模式计划执行工具。它支持分布式任务执行，包含 Map 阶段（并行执行）和 Reduce 阶段（结果聚合）。

## 功能特性

1. **Map 阶段**：支持多个并行执行的步骤
2. **Reduce 阶段**：将 Map 阶段的结果进行聚合
3. **代理类型指定**：可以为每个步骤指定特定的代理类型
4. **并行度控制**：Map 步骤支持设置并行执行实例数量
5. **多种节点类型**：支持并行节点、顺序执行节点和 MapReduce 节点
6. **灵活的步骤组织**：通过顶级 `steps` 结构支持复杂的执行流程

## 节点类型说明

### 1. 并行节点 (Parallel Node)
- **类型**: `parallel`
- **描述**: 包含的所有步骤将并行执行
- **适用场景**: 需要同时执行多个独立任务时

### 2. 顺序执行节点 (Sequential Node)
- **类型**: `sequential`
- **描述**: 包含的步骤将按顺序逐一执行
- **适用场景**: 需要按特定顺序执行任务，后续任务依赖前面任务的结果

### 3. MapReduce 节点 (MapReduce Node)
- **类型**: `mapreduce`
- **描述**: 包含 Map 和 Reduce 两个阶段的分布式计算节点
- **适用场景**: 需要处理大量数据的分布式计算任务

## JSON 参数格式

### 新的顶级结构格式

```json
{
  "command": "create",
  "planId": "planTemplate-1747706333036",
  "title": "增强版计划执行示例",
  "steps": [
    {
      "type": "parallel",
      "steps": [
        {
          "stepRequirement": "[BROWSER_AGENT] 从文件中取出用户，然后通过google搜索该用户，下载两页内容",
          "outputColumns": "第一页内容，第二页内容"
        },
        {
          "stepRequirement": "[API_AGENT] 通过API获取用户的基本信息",
          "outputColumns": "用户ID，用户名，注册时间"
        }
      ]
    },
    {
      "type": "sequential",
      "steps": [
        {
          "stepRequirement": "[DATA_AGENT] 清洗和标准化收集到的数据",
          "outputColumns": "清洗后的用户数据"
        },
        {
          "stepRequirement": "[VALIDATION_AGENT] 验证数据完整性和准确性",
          "outputColumns": "验证结果，错误报告"
        }
      ]
    },
    {
      "type": "mapreduce",
      "mapSteps": [
        {
          "stepRequirement": "[ANALYSIS_AGENT] 分析用户行为模式",
          "outputColumns": "行为模式分析结果",
          "parallelInstances": 3
        },
        {
          "stepRequirement": "[SOCIAL_AGENT] 提取社交网络信息",
          "outputColumns": "社交网络数据"
        }
      ],
      "reduceSteps": [
        {
          "stepRequirement": "[AGGREGATION_AGENT] 聚合所有分析结果",
          "outputColumns": "综合分析报告"
        }
      ]
    },
    {
      "type": "sequential",
      "steps": [
        {
          "stepRequirement": "[REPORT_AGENT] 生成最终报告和清理临时文件",
          "outputColumns": "最终报告"
        },
        {
          "stepRequirement": "[NOTIFICATION_AGENT] 发送结果通知",
          "outputColumns": "通知状态"
        }
      ]
    }
  ]
}
```
