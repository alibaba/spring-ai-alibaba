# Spring AI Alibaba Admin

> Spring AI Alibaba Repo: https://github.com/alibaba/spring-ai-alibaba
>
> Spring AI Alibaba Website: https://java2ai.com
>
> Spring AI Alibaba Website Repo: https://github.com/springaialibaba/spring-ai-alibaba-website

English | [中文](./README-zh.md) 

## Project Background

Agent Studio is an AI Agent development and evaluation platform based on Spring AI Alibaba, designed to provide developers and enterprises with a complete AI Agent lifecycle management solution. The platform supports a complete workflow from Prompt engineering, dataset management, evaluator configuration to experiment execution and result analysis, helping users quickly build, test, and optimize AI Agent applications.

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

## System Architecture

### Overall Architecture

![Overall Architecture](./docs/imgs/arch.png)

## 🚀 Quick Start

### Prerequisites
- 🐳 **Docker** (for containerized deployment) + **Docker Compose**: 2.0+
- ☕ **Java 17+** (for source code execution) + **Maven**: 3.8+
- 🌐 **AI Model Provider API Keys**, supporting OpenAI, DashScope, DeepSeek

### Running from Source Code

#### 1. Clone the Project

```bash
git clone https://github.com/spring-ai-alibaba/spring-ai-alibaba-admin.git
cd spring-ai-alibaba-admin
```

#### 2. Configure Your API Keys
Modify the model configuration in `spring-ai-alibaba-admin-server-start/model-config.yaml` according to your model provider.
- If you use DashScope, please refer to the model-config-dashscope.yaml template for configuration
- If you use DeepSeek, please refer to the model-config-deepseek.yaml template for configuration
- If you use OpenAI, please refer to the model-config-openai.yaml template for configuration
> 💡 **Get Your DashScope API Key**: Visit [Alibaba Cloud Bailian Console](https://bailian.console.aliyun.com/?tab=model#/api-key) to get a free API key.

#### 3. Nacos Configuration (Optional)
If you need to modify the Nacos address, please update the configuration in the `spring-ai-alibaba-admin-server-start/src/main/resources/application.yml` file
```yaml
nacos:
  server-addr: ${nacos-address}
```

### 4. Start SAA Admin

Choose one of the following two startup modes based on your environment requirements:

#### Mode 1: Minimal Startup Mode (Recommended for Development and Testing)

Only MySQL is required to start, no other middleware services needed.

**4.1.1 Start MySQL**

Recommended: Use Docker to quickly start MySQL (no local installation required):

```bash
# Run from project root to start dev mode (MySQL only)
make env-start MODE=dev

# Or run from docker/middleware directory
sh docker/middleware/run.sh dev
```

> For more details, refer to [docker/middleware/README.md](docker/middleware/README.md).

If using local MySQL, ensure MySQL 8.0+ is installed and create the database:

```sql
CREATE DATABASE IF NOT EXISTS saa_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**4.1.2 Configure Database Connection (Optional)**

To modify database configuration, edit `spring-ai-alibaba-admin-server-start/src/main/resources/application-minimal.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:saa_admin}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:your_password}
```

**4.1.3 Start Backend Service**

Navigate to the `spring-ai-alibaba-admin-server-start` directory and start the application (minimal configuration is used by default):

```bash
cd spring-ai-alibaba-admin-server-start
mvn spring-boot:run
```

> **Note**: The following features are unavailable in minimal mode:

> | Feature | Reason | Impact |
> |---------|--------|--------|
> | **Distributed Tracing** | Requires Elasticsearch | Cannot view service call chains, performance analysis, and troubleshooting |
> | **Observability Dashboard** | Requires Elasticsearch | Cannot use Trace analysis, service monitoring, and statistics charts |
> | **Async Document Indexing** | Requires RocketMQ | Documents are processed synchronously, large files may block requests |
> | **Distributed Locks** | Requires Redis | Concurrent issues may occur in multi-instance deployments (no impact for single instance) |
> | **Caching Service** | Requires Redis | Uses local memory cache, data is lost after restart |
> | **Prompt Dynamic Updates** | Requires Nacos | Cannot push Prompt changes to applications in real-time via Nacos |
> | **Agent Application Integration** | Requires Nacos | Cannot connect external Agent applications for observation and management |
> | **Configuration Center** | Requires Nacos | Cannot use externalized configuration and dynamic configuration refresh |

> 
> **Applicable Scenarios**: Local development, feature testing, demo environments
> **Not Applicable Scenarios**: Production environments, multi-instance deployments, scenarios requiring full observability

#### Mode 2: Full Startup Mode (Recommended for Production)

All middleware services are required (MySQL, Elasticsearch, Nacos, Redis, RocketMQ).

**4.2.1 Start Middleware Services**

Navigate to the `docker/middleware` directory in the project root and execute the startup script:

```bash
cd docker/middleware
sh run.sh
```

**4.2.2 Start Backend Service**

Navigate to the `spring-ai-alibaba-admin-server-start` directory and start with full configuration:

```bash
cd spring-ai-alibaba-admin-server-start
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

