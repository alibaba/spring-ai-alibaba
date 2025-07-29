# Spring AI Alibaba NL2SQL Web UI 启动指南

## 概述

这是一个基于 Vue 3 + Vite 构建的 Spring AI Alibaba NL2SQL 管理界面，提供了三个主要功能模块：

- **首页（NL2SQL演示）**: 自然语言转SQL查询演示
- **业务知识管理**: 管理企业知识引擎，配置业务术语、黑话和常用表达
- **语义模型配置**: 对数据集字段进行语义重新设定，提升智能体自动选择数据集和问答的准确性

## 项目结构

```
spring-ai-alibaba-nl2sql-web-ui/
├── src/
│   ├── components/
│   │   └── HeaderComponent.vue    # 公共头部组件
│   ├── views/
│   │   ├── Home.vue              # 首页（NL2SQL演示）
│   │   ├── BusinessKnowledge.vue # 业务知识管理页面
│   │   └── SemanticModel.vue     # 语义模型配置页面
│   ├── App.vue                   # 根组件
│   └── main.js                   # 入口文件
├── public/
├── index.html                    # HTML模板
├── package.json                  # 项目配置
├── vite.config.js               # Vite配置
└── README.md                    # 本文档
```

## 环境要求

- Node.js >= 16.0.0
- npm >= 8.0.0 或 yarn >= 1.22.0

## 快速开始

### 1. 安装依赖

在项目根目录执行：

```bash
# 使用 npm
npm install

# 或使用 yarn
yarn install
```

### 2. 启动开发服务器

```bash
# 使用 npm
npm run dev

# 或使用 yarn
yarn dev
```

启动成功后，浏览器会自动打开 http://localhost:3000

### 3. 构建生产版本

```bash
# 使用 npm
npm run build

# 或使用 yarn
yarn build
```

构建完成后，静态文件将生成在 `dist/` 目录中。

### 4. 预览生产版本

```bash
# 使用 npm
npm run preview

# 或使用 yarn
yarn preview
```

## 后端服务配置

前端应用需要连接到后端 Spring Boot 服务，默认配置如下：

- **后端服务地址**: http://localhost:8065
- **API路径**: `/api/*`
- **NL2SQL路径**: `/nl2sql/*`

如需修改后端服务地址，请编辑 `vite.config.js` 文件中的 proxy 配置：

```javascript
export default defineConfig({
  // ...
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8065', // 修改为实际的后端地址
        changeOrigin: true
      },
      '/nl2sql': {
        target: 'http://localhost:8065', // 修改为实际的后端地址
        target: 'http://your-backend-host:port', // 修改为你的后端地址
        changeOrigin: true
      },
      '/nl2sql': {
        target: 'http://your-backend-host:port', // 修改为你的后端地址
        changeOrigin: true
      }
    }
  }
})
```

## 功能说明

### 首页 - NL2SQL演示

- **功能**: 输入自然语言问题，系统自动转换为SQL查询
- **特性**: 
  - 实时流式响应显示
  - 数据源初始化
  - 查询结果可视化
  - SQL语法高亮
  - 支持Markdown格式输出

### 业务知识管理

- **功能**: 管理企业知识库，配置业务术语和同义词
- **操作**: 
  - 新增/编辑/删除业务知识
  - 搜索功能
  - 默认召回配置

### 语义模型配置

- **功能**: 配置数据集字段的语义信息
- **操作**:
  - 字段语义重命名
  - 同义词配置
  - 批量启用/禁用
  - 按数据集过滤

## 技术栈

- **前端框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **路由**: Vue Router 4
- **HTTP请求**: Fetch API
- **代码高亮**: highlight.js
- **Markdown解析**: marked
- **图标**: Bootstrap Icons
- **样式**: 自定义CSS (响应式设计)

## 开发规范

### 代码结构

- 使用 Vue 3 Composition API
- 组件采用单文件组件 (SFC) 格式
- 样式使用 scoped CSS
- 使用 CSS 变量定义主题色彩

### API 调用

- 使用原生 Fetch API
- 统一错误处理
- 支持 EventSource 流式数据

### 响应式设计

- 移动端优先设计
- 使用 CSS Grid 和 Flexbox
- 断点设置：768px (手机/平板分界)

## 部署说明

### 1. 构建项目

```bash
npm run build
```

### 2. 部署静态文件

将 `dist/` 目录中的文件部署到 Web 服务器（如 Nginx、Apache）

### 3. 配置反向代理

如果前后端分离部署，需要配置反向代理转发 API 请求。

**Nginx 配置示例**:

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 前端静态文件
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理
    location /api/ {
        proxy_pass http://backend-server:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /nl2sql/ {
        proxy_pass http://backend-server:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        # 支持 Server-Sent Events
        proxy_buffering off;
        proxy_cache off;
    }
}
```

## 故障排除

### 常见问题

1. **依赖安装失败**
   - 检查 Node.js 版本
   - 清除 npm 缓存：`npm cache clean --force`
   - 删除 node_modules 重新安装

2. **后端API连接失败**
   - 检查后端服务是否启动
   - 确认 proxy 配置是否正确
   - 检查防火墙和网络设置

3. **构建失败**
   - 检查代码语法错误
   - 确认所有依赖都已安装
   - 查看详细错误信息

### 开发调试

- 使用浏览器开发者工具
- 查看控制台错误信息
- 使用 Vue DevTools 调试 Vue 组件

## 开发模式说明

### 模拟数据模式

为了方便前端独立开发和测试，项目支持模拟数据模式：

- **业务知识管理页面**: 在 `src/views/BusinessKnowledge.vue` 中设置 `useMockData = true`
- **语义模型配置页面**: 在 `src/views/SemanticModel.vue` 中设置 `useMockData = true`

模拟数据模式下：
- 所有 CRUD 操作使用本地模拟数据
- 模拟网络延迟效果
- 无需启动后端服务即可测试界面功能

### 切换到生产模式

当后端服务可用时：
1. 确保后端服务运行在 `http://localhost:8065`
2. 将相应页面中的 `useMockData` 设置为 `false`
3. 重新启动开发服务器

## 许可证

本项目遵循 Apache 2.0 许可证。

## 联系方式

如有问题或建议，请提交 Issue 或联系开发团队。
