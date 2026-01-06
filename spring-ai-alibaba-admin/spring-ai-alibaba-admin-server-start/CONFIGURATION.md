# 配置说明

本文档说明如何配置 Spring AI Alibaba Admin Server 以支持不同环境（本地开发、Docker、Kubernetes）。

## 配置策略

项目使用 **环境变量覆盖 + 默认值** 的策略：

1. **默认值**：`application.yml` 中的默认值适用于本地开发环境（localhost）
2. **环境变量**：可以通过环境变量覆盖任何配置项
3. **Kubernetes**：通过 Deployment 中的环境变量注入 Kubernetes Service 名称

## 支持的配置项

### MySQL 数据库

| 环境变量 | 配置路径 | 默认值 | 说明 |
|---------|---------|--------|------|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:mysql://localhost:3306/admin?...` | MySQL 连接 URL |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `admin` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `admin` | 数据库密码 |

### Redis

| 环境变量 | 配置路径 | 默认值 | 说明 |
|---------|---------|--------|------|
| `SPRING_REDIS_HOST` | `spring.data.redis.host` | `localhost` | Redis 主机地址 |
| `SPRING_REDIS_PORT` | `spring.data.redis.port` | `6379` | Redis 端口 |
| `SPRING_REDIS_DATABASE` | `spring.data.redis.database` | `0` | Redis 数据库索引 |

### Elasticsearch

| 环境变量 | 配置路径 | 默认值 | 说明 |
|---------|---------|--------|------|
| `SPRING_ELASTICSEARCH_URIS` | `spring.elasticsearch.uris` | `http://localhost:9200` | Elasticsearch URI（Spring Data） |
| `SPRING_ELASTICSEARCH_URL` | `spring.elasticsearch.url` | `http://localhost:9200` | Elasticsearch URL（自定义客户端） |

### Nacos

| 环境变量 | 配置路径 | 默认值 | 说明 |
|---------|---------|--------|------|
| `NACOS_SERVER_ADDR` | `nacos.server-addr` | `localhost:8848` | Nacos 服务器地址 |

### RocketMQ

| 环境变量 | 配置路径 | 默认值 | 说明 |
|---------|---------|--------|------|
| `ROCKETMQ_ENDPOINTS` | `rocketmq.endpoints` | `localhost:18080` | RocketMQ Proxy 端点 |
| `ROCKETMQ_NAME_SERVER` | - | - | RocketMQ NameServer 地址（如果代码中使用） |
| `ROCKETMQ_DOCUMENT_INDEX_TOPIC` | `rocketmq.document-index-topic` | `topic_saa_studio_document_index` | 文档索引 Topic |
| `ROCKETMQ_DOCUMENT_INDEX_GROUP` | `rocketmq.document_index_group` | `group_saa_studio_document_index` | 文档索引 Group |

### LoongCollector (OTLP)

| 环境变量 | 配置路径 | 默认值 | 说明 |
|---------|---------|--------|------|
| `MANAGEMENT_OTLP_TRACING_EXPORT_ENDPOINT` | `management.otlp.tracing.export.endpoint` | `http://localhost:4318/v1/traces` | OTLP 追踪导出端点 |

## 使用场景

### 1. 本地开发

**默认配置即可使用**，前提是本地启动了所有中间件服务（通过 Docker Compose 或本地安装）。

```bash
# 启动本地中间件（使用 Docker Compose）
cd docker/middleware
./run.sh

# 运行应用（使用默认配置）
cd spring-ai-alibaba-admin-server-start
mvn spring-boot:run
```

如果需要覆盖某些配置，可以：

**方式一：使用环境变量**
```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/admin?..."
mvn spring-boot:run
```

**方式二：创建 application-dev.yml**
```bash
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
# 编辑 application-dev.yml，然后使用 dev profile 启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Docker 容器

构建镜像时，配置已经内置默认值。运行时通过环境变量覆盖：

```bash
docker run -d \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://mysql-host:3306/admin?..." \
  -e SPRING_REDIS_HOST="redis-host" \
  -e SPRING_ELASTICSEARCH_URIS="http://es-host:9200" \
  -e NACOS_SERVER_ADDR="nacos-host:8848" \
  spring-ai-admin-server:latest
```

### 3. Kubernetes 部署

Kubernetes Deployment 中已经配置了所有必要的环境变量，指向 Kubernetes Service 名称：

```yaml
env:
  - name: SPRING_DATASOURCE_URL
    value: "jdbc:mysql://mysql:3306/admin?..."
  - name: SPRING_REDIS_HOST
    value: "redis"
  - name: SPRING_ELASTICSEARCH_URIS
    value: "http://elasticsearch:9200"
  # ... 更多配置
```

部署时直接使用：
```bash
kubectl apply -f deploy/backend/backend-deployment.yaml
```

## 环境变量命名规则

Spring Boot 环境变量命名规则：
- 将配置路径中的 `.` 替换为 `_`
- 全部大写
- 例如：`spring.datasource.url` → `SPRING_DATASOURCE_URL`

## 验证配置

### 查看当前配置

应用启动后，可以通过 Actuator 端点查看配置：

```bash
# 查看所有配置（需要认证）
curl http://localhost:8080/actuator/configprops

# 查看环境变量
curl http://localhost:8080/actuator/env
```

### 检查日志

应用启动日志会显示连接信息，可以检查是否正确连接到各个服务。

## 常见问题

### Q: 如何知道某个配置是否被环境变量覆盖？

A: 查看启动日志，Spring Boot 会显示配置来源。或者使用 Actuator 的 `/actuator/env` 端点。

### Q: 本地开发时如何连接到远程服务？

A: 使用环境变量覆盖：
```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://remote-host:3306/admin?..."
mvn spring-boot:run
```

### Q: 如何在 Kubernetes 中修改配置？

A: 编辑 `deploy/backend/backend-deployment.yaml` 中的环境变量，然后重新部署：
```bash
kubectl apply -f deploy/backend/backend-deployment.yaml
kubectl rollout restart deployment/spring-ai-admin-server -n spring-ai-admin
```

### Q: 配置了环境变量但没生效？

A: 检查：
1. 环境变量名称是否正确（全大写，使用下划线）
2. 应用是否重启（某些配置需要重启才能生效）
3. 查看日志确认配置值

## 最佳实践

1. **本地开发**：使用默认配置，通过 Docker Compose 启动本地中间件
2. **Docker 部署**：通过环境变量覆盖配置
3. **Kubernetes 部署**：使用 ConfigMap 或 Secret 管理敏感配置，通过环境变量注入
4. **生产环境**：使用 Secret 存储密码等敏感信息，不要硬编码在配置文件中

