# MapReducePlanningTool 简化版使用示例

MapReducePlanningTool 接受JSON格式输入创建执行计划，必需参数包括command固定为create、planId作为唯一标识、title作为计划标题、steps数组包含执行节点，每个节点有type字段指定parallel并行执行、sequential顺序执行或mapreduce分布式处理三种类型，parallel和sequential节点包含steps数组，mapreduce节点包含mapSteps和reduceSteps数组，每个步骤对象包含stepRequirement描述具体任务内容建议用方括号标明代理类型、outputColumns描述期望输出结果用逗号分隔多个字段，工具会自动为步骤添加类型前缀并将输出信息嵌入描述中，支持在同一计划中组合多种节点类型形成复杂工作流。

## 概述

`MapReducePlanningTool` 是一个简化版的计划执行工具，支持三种节点类型：并行、顺序和 MapReduce。该工具专注于核心功能，避免复杂的配置参数。

## 支持的节点类型

### 1. 并行节点 (parallel)
所有步骤同时执行，适用于独立的任务。

### 2. 顺序节点 (sequential) 
步骤按顺序执行，适用于有依赖关系的任务。

### 3. MapReduce节点 (mapreduce)
分布式计算模式，包含Map和Reduce两个阶段。

## JSON 参数格式

每个步骤只需要两个基本参数：
- `stepRequirement`: 步骤要求描述（必需）
- `outputColumns`: 输出列描述（必需）

## 使用示例

### 1. 纯并行执行示例

```json
{
  "command": "create",
  "planId": "parallel-data-collection",
  "title": "并行数据收集任务",
  "steps": [
    {
      "type": "parallel",
      "steps": [
        {
          "stepRequirement": "[WEB_SCRAPER] 抓取电商网站的商品信息",
          "outputColumns": "商品ID,商品名称,价格,库存"
        },
        {
          "stepRequirement": "[API_CLIENT] 调用第三方API获取商品评价",
          "outputColumns": "商品ID,评分,评价数量"
        },
        {
          "stepRequirement": "[DATABASE_READER] 从数据库读取历史销售数据",
          "outputColumns": "商品ID,销售量,销售额"
        }
      ]
    }
  ]
}
```

### 2. 纯顺序执行示例

```json
{
  "command": "create",
  "planId": "sequential-data-process",
  "title": "顺序数据处理流程",
  "steps": [
    {
      "type": "sequential",
      "steps": [
        {
          "stepRequirement": "[DATA_COLLECTOR] 收集原始数据文件",
          "outputColumns": "文件路径,文件大小,记录数"
        },
        {
          "stepRequirement": "[DATA_CLEANER] 清洗和标准化数据",
          "outputColumns": "清洗后数据,错误记录,清洗报告"
        },
        {
          "stepRequirement": "[DATA_VALIDATOR] 验证数据质量",
          "outputColumns": "验证结果,质量评分,问题列表"
        },
        {
          "stepRequirement": "[REPORT_GENERATOR] 生成数据处理报告",
          "outputColumns": "报告文件,处理统计,结果摘要"
        }
      ]
    }
  ]
}
```

### 3. 纯MapReduce执行示例

```json
{
  "command": "create",
  "planId": "mapreduce-analysis",
  "title": "大数据分析任务",
  "steps": [
    {
      "type": "mapreduce",
      "mapSteps": [
        {
          "stepRequirement": "[USER_BEHAVIOR_ANALYZER] 分析用户行为数据",
          "outputColumns": "用户ID,行为模式,活跃度"
        },
        {
          "stepRequirement": "[PURCHASE_ANALYZER] 分析购买行为数据",
          "outputColumns": "用户ID,购买偏好,消费能力"
        },
        {
          "stepRequirement": "[CONTENT_ANALYZER] 分析内容浏览数据",
          "outputColumns": "用户ID,兴趣标签,浏览时长"
        }
      ],
      "reduceSteps": [
        {
          "stepRequirement": "[DATA_AGGREGATOR] 汇总所有分析结果",
          "outputColumns": "用户画像,综合评分,推荐策略"
        },
        {
          "stepRequirement": "[REPORT_GENERATOR] 生成最终分析报告",
          "outputColumns": "分析报告,关键洞察,业务建议"
        }
      ]
    }
  ]
}
```

### 4. 混合模式执行示例

```json
{
  "command": "create",
  "planId": "complex-data-pipeline",
  "title": "复杂数据处理流水线",
  "steps": [
    {
      "type": "parallel",
      "steps": [
        {
          "stepRequirement": "[SOCIAL_DATA_COLLECTOR] 收集社交媒体数据",
          "outputColumns": "用户ID,社交数据,发布时间"
        },
        {
          "stepRequirement": "[TRANSACTION_COLLECTOR] 收集交易数据",
          "outputColumns": "交易ID,用户ID,交易金额,交易时间"
        },
        {
          "stepRequirement": "[BEHAVIOR_COLLECTOR] 收集用户行为数据",
          "outputColumns": "用户ID,行为类型,时间戳"
        }
      ]
    },
    {
      "type": "sequential",
      "steps": [
        {
          "stepRequirement": "[DATA_MERGER] 合并所有收集的数据",
          "outputColumns": "统一用户数据,数据完整性报告"
        },
        {
          "stepRequirement": "[DATA_CLEANER] 清洗合并后的数据",
          "outputColumns": "清洗后数据,数据质量报告"
        }
      ]
    },
    {
      "type": "mapreduce",
      "mapSteps": [
        {
          "stepRequirement": "[PATTERN_ANALYZER] 分析用户行为模式",
          "outputColumns": "行为模式,模式强度,时间分布"
        },
        {
          "stepRequirement": "[PREFERENCE_ANALYZER] 分析用户偏好",
          "outputColumns": "偏好类别,偏好强度,变化趋势"
        }
      ],
      "reduceSteps": [
        {
          "stepRequirement": "[INSIGHT_GENERATOR] 生成用户洞察",
          "outputColumns": "用户洞察,商业价值,行动建议"
        }
      ]
    },
    {
      "type": "sequential",
      "steps": [
        {
          "stepRequirement": "[REPORT_GENERATOR] 生成最终报告",
          "outputColumns": "综合报告,执行摘要,数据图表"
        },
        {
          "stepRequirement": "[NOTIFICATION_SENDER] 发送完成通知",
          "outputColumns": "通知状态,接收确认"
        }
      ]
    }
  ]
}
```

## 关键特性

1. **简化配置**：只需要 `stepRequirement` 和 `outputColumns` 两个基本参数
2. **中文描述**：所有参数描述都使用中文，便于理解
3. **灵活组合**：可以在一个计划中混合使用不同类型的节点
4. **清晰标识**：每个步骤会自动添加类型标识符（[PARALLEL]、[SEQUENTIAL]、[MAP]、[REDUCE]）
5. **输出信息**：输出列信息会自动嵌入到步骤要求中

## 使用建议

1. **合理选择节点类型**：
   - 独立任务使用 `parallel`
   - 有依赖关系的任务使用 `sequential`
   - 大数据处理使用 `mapreduce`

2. **清晰的步骤描述**：
   - 在 `stepRequirement` 中明确指定代理类型（如 [BROWSER_AGENT]）
   - 详细描述要执行的任务内容

3. **准确的输出定义**：
   - 在 `outputColumns` 中明确列出期望的输出结果
   - 使用逗号分隔多个输出列

4. **合理的计划组织**：
   - 将相关的步骤组织在同一个节点中
   - 按照数据流和依赖关系安排节点顺序
