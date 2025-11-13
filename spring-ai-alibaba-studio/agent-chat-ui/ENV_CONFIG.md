# 环境变量配置说明

## 📋 环境文件列表

项目现在使用多个环境文件来自动适配不同的运行模式：

| 文件名 | 用途 | 何时使用 | Git 状态 |
|--------|------|---------|---------|
| `.env.example` | 示例配置 | 参考模板 | ✅ 提交到Git |
| `.env.local` | 通用配置 | 所有模式的备用 | ❌ 不提交 |
| `.env.development.local` | 开发配置 | `pnpm dev` | ❌ 不提交 |
| `.env.production.local` | 生产配置 | `pnpm build:static` | ❌ 不提交 |

## 🎯 Next.js 环境文件优先级

Next.js 会按以下顺序加载环境变量（后者覆盖前者）：

```
.env                    # 所有环境的默认值
.env.local              # 本地覆盖，所有环境
.env.development        # 开发环境
.env.development.local  # 开发环境本地覆盖 (优先级最高)
.env.production         # 生产环境
.env.production.local   # 生产环境本地覆盖 (优先级最高)
```

## ✅ 当前配置

### 1. `.env.development.local` (开发模式)

```bash
# 开发模式下连接到独立的Spring Boot后端
NEXT_PUBLIC_API_URL=http://localhost:8080

NEXT_PUBLIC_APP_NAME=research_agent
NEXT_PUBLIC_USER_ID=user-001
```

**效果**：
- 运行 `pnpm dev` 时自动使用
- 前端：`http://localhost:3000`
- API请求：`http://localhost:8080/apps/...`

### 2. `.env.production.local` (生产模式)

```bash
# 生产模式下使用相对路径
# 不设置 NEXT_PUBLIC_API_URL

NEXT_PUBLIC_APP_NAME=research_agent
NEXT_PUBLIC_USER_ID=user-001
```

**效果**：
- 运行 `pnpm build:static` 时自动使用
- 静态文件使用相对路径
- API请求：使用当前域名和端口

### 3. `.env.local` (备用配置)

```bash
# 通用配置，会被上面两个文件覆盖
NEXT_PUBLIC_APP_NAME=research_agent
NEXT_PUBLIC_USER_ID=user-001
```

## 🚀 使用场景

### 场景 1: 本地开发（前后端分离）

```bash
# 1. 确保后端运行在 localhost:8080
# 2. 启动前端开发服务器
pnpm dev

# 3. 访问 http://localhost:3000
# 4. API 自动请求到 http://localhost:8080
```

**使用的环境文件**: `.env.development.local`
**API URL**: `http://localhost:8080` ✅

---

### 场景 2: 构建并部署到 Spring Boot

```bash
# 1. 构建静态文件
pnpm deploy

# 2. 启动 Spring Boot (端口 9090)
# 3. 访问 http://localhost:9090/chatui/
# 4. API 自动请求到 http://localhost:9090
```

**使用的环境文件**: `.env.production.local`
**API URL**: `''` (空字符串，使用相对路径) ✅

---

### 场景 3: 标准 SSR 构建

```bash
pnpm build
pnpm start
```

**使用的环境文件**: `.env.production.local`
**API URL**: `''` (空字符串)

## 🔧 自定义配置

### 修改开发环境的后端地址

编辑 `.env.development.local`:

```bash
# 如果后端运行在不同的端口
NEXT_PUBLIC_API_URL=http://localhost:9090

# 或使用远程服务器
NEXT_PUBLIC_API_URL=https://dev-server.example.com
```

### 修改应用名称和用户ID

编辑任一环境文件：

```bash
NEXT_PUBLIC_APP_NAME=my_custom_agent
NEXT_PUBLIC_USER_ID=user-123
```

## ⚙️ 环境变量说明

### `NEXT_PUBLIC_API_URL`

- **类型**: String (可选)
- **默认值**: `''` (空字符串)
- **说明**:
  - 如果设置：所有API请求使用该URL
  - 如果为空：使用相对路径（同域名同端口）
- **开发环境推荐**: `http://localhost:8080`
- **生产环境推荐**: 不设置或留空

### `NEXT_PUBLIC_APP_NAME`

- **类型**: String (必需)
- **默认值**: `research_agent`
- **说明**: Spring AI 应用名称

### `NEXT_PUBLIC_USER_ID`

- **类型**: String (必需)
- **默认值**: `user-001`
- **说明**: 用户标识符

## 🎓 最佳实践

### ✅ 推荐做法

1. **不同环境使用不同文件**
   - 开发：`.env.development.local`
   - 生产：`.env.production.local`

2. **不要提交 `.local` 文件到 Git**
   - 这些文件包含本地配置
   - 可能包含敏感信息

3. **使用 `.env.example` 作为模板**
   - 提交到 Git，团队成员可以参考
   - 不包含实际配置值

### ❌ 避免做法

1. **不要在 `.env.local` 中设置 `NEXT_PUBLIC_API_URL`**
   - 会影响所有环境
   - 使用环境特定文件代替

2. **不要在代码中硬编码配置**
   - 始终使用环境变量
   - 便于不同环境切换

## 🔍 故障排查

### 问题：开发模式下 API 404

**检查**:
```bash
# 查看当前环境变量
cat .env.development.local

# 确认包含：
NEXT_PUBLIC_API_URL=http://localhost:8080
```

**解决**:
```bash
# 如果文件不存在，从示例复制
cp .env.example .env.development.local
# 然后编辑文件，确保 NEXT_PUBLIC_API_URL 正确
```

### 问题：构建后的静态文件仍然请求 localhost:8080

**检查**:
```bash
# 查看生产环境配置
cat .env.production.local

# 确认 NEXT_PUBLIC_API_URL 被注释或不存在
```

**解决**:
```bash
# 编辑 .env.production.local，注释掉或删除 API_URL
# NEXT_PUBLIC_API_URL=http://localhost:8080  # ❌ 删除这行

# 重新构建
pnpm deploy
```

### 问题：不确定当前使用的是哪个环境文件

**调试方法**:

在代码中临时添加日志（例如 `src/lib/spring-ai-api.ts`）：

```typescript
export function createApiClient(): SpringAIApiClient {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || '';
  console.log('🔍 API Base URL:', baseUrl);
  console.log('🔍 Environment:', process.env.NODE_ENV);
  return new SpringAIApiClient(baseUrl);
}
```

然后查看浏览器控制台输出。

## 📚 参考资料

- [Next.js Environment Variables](https://nextjs.org/docs/pages/building-your-application/configuring/environment-variables)
- [Next.js Environment Variables Order of Precedence](https://nextjs.org/docs/pages/building-your-application/configuring/environment-variables#environment-variable-load-order)

