# Spring AI Alibaba Starter Prompt Nacos

基于 Nacos Prompt的提示词（Prompt）管理 Spring Boot Starter，为 Spring AI Alibaba 应用提供**动态、集中化**的提示词管理能力。

## 功能特性

- **集中式 Prompt 管理**：通过 Nacos 统一管理 AI 应用的提示词，无需重启应用即可动态更新
- **多版本管理**：支持 Prompt 的多版本管理与灰度发布
- **标签化订阅**：支持按标签进行 Prompt 订阅与筛选
- **动态感知**：通过 Nacos 的长轮询机制自动感知变更并实时生效

## 系统架构

```
┌─────────────────────────────────────────────┐
│              Spring AI Alibaba App            │
│  ┌─────────────────────────────────────────┐  │
│  │   NacosPromptManagerAutoConfiguration   │  │
│  │         @Bean AiService                 │  │
│  └──────────────────┬──────────────────────┘  │
│                     │                         │
│  ┌──────────────────▼──────────────────────┐  │
│  │     NacosAiService (AiFactory创建)       │  │
│  └──────────────────┬──────────────────────┘  │
└─────────────────────┼─────────────────────────┘
                      │
                      ▼
         ┌─────────────────────────┐
         │   Nacos Server 3.2.0+   │
         │   (Prompt Registry)     │
         └─────────────────────────┘
```

## 快速开始

### 1. 环境准备

- JDK 17+
- Nacos Server 3.2.0 及以上版本（Prompt Registry 能力）
- Maven 3.6+

### 2. 添加依赖

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-prompt-nacos</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

### 3. 配置 Nacos 连接

在 `application.yml` 中配置 Nacos 服务端地址：

```yaml
spring:
  ai:
    alibaba:
      prompt:
        nacos:
          enabled: true                     # 是否启用（默认 true）
          server-addr: 127.0.0.1:8848       # Nacos 服务端地址（默认）
          namespace: public                 # Nacos 命名空间（默认 public）
```

### 4. 使用 AiService 管理 Prompt

自动配置会创建一个 `AiService` Bean，可以直接注入使用：

```java
import com.alibaba.nacos.api.ai.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    @Autowired
    private AiService aiService;

    /**
     * 获取 Prompt
     */
    public String getPrompt(String promptKey) {
        // 根据 key 获取提示词内容
        return aiService.getPrompt(promptKey);
    }

    /**
     * 获取指定版本的 Prompt
     */
    public String getPrompt(String promptKey, String version) {
        return aiService.getPrompt(promptKey, version);
    }
}
```

> 注：`AiService` 的具体 API 由 Nacos 客户端提供，支持的完整操作请参考 [Nacos Prompt Registry 文档](https://nacos.io/docs/latest/manual/user/ai/prompt-registry/)。

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.ai.alibaba.prompt.nacos.enabled` | `true` | 是否启用 Nacos Prompt 管理 |
| `spring.ai.alibaba.prompt.nacos.server-addr` | `127.0.0.1:8848` | Nacos 服务端地址 |
| `spring.ai.alibaba.prompt.nacos.namespace` | `public` | Nacos 命名空间 ID |

## 工作原理

1. **自动装配**：`NacosPromptManagerAutoConfiguration` 在满足 `NacosAiService.class` 在类路径上且配置启用时自动生效
2. **创建 AiService**：通过 `AiFactory.createAiService(properties)` 创建 Nacos AI 服务客户端
3. **通过 Nacos 动态管理 Prompt**：`AiService` 封装了对 Nacos Prompt Registry 的 API 调用，支持 Prompt 的增删改查和动态监听
4. **动态刷新**：Prompt 变更通过 Nacos 的长轮询机制实时推送到客户端，应用无需重启

## Nacos Prompt Registry 能力

本 Starter 对应的 Nacos 服务端（3.2.0+）提供了 Prompt Registry 能力，包括：

- **Prompt 存储**：以配置中心方式存储 Prompt，附带 agentId、modelId、版本、标签等元数据
- **多版本管理**：支持同一 Prompt 的多个并行版本管理
- **标签订阅**：基于标签的 Prompt 订阅机制
- **A/B 测试**：支持 Prompt 级别的 A/B 测试
- **Nacos Copilot**：集成 AI 助手，辅助 Prompt 的创建与优化

## 示例

### 完整的 Spring Boot 集成示例

```java
@SpringBootApplication
public class PromptNacosApplication {
    public static void main(String[] args) {
        SpringApplication.run(PromptNacosApplication.class, args);
    }
}
```

### 在应用中实时获取 Prompt

```java
@RestController
@RequestMapping("/prompt")
public class PromptController {

    @Autowired
    private AiService aiService;

    @GetMapping("/{key}")
    public String getPrompt(@PathVariable String key) {
        return aiService.getPrompt(key);
    }
}
```

## 依赖说明

- `spring-boot-starter`：Spring Boot 基础 Starter
- `nacos-client` (>= 3.2.0)：Nacos 客户端，提供 AiFactory、AiService 等 AI 相关 API

## 相关链接

- [Spring AI Alibaba](https://java2ai.com)
- [Nacos Prompt Registry 文档](https://nacos.io/docs/latest/manual/user/ai/prompt-registry/)
- [Nacos 官网](https://nacos.io)