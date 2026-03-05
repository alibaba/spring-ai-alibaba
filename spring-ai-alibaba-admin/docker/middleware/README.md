# 中间件服务本地开发环境

本目录提供了用于本地开发的中间件服务配置，支持 **dev** 和 **prod** 两种模式。

## 模式说明

### Dev 模式（开发模式）
- **包含服务**: 仅 MySQL
- **适用场景**: 本地前端/后端开发，只需要基础数据库
- **启动速度**: 快
- **资源占用**: 低

### Prod 模式（完整模式）
- **包含服务**: MySQL, Redis, Elasticsearch, Kibana, Nacos, RocketMQ, LoongCollector
- **适用场景**: 完整功能测试，需要所有中间件
- **启动速度**: 较慢
- **资源占用**: 高（建议至少 8GB 内存）

## 快速开始

### 使用 Makefile（推荐）

从项目根目录执行：

```bash
# 启动 dev 模式（仅 MySQL）
make env-start MODE=dev

# 启动 prod 模式（所有中间件）
make env-start MODE=prod

# 停止服务（保留数据）
make env-stop MODE=dev
make env-stop MODE=prod

# 停止并清理所有数据
make env-clean MODE=dev
make env-clean MODE=prod
```

### 直接使用脚本

在本目录下执行：

```bash
# 启动服务
./run.sh dev   # 启动 dev 模式
./run.sh prod  # 启动 prod 模式

# 停止服务
./stop.sh dev   # 停止 dev 模式
./stop.sh prod  # 停止 prod 模式

# 清理数据
docker compose -f docker-compose-dev.yaml down -v
docker compose -f docker-compose-prod.yaml down -v
```

## 服务端口

### Dev 模式
- MySQL: 3306

### Prod 模式
- MySQL: 3306
- Redis: 6379
- Elasticsearch: 9200, 9300
- Kibana: 5601
- Nacos: 8848, 9848
- RocketMQ NameServer: 9876
- RocketMQ Broker: 10909, 10911, 10912
- RocketMQ Proxy: 18080, 18081
- LoongCollector: 4318

## 配置文件

- `docker-compose-dev.yaml` - Dev 模式配置（仅 MySQL）
- `docker-compose-prod.yaml` - Prod 模式配置（所有中间件）
- `env.template` - 环境变量模板
- `mysql.env` - MySQL 配置
- `run.sh` - 启动脚本
- `stop.sh` - 停止脚本

## 数据持久化

所有数据存储在当前目录下的子目录中：

```
docker/middleware/
├── mysql/data/          # MySQL 数据
├── redis/data/          # Redis 数据
├── elasticsearch/data/  # Elasticsearch 数据
├── nacos/data/          # Nacos 配置数据
├── rocketmq/store/      # RocketMQ 消息数据
└── ...
```

## 常用命令

```bash
# 查看服务状态
docker compose -f docker-compose-dev.yaml ps
docker compose -f docker-compose-prod.yaml ps

# 查看服务日志
docker compose -f docker-compose-dev.yaml logs -f
docker compose -f docker-compose-prod.yaml logs -f [service-name]

# 重启某个服务
docker compose -f docker-compose-prod.yaml restart mysql

# 进入容器
docker exec -it mysql bash
docker exec -it elasticsearch bash
```

## 故障排查

### MySQL 连接问题

```bash
# 检查 MySQL 是否启动
docker ps | grep mysql

# 查看 MySQL 日志
docker logs mysql

# 测试连接
mysql -h 127.0.0.1 -P 3306 -uadmin -padmin
```

### Elasticsearch 内存不足

如果遇到内存不足问题，可以调整 `docker-compose-prod.yaml` 中的 ES_JAVA_OPTS：

```yaml
environment:
  - "ES_JAVA_OPTS=-Xms512m -Xmx512m"  # 降低内存使用
```

### 端口冲突

如果端口被占用，可以修改对应的 docker-compose 文件中的端口映射：

```yaml
ports:
  - "3307:3306"  # 将 MySQL 映射到 3307 端口
```

## 与完整部署的区别

| 项目 | 本地开发 | Docker Compose 部署 | Kubernetes 部署 |
|------|----------|-------------------|-----------------|
| 目的 | 本地开发 | 完整测试/演示 | 生产环境 |
| 中间件 | 可选 dev/prod | 全部 | 全部 |
| 应用服务 | 本地运行 | 容器运行 | Pod 运行 |
| 启动方式 | make env-start | make deploy-compose | make deploy-k8s |
| 数据持久化 | 本地目录 | 本地目录 | PV/PVC |

## 清理建议

开发完成后，建议清理不需要的数据：

```bash
# 仅停止服务（保留数据）
make env-stop MODE=dev

# 停止并删除所有数据
make env-clean MODE=dev

# 手动清理数据目录
rm -rf mysql/data redis/data elasticsearch/data
```

## 参考文档

- [Docker Compose 部署](../../deploy/docker-compose/README.md)
- [Kubernetes 部署](../../deploy/kubernetes/README.md)
- [项目主文档](../../README.md)
