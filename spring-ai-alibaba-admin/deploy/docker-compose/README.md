# Docker Compose 部署指南

通过 Docker Compose 方式部署 Spring AI Alibaba Admin 平台。

## 目录结构

```
deploy/docker-compose/
├── docker-compose-service.yaml  # 主配置文件（包含所有中间件和应用服务）
├── .env                         # 环境变量配置
├── backend/
│   └── application-docker.yml   # 后端应用 Docker 专用配置
└── README.md                    # 本文档
```

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 8GB 可用内存
- 至少 20GB 可用磁盘空间

## 快速开始

### 1. 使用本地构建的镜像部署

从项目根目录执行：

```bash
# 构建镜像并部署所有服务
make deploy-compose BUILD_LOCAL=true
```

### 2. 使用远程镜像部署

```bash
# 从指定 registry 拉取镜像并部署
make deploy-compose REGISTRY=registry.cn-hangzhou.aliyuncs.com/yournamespace
```

## 服务说明

部署包含以下服务：

### 中间件服务

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 关系数据库 |
| Redis | 6379 | 缓存服务 |
| Elasticsearch | 9200, 9300 | 搜索引擎 |
| Kibana | 5601 | ES 可视化界面 |
| Nacos | 8848, 9848 | 配置中心 |
| RocketMQ NameServer | 9876 | 消息队列命名服务 |
| RocketMQ Broker | 10909, 10911, 10912 | 消息队列代理 |
| RocketMQ Proxy | 18080, 18081 | 消息队列代理服务 |
| LoongCollector | 4318 | OTLP 追踪收集器 |

### 应用服务

| 服务 | 端口 | 说明 |
|------|------|------|
| Backend | 8080 | 后端 API 服务 |
| Frontend | 80 | 前端 Web 界面 |

## 配置说明

### 环境变量配置（.env）

主要配置项：

```env
# 用户 ID（自动设置，用于容器权限）
UID=1000
GID=1000

# 时区设置
TZ=Asia/Shanghai

# MySQL 配置
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=admin
MYSQL_USER=admin
MYSQL_PASSWORD=admin

# 镜像配置（由 Makefile 自动设置）
FRONTEND_IMAGE=spring-ai-admin-frontend
BACKEND_IMAGE=spring-ai-admin-server
IMAGE_TAG=latest

# 数据目录
DATA_HOME=./data
```

### 数据持久化

所有数据默认存储在 `deploy/docker-compose/data/` 目录下：

```
data/
├── mysql/          # MySQL 数据
├── redis/          # Redis 数据
├── elasticsearch/  # Elasticsearch 数据
├── nacos/          # Nacos 配置数据
└── rocketmq/       # RocketMQ 消息数据
```

## 常用命令

### 部署相关

```bash
# 使用本地镜像部署
make deploy-compose BUILD_LOCAL=true

# 使用远程镜像部署
make deploy-compose REGISTRY=your-registry

# 停止服务（保留数据）
make undeploy-compose

# 停止服务并删除所有数据（危险操作！）
make undeploy-compose-clean
```

### 查看服务状态

```bash
# 查看所有服务状态
cd deploy/docker-compose
docker-compose -f docker-compose-service.yaml ps

# 查看服务日志
docker-compose -f docker-compose-service.yaml logs -f [service-name]

# 查看后端日志
docker-compose -f docker-compose-service.yaml logs -f backend

# 查看所有服务日志
docker-compose -f docker-compose-service.yaml logs -f
```

### 服务管理

```bash
# 重启某个服务
docker-compose -f docker-compose-service.yaml restart backend

# 停止某个服务
docker-compose -f docker-compose-service.yaml stop backend

# 启动某个服务
docker-compose -f docker-compose-service.yaml start backend
```

## 访问服务

部署成功后，可以通过以下地址访问各个服务：

- **前端界面**: http://localhost
- **后端 API**: http://localhost:8080
- **健康检查**: http://localhost:8080/actuator/health
- **Kibana**: http://localhost:5601
- **Nacos 控制台**: http://localhost:8848/nacos (用户名/密码: nacos/nacos)

## 启动顺序说明

服务会按照以下顺序启动（通过 `depends_on` 和 healthcheck 控制）：

1. **基础中间件层**：MySQL, Redis, Elasticsearch
2. **ES 初始化**：elasticsearch-init（创建索引）
3. **监控层**：LoongCollector, Kibana
4. **消息队列层**：RocketMQ NameServer → Broker → Proxy → Topic 初始化
5. **配置中心**：Nacos
6. **应用层**：Backend → Frontend

## 故障排查

### 查看服务健康状态

```bash
# 查看所有容器状态
docker ps -a | grep spring-ai-admin

# 检查容器健康状态
docker inspect --format='{{.State.Health.Status}}' spring-ai-admin-backend
```

### 常见问题

1. **MySQL 初始化失败**
   ```bash
   # 检查 MySQL 日志
   docker logs spring-ai-admin-mysql
   
   # 确保初始化脚本存在
   ls -la ../../docker/middleware/init/mysql/
   ```

2. **Elasticsearch 内存不足**
   ```bash
   # 调整 ES_JAVA_OPTS（修改 docker-compose-service.yaml）
   - "ES_JAVA_OPTS=-Xms512m -Xmx512m"  # 降低内存使用
   ```

3. **后端服务启动失败**
   ```bash
   # 查看后端日志
   docker logs -f spring-ai-admin-backend
   
   # 检查环境变量是否正确传递
   docker exec spring-ai-admin-backend env | grep SPRING
   ```

4. **端口冲突**
   ```bash
   # 检查端口占用
   lsof -i :8080
   
   # 修改 docker-compose-service.yaml 中的端口映射
   ports:
     - "8081:8080"  # 改为其他端口
   ```

## 清理数据

```bash
# 警告：这将删除所有数据！
make undeploy-compose-clean

# 或者手动清理
cd deploy/docker-compose
docker-compose -f docker-compose-service.yaml down -v
rm -rf data/
```

## 生产环境建议

1. **修改默认密码**：编辑 `.env` 文件，修改 MySQL、Redis 等服务的密码
2. **配置外部存储**：修改 `DATA_HOME` 指向持久化存储路径
3. **调整资源限制**：根据实际情况调整各服务的 CPU 和内存限制
4. **启用 HTTPS**：配置 Nginx 反向代理并添加 SSL 证书
5. **配置日志收集**：将日志输出到集中式日志系统
6. **备份策略**：定期备份 MySQL 和 Elasticsearch 数据

## 参考文档

- [Kubernetes 部署](../kubernetes/README.md)
- [本地开发模式](../../docker/middleware/README.md)
- [项目主文档](../../README.md)
