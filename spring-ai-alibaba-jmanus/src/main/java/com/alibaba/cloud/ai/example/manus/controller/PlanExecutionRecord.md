# PlanExecutionRecord JSON 格式说明

本文档描述了 PlanExecutionRecord 的 JSON 输出格式及其字段说明。

## 数据结构概览

PlanExecutionRecord 包含四个主要部分：

1. 基本信息（Basic Info）
2. 计划结构（Plan Structure）
3. 执行过程数据（Execution Data）
4. 执行结果（Execution Result）

## 字段说明

### 基本信息

- `id`: 记录的唯一标识符（Long类型）
- `planId`: 计划的唯一标识符（String类型）
- `title`: 计划标题
- `userRequest`: 用户的原始请求
- `startTime`: 执行开始时间（ISO-8601格式）
- `endTime`: 执行结束时间（ISO-8601格式）

### 执行状态

- `currentStepIndex`: 当前执行到的步骤索引（从0开始）
- `completed`: 是否完成（boolean类型）
- `summary`: 执行总结或当前状态描述

### 步骤信息

- `steps`: 计划步骤列表，每个步骤包含智能体标识
  
### 智能体执行记录

- `agentExecutionSequence`: 智能体执行记录列表
  - `id`: 执行记录ID
  - `conversationId`: 对话ID
  - `agentName`: 智能体名称
  - `agentDescription`: 智能体描述
  - `startTime`: 开始时间
  - `endTime`: 结束时间
  - `maxSteps`: 最大步骤数
  - `currentStep`: 当前步骤
  - `status`: 执行状态
  - `isCompleted`: 是否完成
  - `isStuck`: 是否卡住
  - `agentRequest`: 智能体请求
  - `result`: 执行结果
  - `thinkActSteps`: 思考和行动步骤列表

## JSON 结构示例

```json
{
    "id": 1711624451711,
    "planId": "plan_1743142451689",
    "title": "Plan for: 通过百度查询一下最新的阿里巴巴股价",
    "userRequest": "通过百度查询一下最新的阿里巴巴股价",
    "startTime": "2025-03-28T14:14:11.711141",
    "endTime": "2025-03-28T14:14:45.324512",
    "currentStepIndex": 2,
    "completed": false,
    "summary": "正在执行中...",
    "steps": [
        "[BROWSER_AGENT] 打开百度搜索页面",
        "[BROWSER_AGENT] 搜索阿里巴巴股价信息",
        "[REACT_AGENT] 分析并提取股价数据"
    ],
    "agentExecutionSequence": [
        {
            "id": 1711624451712,
            "conversationId": "plan_1743142451689",
            "agentName": "BROWSER_AGENT",
            "agentDescription": "网页浏览代理",
            "startTime": "2025-03-28T14:14:11.712141",
            "endTime": "2025-03-28T14:14:15.324512",
            "maxSteps": 3,
            "currentStep": 1,
            "status": "COMPLETED",
            "isCompleted": true,
            "isStuck": false,
            "agentRequest": "打开百度搜索页面",
            "result": "成功打开百度首页",
            "thinkActSteps": [
                {
                    "id": 1711624451713,
                    "parentExecutionId": 1711624451712,
                    "thinkStartTime": "2025-03-28T14:14:11.713141",
                    "thinkEndTime": "2025-03-28T14:14:12.324512",
                    "actStartTime": "2025-03-28T14:14:12.324512",
                    "actEndTime": "2025-03-28T14:14:15.324512",
                    "thinkInput": "需要打开百度搜索页面",
                    "thinkOutput": "使用浏览器打开百度首页",
                    "actionNeeded": true,
                    "actionDescription": "打开浏览器并导航到百度首页",
                    "actionResult": "成功访问 https://www.baidu.com",
                    "status": "completed",
                    "toolName": "browser",
                    "toolParameters": "{\"url\": \"https://www.baidu.com\"}"
                }
            ]
        }
    ]
}
```

## 使用说明

1. 所有时间戳都使用 ISO-8601 格式
2. 状态值包括：COMPLETED、IN_PROGRESS、PENDING、ERROR 等
3. 智能体名称格式为大写加下划线，如 BROWSER_AGENT
4. 工具参数使用 JSON 字符串格式存储

## 最佳实践

1. 总是检查 `completed` 字段来确定执行是否完成
2. 使用 `currentStepIndex` 和 `steps.length` 来计算进度
