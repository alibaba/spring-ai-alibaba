# Agent Integration Test

## 功能说明

我们已经成功将前端从后端读取agents的代码逻辑复用到 `json-editor-logic.ts` 中，用于展现可选的agents。

## 实现的功能

1. **Agent API 集成**
   - 导入了 `AgentApiService` 从 `/api/agent-api-service`
   - 导入了 `getAllAgents` 从 `/api/agent`
   - 两个API会并行调用以获取全面的agent数据

2. **数据结构**
   - 新增 `AvailableAgent` 接口定义agent选项格式
   - 包含 id, name, description, agentType 字段

3. **状态管理**
   - `availableAgents`: 存储从API获取的agent列表
   - `isLoadingAgents`: 加载状态标识
   - `agentOptions`: 计算属性，合并API数据和默认选项

4. **fallback机制**
   - 如果API调用失败，使用默认的agent类型列表
   - 默认包含：SWEAGENT, BROWSER_AGENT, FILE_AGENT, API_AGENT, CUSTOM

5. **UI更新**
   - agent选择下拉框现在显示动态获取的agents
   - 添加了加载状态显示
   - 添加了刷新agents按钮
   - 显示当前可用agents数量

## API调用逻辑

```typescript
// 同时调用两个API
const [configAgents, managementAgents] = await Promise.allSettled([
  AgentApiService.getAllAgents(),    // /api/agents
  getAllAgents()                     // /api/agent-management
])
```

## 数据处理

1. 将API返回的数据转换为统一格式
2. 去重处理（基于agent id）
3. 与默认agent类型合并
4. 确保UI始终有可用选项

## 使用方式

组件初始化时会自动加载agents，用户也可以通过刷新按钮手动重新加载。

## 测试方法

1. 启动前端项目
2. 打开包含JsonEditor的页面
3. 检查agent选择下拉框是否显示了从后端获取的agents
4. 测试刷新按钮功能
5. 验证在网络错误时是否使用默认选项
