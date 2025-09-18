# inhouse_http_v4.0.1V2 分支说明

## 分支目的
此分支用于区分Vue前端调用和外部HTTP调用的接口处理。

## 主要修改

### 后端修改 (ManusController.java)
- 在 `executeByToolNameAsync` 方法中添加了 `isVueCall` 参数支持
- 添加了调用来源的日志记录，便于调试和监控
- 在响应中包含了调用来源信息

### 前端修改 (plan-act-api-service.ts)
- 在Vue前端调用API时自动添加 `isVueCall: true` 参数
- 确保后端能够识别调用来源

## 使用方式

### Vue前端调用
```javascript
// 前端会自动添加 isVueCall: true
const response = await PlanActApiService.executePlan(planTemplateId, params, files, replacementParams);
```

### 外部HTTP调用
```bash
curl -X POST http://localhost:8080/api/executor/executeByToolNameAsync \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "your-tool-name",
    "rawParam": "your-parameters"
  }'
```

## 日志示例
- Vue前端调用：`🖥️ Plan execution request from Vue frontend for tool: xxx`
- 外部HTTP调用：`🌐 Plan execution request from external HTTP client for tool: xxx`

## 创建时间
2025年1月18日

## 基于分支
inhouse_http_v4.0.1
