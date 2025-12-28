# 评估器调试页面 - 变量输入框功能

## 功能概述

在评估器调试页面的测试数据卡片中，新增了评估器模版中的变量输入框功能。用户可以为评估器模版中定义的变量设置具体的值，这些值将在评估过程中被使用。

## 主要特性

### 1. 动态变量检测
- 自动检测评估器模版中定义的变量（使用 `{{variable_name}}` 格式）
- 根据检测到的变量动态生成输入框

### 2. 变量输入框
- 每个变量都有独立的输入框
- 显示变量名称和"模版变量"标签
- 支持字符计数和最大长度限制（500字符）
- 使用默认值初始化（如果模版中定义了默认值）

### 3. 表单集成
- 变量值集成到主表单中
- 支持表单验证和清空操作
- 变量值在评估运行时被收集和使用
- 所有参数（变量、input、output、reference_output）统一放入variables参数中

### 4. 用户体验优化
- 清晰的视觉分组和标签
- 响应式设计，适配不同屏幕尺寸
- 一致的样式和交互效果

## 技术实现

### 组件结构
```tsx
{/* 评估器模版变量输入框 */}
{debugConfig.variables && Object.keys(debugConfig.variables).length > 0 && (
  <>
    <div className="template-variables-section">
      <div className="template-variables-title">模版变量配置</div>
      <div className="template-variables-description">
        请为评估器模版中的变量设置值
      </div>
      
      {Object.entries(debugConfig.variables).map(([variableName, defaultValue]) => (
        <Form.Item
          key={variableName}
          className="variable-input-item"
          label={
            <div>
              <Text strong>{variableName}</Text>
              <Tag color="blue" className="variable-tag">模版变量</Tag>
            </div>
          }
          name={`variables.${variableName}`}
          initialValue={defaultValue || ''}
        >
          <Input
            placeholder={`请输入 ${variableName} 的值`}
            showCount
            maxLength={500}
          />
        </Form.Item>
      ))}
    </div>
    
    <Divider className="variables-divider" />
  </>
)}
```

### 数据处理
- 在 `handleRun` 函数中收集用户输入的变量值
- 从评估器详情中获取模版变量定义（`evaluator.variables`）
- 将变量值传递给评估器API
- 支持变量值的清空和重置
- 构建统一的variables参数，包含所有测试数据

#### Variables参数结构
```json
{
  "theme": "创作主题",
  "style": "写作风格", 
  "content_type": "内容类型",
  "audience": "目标受众",
  "word_count": "字数要求",
  "input": "用户输入的问题",
  "output": "待评估的回答",
  "reference_output": "参考标准答案"
}
```

其中：
- 前5个字段是模版中定义的变量（从 `evaluator.variables` 字段获取）
- `input`、`output`、`reference_output` 是测试数据
- 所有参数统一通过 `variables` 字段传递给评估器API

#### 变量获取逻辑
```typescript
// 从评估器详情中获取模版变量
const getTemplateVariables = useCallback(() => {
  if (evaluator && evaluator.variables) {
    try {
      return JSON.parse(evaluator.variables);
    } catch (error) {
      console.log('Error parsing evaluator variables:', error);
      return {};
    }
  }
  return {};
}, [evaluator]);

// 在handleRun中使用模版变量
const templateVariables = getTemplateVariables();
if (templateVariables && Object.keys(templateVariables).length > 0) {
  Object.keys(templateVariables).forEach(variableName => {
    const variableValue = values.variables?.[variableName];
    if (variableValue !== undefined && variableValue !== '') {
      allVariables[variableName] = variableValue;
    }
  });
}
```

#### API调用示例
```typescript
const params: EvaluatorsAPI.DebugEvaluatorParams = {
  modelConfig: JSON.stringify({
    modelId: "gpt-4",
    maxTokens: 1000,
    temperature: 0.7,
    topP: 1.0
  }),
  prompt: "系统提示词...",
  variables: JSON.stringify(allVariables), // 包含所有参数的JSON字符串
  input: "", // 已清空，数据在variables中
  output: "", // 已清空，数据在variables中
  referenceOutput: "" // 已清空，数据在variables中
};
```

### 样式设计
- 使用CSS类名进行样式管理
- 响应式设计，适配不同屏幕尺寸
- 与整体页面设计风格保持一致

## 使用流程

1. **进入调试页面**：从评估器详情页或列表页进入调试页面
2. **查看变量配置**：系统自动检测并显示模版中定义的变量
3. **设置变量值**：为每个变量输入具体的值
4. **填写测试数据**：输入待评估的输入、输出和参考输出
5. **运行评估**：点击"运行"按钮，系统将使用设置的变量值进行评估

## 使用示例

假设评估器模版中定义了以下变量：
- `theme`: 创作主题
- `style`: 写作风格
- `content_type`: 内容类型
- `audience`: 目标受众
- `word_count`: 字数要求

用户填写的数据：
- 模版变量：
  - theme: "科技产品评测"
  - style: "专业客观"
  - content_type: "评测文章"
  - audience: "技术爱好者"
  - word_count: "1000字"
- 测试数据：
  - input: "请评测最新的智能手机"
  - output: "这是一款性能出色的智能手机..."
  - reference_output: "该手机在性能、拍照、续航等方面表现优秀..."

最终发送给API的variables参数将是：
```json
{
  "theme": "科技产品评测",
  "style": "专业客观",
  "content_type": "评测文章",
  "audience": "技术爱好者",
  "word_count": "1000字",
  "input": "请评测最新的智能手机",
  "output": "这是一款性能出色的智能手机...",
  "reference_output": "该手机在性能、拍照、续航等方面表现优秀..."
}
```

## 注意事项

- 变量输入框只在模版中定义了变量时显示
- 变量值是可选的，但建议填写以提高评估准确性
- 变量值会在表单清空时重置为默认值
- 变量名称使用中文显示，提高用户体验

## 未来改进

- 支持变量值的验证规则
- 添加变量值的预设选项
- 支持变量值的导入/导出
- 增加变量使用说明和示例 