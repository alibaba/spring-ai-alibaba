# 语义模型配置功能使用指南

## 功能概述

语义模型配置页面是Spring AI Alibaba NL2SQL项目的核心功能之一，用于管理数据集字段的语义重新设定，提升智能体自动选择数据集和问答的准确性。

## 功能特性

### 1. 基础功能
- ✅ 数据列表展示：显示所有语义模型配置，包括数据集ID、原始字段名、智能体字段名称等
- ✅ 搜索功能：支持按字段名称、同义词等关键词搜索
- ✅ 数据集筛选：可按数据集ID筛选配置项
- ✅ 新增配置：创建新的语义模型配置
- ✅ 编辑配置：修改现有的语义模型配置
- ✅ 删除配置：删除不需要的配置项

### 2. 批量操作
- ✅ 批量选择：支持勾选多个配置项进行批量操作
- ✅ 批量启用/禁用：可批量修改选中项的启用状态
- ✅ 按数据集批量操作：可对指定数据集的所有配置进行批量启用/禁用

### 3. 与后台API集成
- ✅ RESTful API调用：使用标准的REST API与后台交互
- ✅ 错误处理：完善的错误处理和用户提示
- ✅ 数据验证：前端表单验证确保数据完整性

## 技术实现

### 前端技术栈
- Vue 3 Composition API
- 响应式数据管理
- 模块化API调用
- Bootstrap Icons图标

### API接口
```javascript
// 获取列表
GET /api/semantic-model
GET /api/semantic-model?keyword=搜索关键词

// 创建配置
POST /api/semantic-model

// 更新配置
PUT /api/semantic-model/{id}

// 删除配置
DELETE /api/semantic-model/{id}

// 批量启用/禁用
POST /api/semantic-model/batch-enable
```

### 数据结构
```json
{
  "id": 1,
  "datasetId": "dataset_001",
  "originalFieldName": "sales_amount",
  "agentFieldName": "销售额",
  "fieldSynonyms": "营收,收入,销售金额",
  "fieldDescription": "某个时间段内的总销售金额",
  "fieldType": "DECIMAL",
  "originalDescription": "销售订单表中的销售金额字段",
  "defaultRecall": true,
  "enabled": true,
  "createTime": "2024-01-15T10:30:00",
  "updateTime": "2024-01-15T10:30:00"
}
```

## 使用方法

### 1. 启动后台服务
```bash
cd spring-ai-alibaba-nl2sql-management
mvn spring-boot:run
```

### 2. 启动前端服务
```bash
cd spring-ai-alibaba-nl2sql-web-ui
npm install
npm run dev
```

### 3. 访问页面
打开浏览器访问：http://localhost:3000

### 4. 功能操作

#### 新增配置
1. 点击"新增配置"按钮
2. 填写表单信息：
   - 数据集ID（必填）
   - 原始字段名（必填）
   - 智能体字段名称
   - 字段名称同义词
   - 字段描述
   - 字段类型
   - 原始字段描述
   - 默认召回设置
   - 启用状态
3. 点击"保存"按钮

#### 编辑配置
1. 在列表中找到要编辑的配置
2. 点击"编辑"按钮
3. 修改表单信息
4. 点击"保存"按钮

#### 批量操作
1. 勾选要操作的配置项
2. 点击"批量启用"或"批量禁用"按钮
3. 确认操作

#### 按数据集批量操作
1. 选择数据集筛选器
2. 点击"数据集启用"或"数据集禁用"按钮
3. 确认操作

## 注意事项

1. **必填字段**：数据集ID和原始字段名为必填项
2. **字段类型**：支持VARCHAR、INTEGER、DECIMAL、DATE、DATETIME、TEXT等类型
3. **同义词格式**：多个同义词用逗号分隔
4. **默认召回**：勾选后，该字段每次提问时都会作为提示词传输给大模型
5. **启用状态**：只有启用的配置才会生效

## 故障排除

### 常见问题

1. **加载数据失败**
   - 检查后台服务是否启动
   - 检查网络连接
   - 查看浏览器控制台错误信息

2. **保存失败**
   - 检查必填字段是否填写
   - 检查数据格式是否正确
   - 查看后台日志

3. **批量操作失败**
   - 确保选中了要操作的项
   - 检查后台服务状态

### 调试模式

如需使用模拟数据进行测试，可以修改 `SemanticModel.vue` 文件中的配置：

```javascript
const useMockData = ref(true) // 设置为 true 使用模拟数据
```

## 更新日志

### v1.0.0 (2025-07-29)
- ✅ 实现基础CRUD功能
- ✅ 添加搜索和筛选功能
- ✅ 实现批量操作
- ✅ 集成后台API
- ✅ 完善错误处理
- ✅ 优化用户体验
