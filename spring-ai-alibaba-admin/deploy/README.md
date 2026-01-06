# Kubernetes Deployment Guide

本目录包含 Spring AI Alibaba Admin 项目的 Kubernetes 部署资源文件。

## 目录结构

```
deploy/
├── namespace.yaml                    # 命名空间定义
├── ingress.yaml                      # Ingress 配置（前端访问入口）
├── middleware/                       # 中间件服务
│   ├── mysql/                        # MySQL 数据库
│   ├── elasticsearch/                # Elasticsearch
│   ├── kibana/                       # Kibana
│   ├── redis/                        # Redis
│   ├── nacos/                        # Nacos 配置中心
│   ├── rocketmq/                     # RocketMQ 消息队列
│   └── loongcollector/               # LoongCollector 可观测性
├── frontend/                         # 前端服务
└── backend/                          # 后端服务
```

## 前置要求

1. Kubernetes 集群（版本 >= 1.20）
2. kubectl 已配置并可以访问集群
3. 已安装 Ingress Controller（如 Nginx Ingress Controller）
4. 存储类（StorageClass）已配置，用于持久化存储

## 部署步骤

### 1. 构建 Docker 镜像

首先需要构建前端和后端的 Docker 镜像：

```bash
# 构建前端镜像
cd frontend
docker build -t spring-ai-admin-frontend:latest .

# 构建后端镜像（从项目根目录）
cd /path/to/spring-ai-alibaba-admin
docker build -f spring-ai-alibaba-admin-server-start/Dockerfile -t spring-ai-admin-server:latest .

# 如果使用私有镜像仓库，需要推送镜像
docker tag spring-ai-admin-frontend:latest your-registry/spring-ai-admin-frontend:latest
docker tag spring-ai-admin-server:latest your-registry/spring-ai-admin-server:latest
docker push your-registry/spring-ai-admin-frontend:latest
docker push your-registry/spring-ai-admin-server:latest
```

### 2. 更新镜像名称（如果使用私有仓库）

编辑以下文件，将镜像名称替换为你的镜像仓库地址：

- `deploy/frontend/frontend-deployment.yaml`
- `deploy/backend/backend-deployment.yaml`

### 3. 创建命名空间

```bash
kubectl apply -f deploy/namespace.yaml
```

### 4. 创建 MySQL 初始化脚本 ConfigMap（重要！）

**必须在部署 MySQL 之前执行**，这样 MySQL 首次启动时才能自动执行初始化脚本：

```bash
# 从项目根目录执行
kubectl create configmap mysql-init-scripts \
  --from-file=admin-schema.sql=docker/middleware/init/mysql/admin-schema.sql \
  --from-file=agentscope-schema.sql=docker/middleware/init/mysql/agentscope-schema.sql \
  -n spring-ai-admin \
  --dry-run=client -o yaml | kubectl apply -f -
```

### 5. 部署中间件服务

按顺序部署中间件服务：

```bash
# MySQL（需要先部署，其他服务依赖）
kubectl apply -f deploy/middleware/mysql/

# Elasticsearch
kubectl apply -f deploy/middleware/elasticsearch/

# Kibana（依赖 Elasticsearch）
kubectl apply -f deploy/middleware/kibana/

# Redis
kubectl apply -f deploy/middleware/redis/

# Nacos
kubectl apply -f deploy/middleware/nacos/

# RocketMQ
kubectl apply -f deploy/middleware/rocketmq/

# LoongCollector
kubectl apply -f deploy/middleware/loongcollector/
```

### 6. 等待中间件就绪

```bash
# 检查所有 Pod 状态
kubectl get pods -n spring-ai-admin

# 等待所有中间件服务就绪（特别是 MySQL 和 Elasticsearch）
kubectl wait --for=condition=ready pod -l app=mysql -n spring-ai-admin --timeout=300s
kubectl wait --for=condition=ready pod -l app=elasticsearch -n spring-ai-admin --timeout=300s
```

### 6. 初始化 MySQL 数据库（重要！）

**必须在部署 MySQL 之前创建 ConfigMap**，这样 MySQL 容器首次启动时才能自动执行初始化脚本。

MySQL deployment 会将 `mysql-init-scripts` ConfigMap 挂载到 `/docker-entrypoint-initdb.d` 目录，MySQL 官方镜像会在首次初始化数据库时自动执行该目录下的所有 `.sql` 文件。

```bash
# 从项目根目录执行（重要：必须在部署 MySQL 之前执行）
kubectl create configmap mysql-init-scripts \
  --from-file=admin-schema.sql=docker/middleware/init/mysql/admin-schema.sql \
  --from-file=agentscope-schema.sql=docker/middleware/init/mysql/agentscope-schema.sql \
  -n spring-ai-admin \
  --dry-run=client -o yaml | kubectl apply -f -
```

