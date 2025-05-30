# Spring AI Alibaba DeepResearch 数据脱敏功能使用指南

## 概述

本指南介绍如何在Spring AI Alibaba DeepResearch模块中使用数据脱敏功能，保护研究过程中的敏感信息。

**重要更新**：从最新版本开始，sensitivefilter模块已支持自定义正则表达式配置，您现在可以直接在sensitivefilter配置中定义所有脱敏规则，无需在data-privacy中重复配置。

## 配置架构说明

### 新的配置架构
```
sensitivefilter (基础脱敏 + 自定义正则表达式)
    ↓
EnhancedDataPrivacyService (企业级补充规则)
    ↓  
DeepResearch工作流 (工作流控制)
```

### 为什么这样设计？

1. **sensitivefilter**：提供通用的脱敏能力，支持自定义正则表达式
2. **EnhancedDataPrivacyService**：针对特定场景的补充脱敏规则
3. **data-privacy配置**：控制在DeepResearch工作流中何时应用脱敏

## 功能特性

### 🔒 支持的敏感信息类型

1. **基础敏感信息**（sensitivefilter内置）
   - 手机号码
   - 身份证号码
   - 银行卡号
   - 电子邮箱地址

2. **自定义敏感信息**（现在可在sensitivefilter中配置）
   - 社会保障号
   - 信用卡号
   - IP地址
   - API密钥
   - 任何自定义正则表达式模式

3. **企业级补充规则**（EnhancedDataPrivacyService提供）
   - URL参数脱敏
   - 公司内部编号
   - MAC地址

## 推荐配置方式

### 1. 统一在sensitivefilter中配置（推荐）

```yaml
spring:
  ai:
    alibaba:
      toolcalling:
        sensitivefilter:
          enabled: true
          replacement: "[已脱敏]"
          filter-phone-number: true
          filter-id-card: true
          filter-bank-card: true
          filter-email: true
          # 现在可以直接在这里配置自定义正则表达式
          custom-patterns:
            - name: "social-security"
              pattern: "\\d{3}-\\d{2}-\\d{4}"
              replacement: "***-**-****"
              enabled: true
            - name: "credit-card"
              pattern: "\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}"
              replacement: "****-****-****-****"
              enabled: true
            - name: "ip-address"
              pattern: "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"
              replacement: "***.***.***.***"
              enabled: true
            - name: "api-key"
              pattern: "(?i)(api[_-]?key|token|secret)[\"'\\s]*[:=][\"'\\s]*[a-zA-Z0-9+/]{16,}"
              replacement: "***API_KEY***"
              enabled: true
      deepreserch:
        # 工作流控制配置
        data-privacy:
          enabled: true
          auto-filter-output: true
          filter-intermediate-results: true
```

### 2. 配置参数说明

#### sensitivefilter配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | false | 是否启用sensitivefilter |
| `replacement` | string | "***" | 默认替换文本 |
| `filter-phone-number` | boolean | true | 是否过滤手机号 |
| `filter-id-card` | boolean | true | 是否过滤身份证号 |
| `filter-bank-card` | boolean | true | 是否过滤银行卡号 |
| `filter-email` | boolean | true | 是否过滤邮箱 |
| `custom-patterns` | List | [] | 自定义脱敏模式列表 |

#### custom-patterns子配置

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | string | 模式名称（唯一标识） |
| `pattern` | string | 正则表达式模式 |
| `replacement` | string | 替换文本（可选，默认使用全局replacement） |
| `enabled` | boolean | 是否启用此模式 |

#### data-privacy配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | true | 是否启用DeepResearch中的脱敏功能 |
| `auto-filter-output` | boolean | true | 是否自动过滤最终输出 |
| `filter-intermediate-results` | boolean | true | 是否过滤中间处理结果 |

## 使用示例

### 1. 基础研究查询（自动脱敏）

```bash
curl -X POST "http://localhost:8080/deep-research/chat/stream" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "分析张三（手机：13800138000，API密钥：sk_1234567890abcdef）的技术方案",
    "threadId": "privacy_demo",
    "autoAcceptPlan": true
  }'
```

**自动脱敏后的查询**：
```
"分析张三（手机：[已脱敏]，API密钥：***API_KEY***）的技术方案"
```

