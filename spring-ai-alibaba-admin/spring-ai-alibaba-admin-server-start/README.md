# Spring AI Alibaba Admin Server

> Spring AI Alibaba Repo: https://github.com/alibaba/spring-ai-alibaba
>
> Spring AI Alibaba Website: https://java2ai.com
>
> Spring AI Alibaba Website Repo: https://github.com/springaialibaba/spring-ai-alibaba-website

English  | [中文](./README-zh.md)  

## Project Overview

Spring AI Alibaba Admin Server is a backend service for AI Agent management platform built on Spring Boot 3.x, providing complete RESTful API support for Agent Studio. The service supports core functionalities including Prompt management, dataset management, evaluator configuration, experiment execution, result analysis, and observability.

## Core Features

### 🤖 Prompt Management
- **Prompt Template Management**: Create, update, and delete Prompt templates
- **Version Control**: Support for Prompt version management and history tracking
- **Real-time Debugging**: Provide online Prompt debugging and streaming responses
- **Session Management**: Support for multi-turn conversation session management

### 📊 Dataset Management
- **Dataset Creation**: Support for importing and creating datasets in multiple formats
- **Version Management**: Dataset version control and history management
- **Data Item Management**: Fine-grained data item CRUD operations
- **Create from Trace**: Support for creating datasets from OpenTelemetry trace data

### ⚖️ Evaluator Management
- **Evaluator Configuration**: Support for creating and configuring various evaluators
- **Template System**: Provide evaluator templates and custom evaluation logic
- **Debugging Features**: Support for online evaluator debugging and testing
- **Version Management**: Evaluator version control and release management

### 🧪 Experiment Management
- **Experiment Execution**: Automated execution of evaluation experiments
- **Result Analysis**: Detailed experiment result analysis and statistics
- **Experiment Control**: Support for starting, stopping, restarting, and deleting experiments
- **Batch Processing**: Support for batch experiment execution and result comparison

### 📈 Observability
- **Trace Tracking**: Integrated OpenTelemetry providing complete trace tracking
- **Service Monitoring**: Support for service list and overview statistics
- **Trace Analysis**: Provide detailed Trace details and Span analysis

### 🔧 Model Configuration
- **Multi-model Support**: Support for mainstream AI models including OpenAI, DashScope, DeepSeek
- **Configuration Management**: Unified configuration and management of model parameters
- **Dynamic Switching**: Support for dynamic updates of model configuration at runtime

## Quick Start

### Prerequisites
- **JDK 17+**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Elasticsearch 9.x**
- **Nacos 2.x**

#### 1. Clone the Project

```bash
git clone https://github.com/spring-ai-alibaba/spring-ai-alibaba-admin.git
cd admin
```

#### 2. Configure Your API Keys
Modify the model configuration in `spring-ai-alibaba-admin-server/model-config.yaml` according to your model provider.
- If you use DashScope, please refer to the model-config-dashscope.yaml template for configuration
- If you use DeepSeek, please refer to the model-config-deepseek.yaml template for configuration
- If you use OpenAI, please refer to the model-config-openai.yaml template for configuration
> 💡 **Get Your DashScope API Key**: Visit [Alibaba Cloud Bailian Console](https://bailian.console.aliyun.com/?tab=model#/api-key) to get a free API key.

#### 3. Nacos Configuration (Optional)
If you need to modify the Nacos address, please update the configuration in the `spring-ai-alibaba-admin-server/src/main/resources/application.yml` file
```yaml
nacos:
  server-addr: ${nacos-address}
```

### 4. Start SAA Admin
Execute the startup script in the root directory. This script will help you start the database-related services

```bash
sh start.sh
```
Start the application in the spring-ai-alibaba-admin-server directory
```bash
mvn spring-boot:run
```

### 5. Access the Application

Open your browser and visit http://localhost:8080 to use the SAA Admin platform.

At this point, you can already manage, debug, evaluate, and observe prompts on the platform. If you expect your Spring AI Alibaba Agent application to integrate with Nacos for prompt loading and dynamic updates, and observe the online running status, you can refer to step 6 to configure your AI Agent application.

## Configuration

### Database Configuration
```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/admin
    username: admin
    password: admin
```

### Elasticsearch Configuration
```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

### Nacos Configuration
```yaml
nacos:
  server-addr: 127.0.0.1:8848
```

### Observability Configuration
```yaml
management:
  otlp:
    tracing:
      export:
        enabled: true
      endpoint: http://localhost:4318/v1/traces
```

## License

This project is open source under the Apache License 2.0 license.

## Contributing

We welcome submitting Issues and Pull Requests to help improve the project.