**注意**：
- 如果使用 `deploy.sh` 脚本，这一步会自动处理
- 如果 MySQL 已经部署且数据已初始化，再次创建 ConfigMap 不会重新执行脚本（MySQL 只在首次初始化时执行）
- 如果需要重新初始化，需要删除 MySQL 的 PVC 和数据

### 7. 部署应用服务

```bash
# 部署后端服务
kubectl apply -f deploy/backend/

# 部署前端服务
kubectl apply -f deploy/frontend/
```

### 8. 配置 Ingress

编辑 `deploy/ingress.yaml`，将 `spring-ai-admin.local` 替换为你的实际域名，然后部署：

```bash
kubectl apply -f deploy/ingress.yaml
```

### 9. 验证部署

```bash
# 查看所有服务状态
kubectl get all -n spring-ai-admin

# 查看 Ingress
kubectl get ingress -n spring-ai-admin

# 查看 Pod 日志（如有问题）
kubectl logs -f deployment/spring-ai-admin-server -n spring-ai-admin
kubectl logs -f deployment/frontend -n spring-ai-admin
```

## 配置说明

### 环境变量

后端服务的环境变量在 `deploy/backend/backend-deployment.yaml` 中配置。所有配置都支持通过环境变量覆盖，默认值适用于本地开发环境。

主要配置项：

- **MySQL**: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- **Redis**: `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`, `SPRING_REDIS_DATABASE`
- **Elasticsearch**: `SPRING_ELASTICSEARCH_URIS`, `SPRING_ELASTICSEARCH_URL`
- **Nacos**: `NACOS_SERVER_ADDR`
- **RocketMQ**: `ROCKETMQ_ENDPOINTS`, `ROCKETMQ_NAME_SERVER`, `ROCKETMQ_DOCUMENT_INDEX_TOPIC`, `ROCKETMQ_DOCUMENT_INDEX_GROUP`
- **LoongCollector**: `MANAGEMENT_OTLP_TRACING_EXPORT_ENDPOINT`

详细配置说明请参考：`spring-ai-alibaba-admin-server-start/CONFIGURATION.md`

### 存储

所有需要持久化的服务都使用 PersistentVolumeClaim。确保你的集群已配置 StorageClass。

### 资源限制

所有服务的资源请求和限制已在配置文件中定义，可根据实际需求调整。

## 一键部署脚本

可以创建一个部署脚本简化部署过程：

```bash
#!/bin/bash
set -e

echo "Creating namespace..."
kubectl apply -f deploy/namespace.yaml

echo "Deploying middleware..."
kubectl apply -f deploy/middleware/mysql/
kubectl apply -f deploy/middleware/elasticsearch/
kubectl apply -f deploy/middleware/kibana/
kubectl apply -f deploy/middleware/redis/
kubectl apply -f deploy/middleware/nacos/
kubectl apply -f deploy/middleware/rocketmq/
kubectl apply -f deploy/middleware/loongcollector/

echo "Waiting for middleware to be ready..."
kubectl wait --for=condition=ready pod -l app=mysql -n spring-ai-admin --timeout=300s || true
kubectl wait --for=condition=ready pod -l app=elasticsearch -n spring-ai-admin --timeout=300s || true

echo "Deploying applications..."
kubectl apply -f deploy/backend/
kubectl apply -f deploy/frontend/

echo "Deploying ingress..."
kubectl apply -f deploy/ingress.yaml

echo "Deployment completed!"
kubectl get all -n spring-ai-admin
```

## 卸载

```bash
# 删除所有资源
kubectl delete -f deploy/ingress.yaml
kubectl delete -f deploy/frontend/
kubectl delete -f deploy/backend/
kubectl delete -f deploy/middleware/
kubectl delete -f deploy/namespace.yaml
```

## 注意事项

1. **存储持久化**: 删除命名空间会删除所有 PVC，数据会丢失。生产环境请谨慎操作。
2. **密码安全**: 生产环境请使用 Kubernetes Secret 管理敏感信息，不要将密码硬编码在配置文件中。
3. **资源限制**: 根据实际负载调整资源请求和限制。
4. **高可用**: 生产环境建议为关键服务配置多个副本，并配置 PodDisruptionBudget。
5. **监控**: 建议配置 Prometheus 和 Grafana 进行监控。

## 故障排查

### 查看 Pod 状态
```bash
kubectl describe pod <pod-name> -n spring-ai-admin
```

### 查看日志
```bash
kubectl logs <pod-name> -n spring-ai-admin
kubectl logs -f deployment/<deployment-name> -n spring-ai-admin
```

### 查看事件
```bash
kubectl get events -n spring-ai-admin --sort-by='.lastTimestamp'
```

### 进入容器调试
```bash
kubectl exec -it <pod-name> -n spring-ai-admin -- /bin/sh
```

