# 注解驱动 MCP 工具注册

## 概述

本系统提供了基于注解的轻量级 MCP 工具自动注册机制，无需修改现有代码即可将工具类自动注册到 MCP 服务器中。

## 核心特性

✅ **零代码修改**: 添加新工具只需添加注解
✅ **自动发现**: Spring 容器启动时自动扫描和注册
✅ **类型安全**: 通过注解约束确保工具实现正确
✅ **配置灵活**: 支持配置文件控制注册行为
✅ **向后兼容**: 现有工具类无需大幅修改
✅ **易于扩展**: 支持多种工具类型和调用方式

## 快速开始

### 1. 创建工具类

#### 方式一：使用 @McpTool 注解（推荐）

```java
// 方式1：在注解中直接指定
@McpTool(name = "my_tool", description = "我的工具描述")
@McpToolSchema("""
    {
        "type": "object",
        "properties": {
            "input": {
                "type": "string",
                "description": "输入参数"
            }
        },
        "required": ["input"]
    }
    """)
@Component
public class MyTool {
    
    public String execute(Map<String, Object> arguments) {
        String input = (String) arguments.get("input");
        return "处理结果: " + input;
    }
}

// 方式2：注解为空，自动从工具对象获取
@McpTool(name = "", description = "")
@McpToolSchema("""
    {
        "type": "object",
        "properties": {
            "input": {
                "type": "string",
                "description": "输入参数"
            }
        },
        "required": ["input"]
    }
    """)
@Component
public class MyTool extends AbstractBaseTool<Map<String, Object>> {
    
    @Override
    public String getName() {
        return "my_tool";  // 自动获取
    }
    
    @Override
    public String getDescription() {
        return "我的工具描述";  // 自动获取
    }
    
    @Override
    public ToolExecuteResult run(Map<String, Object> input) {
        String inputValue = (String) input.get("input");
        return new ToolExecuteResult("处理结果: " + inputValue);
    }
}
```

#### 方式二：继承 AbstractBaseTool

```java
@McpTool(name = "database", description = "数据库操作工具")
@Component
public class DatabaseUseTool extends AbstractBaseTool<DatabaseRequest> {
    
    @Override
    public ToolExecuteResult run(DatabaseRequest request) {
        // 实现数据库操作逻辑
        return new ToolExecuteResult("操作成功");
    }
}
```

### 2. 注解说明

#### @McpTool

- `name`: 工具名称，MCP 客户端调用时使用的标识符。如果为空，会自动从工具对象的 `getName()` 方法获取
- `description`: 工具描述，说明工具的功能。如果为空，会自动从工具对象的 `getDescription()` 方法获取
- `enabled`: 是否启用该工具（默认 true）

#### @McpToolSchema

- `value`: JSON Schema 定义，描述工具的参数格式

### 3. 配置选项

在 `application.yml` 中配置：

```yaml
mcp:
  tools:
    # 是否启用自动发现
    auto-discover: true
    
    # 要扫描的包路径
    packages:
      - "com.alibaba.cloud.ai.example.manus.tool"
      - "com.alibaba.cloud.ai.example.manus.inhouse.tool"
    
    # 要排除的包路径
    excluded:
      - "com.alibaba.cloud.ai.example.manus.tool.internal"
    
    # 是否启用调试日志
    debug: false
```

## 工具类型支持

### 1. 通用工具类

实现 `execute(Map<String, Object>)` 方法：

```java
@McpTool(name = "calculator", description = "计算器工具")
@Component
public class CalculatorTool {
    
    public String execute(Map<String, Object> arguments) {
        String expression = (String) arguments.get("expression");
        // 计算逻辑
        return "计算结果: " + result;
    }
}
```

### 2. AbstractBaseTool 子类

继承 `AbstractBaseTool` 并实现 `run()` 方法：

```java
@McpTool(name = "database", description = "数据库工具")
@Component
public class DatabaseUseTool extends AbstractBaseTool<DatabaseRequest> {
    
    @Override
    public ToolExecuteResult run(DatabaseRequest request) {
        // 数据库操作逻辑
        return new ToolExecuteResult("操作成功");
    }
}
```

