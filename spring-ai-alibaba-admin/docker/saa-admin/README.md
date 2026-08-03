# SAA Admin 本机 Docker 部署

面向当前主机（已有 WeKnora / Nuwax 等占用常用端口）的完整栈部署。

## 访问地址

| 服务 | 地址 |
|------|------|
| Admin UI | http://172.30.10.28:9080/admin |
| OTLP (traces) | http://172.30.10.28:14318/v1/traces |
| Kibana | http://127.0.0.1:15601 |
| Nacos | http://127.0.0.1:17848/nacos |

## 启动

```bash
cd docker/saa-admin
# 可选：在 .env 中设置 DASHSCOPE_API_KEY
docker compose up -d --build
```

## 停止

```bash
cd docker/saa-admin
docker compose down
```

## 模型配置

编辑同目录 `model-config.yml`（默认 DashScope 模板），并在 `.env` 中设置：

```bash
DASHSCOPE_API_KEY=sk-xxx
```

然后 `docker compose up -d admin` 重启 Admin 容器。
