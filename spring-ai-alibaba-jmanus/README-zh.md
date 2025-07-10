# Spring AI Alibaba JManus

<div align="center">

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![GitHub Stars](https://img.shields.io/github/stars/alibaba/spring-ai-alibaba.svg)](https://github.com/alibaba/spring-ai-alibaba/stargazers)

[English](./README.md) | 🌍 **中文**

**一个全面实现了 OpenManus 的多 Agent 框架，具备无限上下文能力**

*赋能各种用户，轻松构建复杂的多 Agent 系统，释放前所未有的生产力*

[关于](#-关于) • [快速开始](#-快速开始) • [如何贡献](#-如何贡献)

</div>

![image](https://github.com/user-attachments/assets/07feeb29-c410-4f56-89bf-532210bc1b63)

---

## 🎯 关于

JManus 是 [OpenManus](https://github.com/FoundationAgents/OpenManus) 模型的一个健壮、生产就绪的实现，构建在坚实的 Spring AI 基础之上。它使开发者能够以最少的配置创建复杂的 AI Agent 生态系统，同时确保企业级的可靠性和可伸缩性。

JManus 采用成熟的 Plan-Act 架构模式，支持**自定义 Agent**，并能智能地将**复杂任务分解**为由多个专业 Agent 协作完成的子任务。这种创新方法通过策略性的多 Agent 协同，实现了**无限的上下文处理**，超越了单模型上下文窗口的限制。

### 为什么选择 JManus？

- 🤖 **原生多 Agent 架构**：内置协作框架，支持用户自定义的 Agent 能力和专业角色。
- 🌊 **无限上下文处理**：通过智能的多 Agent 协作，克服单模型上下文限制，实现无限内容处理。
- 🎯 **卓越的 Plan-Act 模式**：完整实现 Plan-Act 范式，具有智能规划和执行分离的特点。
- 🔗 **MCP 集成**：原生支持模型上下文协议（Model Context Protocol），实现与外部服务和工具的无缝集成。
- 📜 **网页界面配置 Agent**：通过直观的网页管理界面轻松配置 agent，无需修改代码。

## 🚀 快速开始

在 5 分钟内启动并运行 JManus：

### 先决条件

- ☕ **Java 17+** (推荐 OpenJDK)
- 🌐 **DashScope API 密钥** (或替代的 AI 模型提供商)

### 1. 克隆并导航

```bash
git clone https://github.com/alibaba/spring-ai-alibaba.git
cd spring-ai-alibaba/spring-ai-alibaba-jmanus
```

### 2. 配置您的 API 密钥

```bash
# 设置您的 DashScope API 密钥
export DASHSCOPE_API_KEY=your_api_key_here
```

> 💡 **获取您的 DashScope API 密钥**: 访问 [阿里云百炼控制台](https://bailian.console.aliyun.com/?tab=model#/api-key) 获取免费 API 密钥。
>
> **使用其他提供商?** 在 `src/main/resources/application.yml` 中更新配置，以使用您偏好的 AI 模型平台。

### 3. 数据库配置（可选）

JManus 支持 H2（默认）和 MySQL 数据库。

#### 如何使用 MySQL

1. **创建 MySQL 数据库**：

   ```sql
   CREATE DATABASE openmanus_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **配置数据库连接**：
   在 `src/main/resources/application-mysql.yml` 中更新数据库配置：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://your-mysql-host:3306/openmanus_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8
       username: your_mysql_username
       password: your_mysql_password
   ```

3. **激活 MySQL 配置**：
   在 `src/main/resources/application.yml` 中更新配置：

   ```yaml
   spring:
     ...
     profiles:
       active: mysql  
   ```

> 💡 **注意**：应用程序将在首次启动时自动创建所需的表，使用 JPA 的 `ddl-auto: update` 配置。

### 4. 启动应用程序

**对于类 Unix 系统 (macOS, Linux):**
```bash
../mvnw spring-boot:run
```

**对于 Windows 系统:**
```bash
../mvnw.cmd spring-boot:run
```

### 5. 访问您的多 Agent 仪表盘

在您的浏览器中访问 `http://localhost:18080`。

🎉 **恭喜!** 您的多 Agent 系统现已上线并准备就绪。

## 🤝 如何贡献

我们热烈欢迎来自开发者社区的贡献！以下是您可以产生影响的方式：

### 贡献机会

您可以在我们的 [项目看板](https://github.com/orgs/alibaba/projects/24) 上找到可用的任务。

- 🐛 **报告 Bug**: [提交详细的问题报告](https://github.com/alibaba/spring-ai-alibaba/issues)
- 💡 **功能请求**: [提出创新的增强建议](https://github.com/alibaba/spring-ai-alibaba/issues)
- 📝 **文档**: 帮助我们提高文档的清晰度和完整性
- 🔧 **代码贡献**: [提交包含您改进的拉取请求](https://github.com/alibaba/spring-ai-alibaba/pulls)

### 开发环境设置

```bash
# Fork 并克隆仓库
git clone https://github.com/your-username/spring-ai-alibaba.git
cd spring-ai-alibaba/spring-ai-alibaba-jmanus

# 安装项目依赖
mvn clean install

# 应用代码格式化标准
mvn spotless:apply

# 启动开发服务器
mvn spring-boot:run
```

### 开发指南

- 遵循现有的代码风格和约定
- 为新功能编写全面的测试
- 为任何 API 变更更新文档
- 在提交 PR 前确保所有测试都通过

---

<div align="center">

**由 Spring AI Alibaba 团队用心打造 ❤️**

⭐ 如果 JManus 加速了您的开发之旅，请在 **GitHub 上给我们点亮一颗星**！

</div>