Or modify `spring.profiles.active` to `local`, `dev`, or `test` in `application.yml`.
#### 4.3 Start Frontend Service
Navigate to the `frontend` directory in the project root, read the corresponding README to install dependencies and configure the environment, then start the service:
```bash
cd packages/main
npm run dev
```

### 5. Access the Application

Open your browser and visit http://localhost:8000 to use the SAA Admin platform.

At this point, you can already manage, debug, evaluate, and observe prompts on the platform. If you expect your Spring AI Alibaba Agent application to integrate with Nacos for prompt loading and dynamic updates, and observe the online running status, you can refer to step 6 to configure your AI Agent application.

### 6. Connect Your AI Agent Application
In your Spring AI Alibaba Agent application, add the following dependencies
```xml
<dependencies>
    <!-- Introduce spring ai alibaba agent nacos proxy module -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-agent-nacos</artifactId>
        <version>{spring.ai.alibaba.version}</version>
    </dependency>

    <!-- Introduce observability module -->

    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-autoconfigure-arms-observation</artifactId>
        <version>{spring.ai.alibaba.version}</version>
    </dependency>
    
    
    <!-- For implementing various OTel related components, such as automatic loading of Tracer, Exporter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- For connecting micrometer generated metrics to otlp format -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-otlp</artifactId>
    </dependency>
    
    <!-- For replacing micrometer underlying trace tracer with OTel tracer -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-otel</artifactId>
    </dependency>
    
    <!-- For reporting spans generated by OTel tracer according to otlp protocol -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-autoconfigure-model-tool</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
``` 

Specify Nacos address and promptKey
```yaml
    spring.ai.alibaba.agent.proxy.nacos.serverAddr={replace nacos address, example: 127.0.0.1:8848}
    spring.ai.alibaba.agent.proxy.nacos.username={replace nacos username, example: nacos}
    spring.ai.alibaba.agent.proxy.nacos.password={replace nacos password, example: nacos}
    spring.ai.alibaba.agent.proxy.nacos.promptKey={replace with promptKey, example: mse-nacos-helper} 
```

Set observability parameters

```yaml
    management.otlp.tracing.export.enabled=true
    management.tracing.sampling.probability=1.0
    management.otlp.tracing.endpoint=http://{admin address}:4318/v1/traces
    management.otlp.metrics.export.enabled=false
    management.otlp.logging.export.enabled=false
    management.opentelemetry.resource-attributes.service.name=agent-nacos-prompt-test
    management.opentelemetry.resource-attributes.service.version=1.0
    spring.ai.chat.client.observations.log-prompt=true
    spring.ai.chat.observations.log-prompt=true
    spring.ai.chat.observations.log-completion=true
    spring.ai.image.observations.log-prompt=true
    spring.ai.vectorstore.observations.log-query-response=true
    spring.ai.alibaba.arms.enabled=true
    spring.ai.alibaba.arms.tool.enabled=true
    spring.ai.alibaba.arms.model.capture-input=true
    spring.ai.alibaba.arms.model.capture-output=true
```

## License

This project is open source under the Apache License 2.0 license.

## Contributing

We welcome submitting Issues and Pull Requests to help improve the project.
