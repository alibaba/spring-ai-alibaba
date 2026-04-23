# Spring AI Alibaba Admin - 最小启动模式 MySQL 版本 变更说明

## 一、分支信息

- **分支名称**: `feature/admin-minimal-mysql20260227`
- **主要提交**: `f1e1af9c4` - feat(server): 添加最小化配置模式并优化组件条件加载
- **目标**: 实现仅需 MySQL 即可启动的轻量级部署模式，降低开发和测试环境的部署门槛

---

## 二、核心变更内容

### 2.1 新增最小化配置文件

**文件**: `spring-ai-alibaba-admin-server-start/src/main/resources/application-minimal.yml`

该配置文件仅依赖 MySQL，禁用了所有其他中间件：
- 禁用 Redis（使用本地内存缓存降级）
- 禁用 Elasticsearch
- 禁用 RocketMQ
- 禁用 Nacos（可选）
- 禁用链路追踪（OTLP）

### 2.2 组件条件化加载改造

通过 Spring Boot 的 `@ConditionalOnProperty` 和 `@ConditionalOnBean` 注解，实现组件的按需加载：

| 组件 | 配置类 | 条件注解 | 配置项 |
|------|--------|----------|--------|
| **Elasticsearch** | `ElasticsearchConfig` | `@ConditionalOnProperty(prefix="spring.elasticsearch", name="uris")` | `spring.elasticsearch.uris` |
| **Nacos** | `NacosConfig` | `@ConditionalOnProperty(prefix="nacos", name="server-addr")` | `nacos.server-addr` |
| **RocketMQ** | `MqConfig` | `@ConditionalOnProperty(prefix="rocketmq", name="endpoints")` | `rocketmq.endpoints` |
| **Redis/Redisson** | `RedissonConfig` | `@ConditionalOnProperty(prefix="spring.data.redis", name="host")` | `spring.data.redis.host` |
| **MQ消费者** | `MqConsumerManager` | `@ConditionalOnBean(ClientConfiguration.class)` | 依赖 MQ 配置 |
| **文档索引处理器** | `DocumentIndexHandler` | `@ConditionalOnBean(MqConsumerManager.class)` | 依赖 MQ 消费者 |
| **链路追踪服务** | `TracingServiceImpl` | `@ConditionalOnBean(TracingRepository.class)` | 依赖 ES 仓库 |

### 2.3 RedisManager 本地缓存降级

**文件**: `RedisManager.java`

- 通过 `@Autowired(required = false)` 注入 `RedissonClient`
- 当 Redis 未配置时，自动使用本地内存缓存作为降级方案
- 支持的操作：
  - ✅ KV 存储（带 TTL）
  - ✅ 原子计数器
  - ✅ Set 操作
  - ✅ 分布式锁（本地 JVM 锁降级，仅限单实例）
- ⚠️ **限制**: 本地锁仅在单 JVM 实例内有效，多实例部署时需配置 Redis

### 2.4 MQ Producer 可选化

**文件**: `DocumentServiceImpl.java`

- `documentIndexProducer` 改为 `@Autowired(required = false)` 注入
- 当 MQ 未配置时，文档索引操作直接同步执行，不发送 MQ 消息

### 2.5 application.yml 默认配置调整

**默认启用的 Profile**: `minimal`

**变更内容**:
1. 所有中间件配置改为注释状态（可选配置）
2. 默认禁用 OTLP 链路追踪导出
3. 移除强制环境变量依赖，使用 Spring 配置覆盖机制

---

## 三、启动方式

### 方式一：使用 Minimal Profile（推荐）

```bash
# 进入启动模块
cd spring-ai-alibaba-admin-server-start

# 启动（自动使用 application-minimal.yml）
mvn spring-boot:run
```

### 方式二：命令行指定 Profile

```bash
# 启动时指定 minimal profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=minimal"
```

### 方式三：Jar 包运行

```bash
# 打包
mvn clean package -DskipTests

# 运行（使用 minimal 配置）
java -jar target/spring-ai-alibaba-admin-server-start-*.jar --spring.profiles.active=minimal
```

### 方式四：Docker 部署

