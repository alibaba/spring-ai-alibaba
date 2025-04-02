Sensitive Information Filter

它可以自动检测和过滤文本中的敏感信息，如身份证号、手机号、信用卡号等。

## 功能特性

- 支持多种敏感信息类型的过滤：
  - 身份证号
  - 手机号
  - 信用卡号
  - 邮箱地址
- 可配置的过滤规则
- 支持自定义正则表达式模式

## 配置说明

在`application.yml`或`application.properties`中添加以下配置：

```yaml
spring:
  ai:
    alibaba:
      toolcalling:
        sensitivefilter:
          enabled: true
          filter-id-card: true
          filter-phone: true
          filter-credit-card: true
          filter-email: true
          custom-patterns:
            - pattern: "自定义正则表达式"
              replacement: "替换文本"
```

### 配置项说明

- `enabled`: 是否启用敏感信息过滤
- `filter-id-card`: 是否过滤身份证号
- `filter-phone`: 是否过滤手机号
- `filter-credit-card`: 是否过滤信用卡号
- `filter-email`: 是否过滤邮箱地址
- `custom-patterns`: 自定义正则表达式模式列表

## 注意事项

1. 默认情况下，所有过滤器都是启用的
2. 可以通过配置禁用特定的过滤器
3. 自定义正则表达式模式会按照配置的顺序依次应用