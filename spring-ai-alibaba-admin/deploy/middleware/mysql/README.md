# MySQL 初始化脚本说明

## 工作原理

MySQL deployment 将 `mysql-init-scripts` ConfigMap 挂载到容器的 `/docker-entrypoint-initdb.d` 目录。

MySQL 官方镜像（基于 `docker-entrypoint.sh`）会在**首次初始化数据库时**自动执行该目录下的所有 `.sql`、`.sh`、`.sql.gz` 文件。

## ConfigMap 的使用

### 1. ConfigMap 确实被使用

在 `mysql-deployment.yaml` 中：

```yaml
volumes:
  - name: mysql-init
    configMap:
      name: mysql-init-scripts
      optional: true  # 如果 ConfigMap 不存在，容器仍可启动
```

```yaml
volumeMounts:
  - name: mysql-init
    mountPath: /docker-entrypoint-initdb.d
    readOnly: true
```

### 2. 创建时机很重要

**必须在部署 MySQL Deployment 之前创建 ConfigMap**，因为：

- MySQL 容器启动时会检查 `/docker-entrypoint-initdb.d` 目录
- 如果目录为空或不存在，MySQL 会正常初始化，但不会执行任何脚本
- 如果目录中有文件，MySQL 会在首次初始化时按字母顺序执行这些文件
- **一旦数据库初始化完成，即使后续添加文件也不会再执行**

### 3. 创建方法

#### 方法一：使用 kubectl create（推荐）

从项目根目录执行：

```bash
kubectl create configmap mysql-init-scripts \
  --from-file=admin-schema.sql=docker/middleware/init/mysql/admin-schema.sql \
  --from-file=agentscope-schema.sql=docker/middleware/init/mysql/agentscope-schema.sql \
  -n spring-ai-admin \
  --dry-run=client -o yaml | kubectl apply -f -
```

#### 方法二：使用 deploy.sh 脚本

`deploy.sh` 脚本会自动检测 SQL 文件并创建 ConfigMap（在部署 MySQL 之前）：

```bash
./deploy/deploy.sh
```

#### 方法三：手动创建 YAML（不推荐）

可以编辑 `mysql-init-configmap.yaml`，将 SQL 文件内容粘贴进去，然后：

```bash
kubectl apply -f deploy/middleware/mysql/mysql-init-configmap.yaml
```

## 验证初始化是否成功

### 1. 检查 ConfigMap 是否存在

```bash
kubectl get configmap mysql-init-scripts -n spring-ai-admin
kubectl describe configmap mysql-init-scripts -n spring-ai-admin
```

### 2. 检查 MySQL Pod 日志

```bash
kubectl logs -f deployment/mysql -n spring-ai-admin
```

查找类似以下日志：
```
/docker-entrypoint.sh: running /docker-entrypoint-initdb.d/admin-schema.sql
/docker-entrypoint.sh: running /docker-entrypoint-initdb.d/agentscope-schema.sql
```

### 3. 连接数据库验证

```bash
# 进入 MySQL Pod
kubectl exec -it deployment/mysql -n spring-ai-admin -- mysql -uadmin -padmin

# 在 MySQL 中执行
SHOW DATABASES;
USE admin;
SHOW TABLES;
```

## 常见问题

### Q: 如果忘记在部署前创建 ConfigMap 怎么办？

A: 如果 MySQL 已经初始化完成，需要：

1. 删除 MySQL Deployment 和 PVC（**会丢失数据**）：
   ```bash
   kubectl delete deployment mysql -n spring-ai-admin
   kubectl delete pvc mysql-data -n spring-ai-admin
   ```

2. 创建 ConfigMap：
   ```bash
   kubectl create configmap mysql-init-scripts \
     --from-file=admin-schema.sql=docker/middleware/init/mysql/admin-schema.sql \
     --from-file=agentscope-schema.sql=docker/middleware/init/mysql/agentscope-schema.sql \
     -n spring-ai-admin
   ```

3. 重新部署 MySQL：
   ```bash
   kubectl apply -f deploy/middleware/mysql/
   ```

### Q: 如何更新初始化脚本？

A: 如果数据库已经初始化，更新 ConfigMap 不会自动执行新脚本。需要：

1. 手动执行 SQL（推荐）：
   ```bash
   kubectl exec -it deployment/mysql -n spring-ai-admin -- mysql -uadmin -padmin admin < your-update.sql
   ```

2. 或者删除 PVC 重新初始化（会丢失数据）

### Q: ConfigMap 设置为 optional: true 是什么意思？

A: `optional: true` 表示如果 ConfigMap 不存在，容器仍然可以启动。这允许：

- 先部署 MySQL，后续再添加初始化脚本（不推荐）
- 在开发环境中，可以选择性地使用初始化脚本

但在生产环境中，建议确保 ConfigMap 存在后再部署 MySQL。

## 最佳实践

1. **始终在部署 MySQL 之前创建 ConfigMap**
2. **使用 deploy.sh 脚本自动化部署**
3. **验证初始化脚本是否成功执行**
4. **将初始化脚本纳入版本控制**
5. **生产环境考虑使用 Init Container 或 Job 来执行初始化，而不是依赖 MySQL 的自动初始化机制**