## 示例工具

### 1. 计算器工具

```java
@McpTool(name = "calculator", description = "数学计算工具")
@McpToolSchema("""
    {
        "type": "object",
        "properties": {
            "expression": {
                "type": "string",
                "description": "数学表达式"
            }
        },
        "required": ["expression"]
    }
    """)
@Component
public class CalculatorTool {
    
    public String execute(Map<String, Object> arguments) {
        String expression = (String) arguments.get("expression");
        double result = evaluateExpression(expression);
        return String.format("计算结果: %s = %.2f", expression, result);
    }
}
```

### 2. 回显工具

```java
@McpTool(name = "echo", description = "回显工具")
@McpToolSchema("""
    {
        "type": "object",
        "properties": {
            "message": {
                "type": "string",
                "description": "要回显的消息"
            }
        },
        "required": ["message"]
    }
    """)
@Component
public class EchoTool {
    
    public String execute(Map<String, Object> arguments) {
        String message = (String) arguments.get("message");
        return "回显: " + message;
    }
}
```

### 3. 数据库工具

```java
@McpTool(name = "database", description = "数据库操作工具")
@McpToolSchema("""
    {
        "oneOf": [
            {
                "type": "object",
                "properties": {
                    "action": { "type": "string", "const": "execute_sql" },
                    "query": { "type": "string", "description": "SQL语句" }
                },
                "required": ["action", "query"]
            }
        ]
    }
    """)
@Component
public class DatabaseUseTool extends AbstractBaseTool<DatabaseRequest> {
    
    @Override
    public ToolExecuteResult run(DatabaseRequest request) {
        // 数据库操作逻辑
        return new ToolExecuteResult("SQL执行成功");
    }
}
```

## 最佳实践

### 1. 工具命名

- 使用小写字母和下划线
- 名称应该简洁明了
- 避免使用特殊字符

### 2. Schema 设计

- 使用清晰的属性名称
- 提供详细的描述信息
- 合理设置必填字段

### 3. 错误处理

```java
public String execute(Map<String, Object> arguments) {
    try {
        // 工具逻辑
        return "成功结果";
    } catch (Exception e) {
        log.error("工具执行失败", e);
        return "错误: " + e.getMessage();
    }
}
```

### 4. 日志记录

```java
private static final Logger log = LoggerFactory.getLogger(MyTool.class);

public String execute(Map<String, Object> arguments) {
    log.info("执行工具，参数: {}", arguments);
    // 工具逻辑
    log.info("工具执行完成");
    return "结果";
}
```

## 故障排除

### 1. 工具未注册

检查：
- 类是否有 `@Component` 注解
- 类是否有 `@McpTool` 注解
- 包路径是否在扫描范围内
- 是否有编译错误

### 2. 工具调用失败

检查：
- JSON Schema 是否正确
- 参数格式是否匹配
- 工具方法是否存在
- 异常处理是否正确

### 3. 配置不生效

检查：
- 配置文件路径是否正确
- 配置属性名称是否正确
- 是否启用了配置类

## 扩展开发

### 1. 自定义工具类型

如果需要支持新的工具类型，可以扩展 `McpToolRegistry`：

```java
private CallToolResult invokeCustomTool(Object toolBean, CallToolRequest request) {
    // 自定义调用逻辑
}
```

### 2. 自定义注解

可以创建自定义注解来扩展功能：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomMcpTool {
    String category() default "default";
    int priority() default 0;
}
```

## 总结

注解驱动的 MCP 工具注册系统提供了：

1. **简单易用**: 只需添加注解即可注册工具
2. **自动发现**: 无需手动管理工具列表
3. **类型安全**: 通过注解和接口确保正确性
4. **配置灵活**: 支持多种配置选项
5. **向后兼容**: 不影响现有代码

通过这种方式，您可以轻松地将任何 Spring Bean 注册为 MCP 工具，大大简化了工具的开发和管理工作。 