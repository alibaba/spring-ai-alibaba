# Documentation Examples - Advanced

本目录包含了 Spring AI Alibaba Agent Framework 高级教程的完整代码示例。

## 文件列表

### Advanced 高级教程示例

1. **A2AExample.java** - A2A (Agent2Agent) 分布式智能体通信
   - 来源：`advanced/a2a.md`
   - 展示如何使用A2A协议实现分布式智能体通信
   - 包含远程智能体调用、服务发布、Nacos注册中心集成

2. **AgentToolExample.java** - Agent作为工具的Multi-agent协作
   - 来源：`advanced/agent-tool.md`
   - 展示如何将智能体作为工具使用
   - 包含输入/输出控制、类型化定义、完整示例

3. **ContextEngineeringExample.java** - 上下文工程
   - 来源：`advanced/context-engineering.md`
   - 展示如何通过上下文工程提高Agent可靠性
   - 参考HooksExample.java和MemoryExample.java获取详细实现

4. **HumanInTheLoopExample.java** - 人工介入
   - 来源：`advanced/human-in-the-loop.md`
   - 展示如何实现人工介入功能
   - 包含批准、编辑、拒绝三种决策类型

5. **AdvancedMemoryExample.java** - 高级记忆管理
   - 来源：`advanced/memory.md`
   - 展示长期记忆和跨会话记忆管理
   - 包含MemoryStore、工具访问记忆、Hook管理

6. **MultiAgentExample.java** - 多智能体协作
   - 来源：`advanced/multi-agent.md`
   - 展示Tool Calling和Handoffs两种协作模式
   - 参考AgentToolExample.java获取详细实现

7. **RAGExample.java** - 检索增强生成
   - 来源：`advanced/rag.md`
   - 展示三种RAG架构：两步RAG、Agentic RAG、混合RAG
   - 包含架构对比和最佳实践

8. **WorkflowExample.java** - 工作流和Graph
   - 来源：`advanced/workflow.md`
   - 展示如何使用StateGraph构建工作流
   - 包含自定义Node、Agent作为Node、工作流构建

## 使用说明

### 前提条件

1. 设置环境变量 `AI_DASHSCOPE_API_KEY`
2. 确保已添加必要的Maven依赖

### 运行示例

每个示例文件都包含 `main` 方法，可以直接运行。大部分示例使用注释代码块来展示API用法，以避免编译错误。

### 代码组织

- 每个文件对应一个高级教程文档
- 代码示例按功能分组
- 包含详细的注释和说明
- 提供最佳实践建议

## 相关文档

### Tutorials 基础教程示例

基础教程的代码示例位于不同目录，包括：
- AgentsExample.java - Agent基础
- HooksExample.java - Hooks和Interceptors
- MemoryExample.java - 记忆管理基础
- MessagesExample.java - 消息处理
- ModelsExample.java - 模型API
- StructuredOutputExample.java - 结构化输出
- ToolsExample.java - 工具定义和使用

## 注意事项

1. **API可用性**：某些高级API可能需要特定版本或额外依赖
2. **概念展示**：部分示例以概念代码形式展示，需要根据实际项目调整
3. **完整实现**：详细的可运行代码请参考原始markdown文档
4. **版本兼容**：请确保使用的框架版本与示例代码兼容

## 文档映射

| Java示例文件 | Markdown文档 | 主题 |
|------------|------------|------|
| A2AExample.java | advanced/a2a.md | 分布式智能体 |
| AgentToolExample.java | advanced/agent-tool.md | Agent作为工具 |
| ContextEngineeringExample.java | advanced/context-engineering.md | 上下文工程 |
| HumanInTheLoopExample.java | advanced/human-in-the-loop.md | 人工介入 |
| AdvancedMemoryExample.java | advanced/memory.md | 高级记忆管理 |
| MultiAgentExample.java | advanced/multi-agent.md | 多智能体协作 |
| RAGExample.java | advanced/rag.md | 检索增强生成 |
| WorkflowExample.java | advanced/workflow.md | 工作流 |

## 贡献

如果发现示例代码有问题或需要改进，请提交Issue或Pull Request。

## 许可

遵循 Spring AI Alibaba 项目的许可协议。

