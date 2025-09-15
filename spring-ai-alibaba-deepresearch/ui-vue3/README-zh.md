# Spring AI Alibaba DeepResearch Web UI Startup Guide

### 概述

这是一个基于 Vue 3 + Vite 构建的 Spring AI Alibaba DeepResearch 管理界面，提供 AI 驱动的任务自动化和研究能力。项目包含后端服务和现代化的 Web UI，用于深度研究任务。

**主要特性：**
- 🤖 **AI 驱动研究**: 智能任务规划的自动化研究
- 💬 **交互式聊天界面**: 与 AI 智能体的实时对话
- 📊 **任务管理**: 分步执行和进度跟踪
- 🔍 **多源搜索**: 集成 Tavily、Jina 等搜索 API
- 📝 **报告生成**: 自动化研究报告创建和导出
- 🌐 **国际化**: 支持多语言

### 架构图

![架构图](../../docs/imgs/deepresearch-workflow.png)

> 上图展示了 deepresearch 的核心模块分层与主要调用关系。

### 主要流程图

![主要流程图](../../docs/imgs/202506302113562.png)

> 上图展示了用户请求在 deepresearch 系统中的主要流转流程。

### 项目结构

```
spring-ai-alibaba-deepresearch/
├── ui-vue3/                      # 前端 Vue 3 应用
│   ├── src/
│   │   ├── components/           # 可复用组件
│   │   ├── views/               # 页面组件
│   │   ├── router/              # Vue Router 配置
│   │   ├── base/                # 基础工具
│   │   └── utils/               # 工具函数
│   ├── public/
│   ├── package.json
│   └── vite.config.js
```

### 环境要求
- **前端**: Node.js >= 16.0.0, npm >= 8.0.0 或 yarn >= 1.22.0
- **可选**: Docker, Redis, Elasticsearch

### 快速开始


#### 启动

```bash
# 进入 UI 目录
cd ui-vue3

# 安装依赖
pnpm install

# 启动开发服务器
npm run dev

```

前端将在 http://localhost:5173/ui 启动

#### 生产环境构建

```bash
# 构建前端
cd ui-vue3
npm run build

# 复制构建文件到后端资源目录
cp -r dist/* ../src/main/resources/static/
```

### 技术栈

- Vue 3 (Composition API)
- Vite
- Ant Design Vue
- TypeScript
- Ant Design X Vue
- Vue Router 4


### 许可证

本项目遵循 Apache License 2.0 许可证。

### 联系方式

如有问题或建议，请提交 Issue 或联系开发团队。