```bash
# 使用环境变量覆盖数据库配置
docker run -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/admin \
           -e SPRING_DATASOURCE_USERNAME=admin \
           -e SPRING_DATASOURCE_PASSWORD=admin \
           -p 8080:8080 \
           saa-admin-server:latest
```

---

## 四、仅使用 MySQL 时可用的功能

### ✅ 可用功能

| 功能模块 | 说明 |
|----------|------|
| **Prompt 管理** | 完整的 Prompt 模板 CRUD、版本管理、调试功能 |
| **数据集管理** | 数据集创建、版本管理、数据项管理 |
| **评估器管理** | 评估器配置、模板系统、调试功能 |
| **实验管理** | 实验执行、结果分析、批量处理 |
| **知识库管理** | 知识库创建、文档管理（同步索引） |
| **模型配置** | 多模型支持（OpenAI/DashScope/DeepSeek） |
| **Plugin/Tool 管理** | 插件和工具的 CRUD（本地缓存） |

### ❌ 不可用的功能

| 功能模块 | 原因 | 影响 |
|----------|------|------|
| **链路追踪（Trace）** | 依赖 Elasticsearch | 无法查看服务调用链路、性能分析 |
| **可观测性 Dashboard** | 依赖 Elasticsearch | 无法使用 Trace 分析、服务监控 |
| **文档异步索引** | 依赖 RocketMQ | 文档上传后同步处理，可能阻塞请求 |
| **分布式锁** | 依赖 Redis | 多实例部署时可能出现并发问题 |
| **Prompt 动态更新（Nacos）** | 依赖 Nacos | 无法通过 Nacos 推送 Prompt 变更 |
| **Agent 应用接入** | 依赖 Nacos | 无法接入外部 Agent 应用进行观测 |

---

## 五、启用完整功能的配置

如需启用所有功能，需部署以下中间件：

```yaml
# 1. 激活完整配置 profile（需自行创建 application-full.yml）
spring:
  profiles:
    active: full

# 2. 或逐步启用所需组件
spring:
  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: ${SPRING_REDIS_PORT:6379}
  elasticsearch:
    uris: ${SPRING_ELASTICSEARCH_URIS:http://localhost:9200}

rocketmq:
  endpoints: ${ROCKETMQ_ENDPOINTS:localhost:8081}

nacos:
  server-addr: ${NACOS_SERVER_ADDR:localhost:8848}

management:
  otlp:
    tracing:
      export:
        enabled: true
        endpoint: http://localhost:4318/v1/traces
```

---

## 六、推荐的部署模式

| 场景 | 推荐模式 | 所需中间件 |
|------|----------|------------|
| **本地开发** | Minimal | MySQL |
| **功能测试** | Minimal + Redis | MySQL + Redis |
| **集成测试** | Full（单节点） | MySQL + Redis + ES + MQ |
| **生产环境** | Full（集群） | MySQL + Redis + ES + MQ + Nacos |

---

## 七、注意事项

1. **本地缓存限制**: Minimal 模式下使用本地内存缓存，重启后缓存数据会丢失
2. **分布式锁**: Minimal 模式下分布式锁退化为本地锁，多实例部署时需启用 Redis
3. **文档索引**: Minimal 模式下文档索引导步处理能力受限，大文件上传可能阻塞
4. **数据持久化**: 所有业务数据仍持久化到 MySQL，缓存数据可接受丢失

---

## 八、相关文件清单

### 新增文件
- `application-minimal.yml` - 最小化配置文件

### 修改文件
- `application.yml` - 默认启用 minimal profile，注释可选配置
- `ElasticsearchConfig.java` - 添加条件注解
- `NacosConfig.java` - 添加条件注解
- `MqConfig.java` - 添加条件注解
- `RedissonConfig.java` - 添加条件注解
- `MqConsumerManager.java` - 添加条件注解
- `DocumentIndexHandler.java` - 添加条件注解
- `RedisManager.java` - 支持本地缓存降级
- `DocumentServiceImpl.java` - MQ Producer 可选化
- `TracingServiceImpl.java` - 添加条件注解
- `PluginServiceImpl.java` - Bug 修复（OpenAPI 校验逻辑）

---

