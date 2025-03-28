# PlanExecutionRecord JSON 格式说明

本文档描述了 PlanExecutionRecord 的 JSON 输出格式详细示例。

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
    "progress": 66.67,
    "completed": false,
    "summary": "正在执行中...",
    "steps": [
        "[BROWSER_AGENT] 打开百度搜索页面",
        "[BROWSER_AGENT] 搜索阿里巴巴股价信息",
        "[REACT_AGENT] 分析并提取股价数据"
    ],
    "stepStatuses": [
        "completed",
        "completed",
        "in_progress"
    ],
    "stepAgents": [
        "BROWSER_AGENT",
        "BROWSER_AGENT",
        "REACT_AGENT"
    ],
    "stepNotes": [
        "成功打开百度首页",
        "搜索完成，找到相关信息",
        "正在分析数据..."
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
    ],
    "statusCounts": {
        "completed": 2,
        "in_progress": 1,
        "blocked": 0,
        "not_started": 0
    }
}
```

## 字段说明

### 基本信息
- `id`: 记录的唯一标识符 (Long)
- `planId`: 计划的唯一标识符 (String)
- `title`: 计划标题
- `userRequest`: 用户的原始请求
- `startTime`: 执行开始时间
- `endTime`: 执行结束时间（如果已完成）

### 执行状态
- `currentStepIndex`: 当前执行的步骤索引
- `progress`: 执行进度（百分比）
- `completed`: 是否完成
- `summary`: 执行总结

### 步骤信息
- `steps`: 计划步骤列表
- `stepStatuses`: 每个步骤的状态（completed/in_progress/blocked/not_started）
- `stepAgents`: 负责执行每个步骤的智能体
- `stepNotes`: 每个步骤的执行注释

### 智能体执行记录
- `agentExecutionSequence`: 智能体执行记录列表
  - `id`: 执行记录ID
  - `conversationId`: 对话ID
  - `agentName`: 智能体名称
  - `agentDescription`: 智能体描述
  - `startTime`: 开始时间
  - `endTime`: 结束时间
  - `status`: 执行状态
  - `result`: 执行结果
  - `thinkActSteps`: 思考-行动步骤记录

### 状态统计
- `statusCounts`: 各状态的统计数据
  - `completed`: 已完成的步骤数
  - `in_progress`: 进行中的步骤数
  - `blocked`: 被阻塞的步骤数
  - `not_started`: 未开始的步骤数

## 注意事项

1. 所有时间字段都使用 ISO-8601 格式
2. 进度（progress）是一个 0-100 的浮点数
3. ID 字段采用 Long 类型，由时间戳和随机数组合生成
4. 每个步骤都有对应的状态、执行智能体和执行注释
5. 智能体执行记录包含完整的思考和行动过程