### 2. 数据脱敏API使用

同之前的API保持不变，现在底层使用增强的sensitivefilter。

## 高级配置示例

### 1. 金融行业配置

```yaml
sensitivefilter:
  custom-patterns:
    - name: "account-number"
      pattern: "\\b\\d{10,16}\\b"
      replacement: "****ACCOUNT****"
    - name: "swift-code"
      pattern: "\\b[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?\\b"
      replacement: "****SWIFT****"
    - name: "routing-number"
      pattern: "\\b\\d{9}\\b"
      replacement: "***ROUTING***"
```

### 2. 医疗健康配置

```yaml
sensitivefilter:
  custom-patterns:
    - name: "medical-record"
      pattern: "\\bMR\\d{8}\\b"
      replacement: "MR********"
    - name: "patient-id"
      pattern: "\\bP\\d{6}\\b"
      replacement: "P******"
    - name: "ssn"
      pattern: "\\b\\d{3}-\\d{2}-\\d{4}\\b"
      replacement: "***-**-****"
```

### 3. 技术开发配置

```yaml
sensitivefilter:
  custom-patterns:
    - name: "github-token"
      pattern: "\\bgh[pousr]_[A-Za-z0-9_]{36,255}\\b"
      replacement: "***GITHUB_TOKEN***"
    - name: "aws-access-key"
      pattern: "\\bAKIA[0-9A-Z]{16}\\b"
      replacement: "***AWS_KEY***"
    - name: "docker-registry"
      pattern: "\\b[a-z0-9]+(?:[._-][a-z0-9]+)*\\.[a-z]{2,}(?::[0-9]+)?/[a-z0-9._/-]+\\b"
      replacement: "***REGISTRY***"
```

## 迁移指南

### 从旧配置迁移

如果您之前在`data-privacy.custom-patterns`中配置了自定义正则表达式：

**旧配置：**
```yaml
deepreserch:
  data-privacy:
    custom-patterns:
      - name: "credit-card"
        pattern: "\\d{4}-\\d{4}-\\d{4}-\\d{4}"
        replacement: "****-****-****-****"
```

**新配置：**
```yaml
toolcalling:
  sensitivefilter:
    custom-patterns:
      - name: "credit-card"
        pattern: "\\d{4}-\\d{4}-\\d{4}-\\d{4}"
        replacement: "****-****-****-****"
        enabled: true
```

### 配置验证

启动应用时，查看日志确认配置是否生效：

```
DEBUG - 已注册自定义脱敏模式: credit-card -> \d{4}-\d{4}-\d{4}-\d{4}
DEBUG - 已注册自定义脱敏模式: api-key -> (?i)(api[_-]?key|token|secret)["'\s]*[:=]["'\s]*[a-zA-Z0-9+/]{16,}
```

## 性能优化

1. **正则表达式优化**：使用高效的正则表达式模式
2. **条件启用**：通过`enabled`参数控制不需要的模式
3. **缓存机制**：正则表达式模式会被预编译和缓存

## 故障排除

### 常见问题

1. **自定义模式不生效**
   - 检查`sensitivefilter.enabled`是否为true
   - 检查具体模式的`enabled`是否为true
   - 验证正则表达式语法是否正确

2. **模式冲突**
   - 确保模式名称唯一
   - 检查正则表达式是否互相重叠

3. **性能问题**
   - 优化复杂的正则表达式
   - 减少不必要的自定义模式

### 调试模式

```yaml
logging:
  level:
    com.alibaba.cloud.ai.toolcalling.sensitivefilter: DEBUG
    com.alibaba.cloud.ai.example.deepresearch.service: DEBUG
```

## 最佳实践

1. **统一配置**：优先在sensitivefilter中配置所有脱敏规则
2. **分层设计**：基础规则用sensitivefilter，特殊需求用EnhancedDataPrivacyService
3. **性能考虑**：合理设计正则表达式，避免过度复杂的模式
4. **安全审计**：定期检查和更新脱敏规则
5. **测试验证**：在生产环境部署前充分测试脱敏效果

通过这种新的配置架构，您可以更灵活、更高效地管理数据脱敏规则，同时保持良好的性能和可维护性。 