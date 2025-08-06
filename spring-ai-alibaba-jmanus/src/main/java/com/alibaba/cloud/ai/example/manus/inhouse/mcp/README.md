# McpPlan 组件使用说明

## 概述

McpPlan 组件用于将 Jmanus plan_template_version 中的 plan_json 数据转换成 McpPlanConfig。该组件提供了完整的参数解析、数据转换和异常处理功能。

## 核心组件

### 1. 数据结构类

#### PlanJson
输入数据结构，包含计划的基本信息和执行步骤。

```java
PlanJson planJson = new PlanJson();
planJson.setPlanId("planTemplate-1754276365157");
planJson.setTitle("Plan for retrieving and saving Alibaba's stock information");
planJson.setUserRequest("打开百度查询阿里巴巴最近一周的股票。生成markdown文件到本地");
```

#### PlanStep
步骤数据结构，包含步骤要求和终止条件。

```java
PlanStep step = new PlanStep("[BROWSER_AGENT] Search for {company} stock information", "Status");
```

#### McpPlanConfigVO
输出数据结构，包含转换后的配置信息。

#### McpPlanParameterVO
参数对象，包含参数名称、类型、描述和是否必需等信息。

### 2. 核心转换器

#### McpPlanConfigConverter
主要的转换器类，负责将 PlanJson 转换为 McpPlanConfigVO。

```java
McpPlanConfigConverter converter = new McpPlanConfigConverter();
McpPlanConfigVO result = converter.convert(planJson);
```

#### ParameterParser
参数解析器，负责从步骤中提取参数信息。

```java
McpPlanConfigConverter converter = new McpPlanConfigConverter();
List<McpPlanParameterVO> parameters = converter.parseParameters(steps);
```

### 3. 异常处理

#### McpPlanConversionException
自定义异常类，用于处理转换过程中的异常情况。

## 使用示例

### 基本使用

```java
// 1. 创建 PlanJson 数据
PlanJson planJson = new PlanJson();
planJson.setPlanId("test-plan");
planJson.setTitle("Test Plan");
planJson.setUserRequest("Test request");

List<PlanStep> steps = Arrays.asList(
    new PlanStep("请输入您的{name}", "Status"),
    new PlanStep("请输入您的{email}", "Status")
);
planJson.setSteps(steps);

// 2. 执行转换
McpPlanConfigConverter converter = new McpPlanConfigConverter();
McpPlanConfigVO result = converter.convert(planJson);

// 3. 获取结果
System.out.println("ID: " + result.getId());
System.out.println("名称: " + result.getName());
System.out.println("描述: " + result.getDescription());
System.out.println("参数数量: " + result.getParameters().size());
```

### 批量转换

```java
List<PlanJson> planJsonList = Arrays.asList(planJson1, planJson2, planJson3);
// 注意：McpPlanConfigConverter没有convertBatch方法，需要手动转换
List<McpPlanConfigVO> results = planJsonList.stream()
        .map(converter::convert)
        .collect(Collectors.toList());
```

### Spring Boot 集成

```java
@Autowired
private McpPlanConfigConverter converter;

public void processPlan(PlanJson planJson) {
    McpPlanConfigVO config = converter.convert(planJson);
    // 处理转换结果
}
```

## 参数解析规则

### 参数提取
- 从 `stepRequirement` 字段中提取 `{paramName}` 格式的参数
- 支持多个参数在同一步骤中
- 自动去重重复参数

### 参数属性
- **name**: 参数名称
- **type**: 参数类型，默认为 "String"
- **description**: 参数描述，格式为 "参数: {参数名}"
- **required**: 是否必需，默认为 true

### 示例
```java
// 输入步骤
"请输入您的{name}和{email}"

// 输出参数
[
  {
    "name": "name",
    "type": "String",
    "description": "参数: name",
    "required": true
  },
  {
    "name": "email", 
    "type": "String",
    "description": "参数: email",
    "required": true
  }
]
```

## 异常处理

### 输入验证
- PlanJson 不能为 null
- planId 不能为空
- title 不能为空
- userRequest 不能为空

### 异常类型
- `PlanConversionException`: 转换过程中的异常
- 包含详细的错误信息和原因

### 异常处理示例
```java
try {
    PlanMcpConfigVO result = converter.convert(planJson);
} catch (PlanConversionException e) {
    logger.error("转换失败: {}", e.getMessage());
    // 处理异常
}
```

## 单元测试

项目包含完整的单元测试，覆盖以下场景：

### McpPlanConfigConverterTest
- 基本转换测试
- 参数解析测试
- 无参数测试
- 异常情况测试
- 批量转换测试

### ParameterParserTest
- 有效步骤参数解析
- 无参数步骤测试
- 空步骤列表测试
- 复杂参数解析测试
- 重复参数去重测试

## 性能优化

### 正则表达式优化
- 预编译正则表达式 Pattern，避免重复编译
- 使用非贪婪匹配，提高匹配效率

### 内存优化
- 使用 Set 去重参数名称
- 避免创建不必要的中间对象

## 扩展性

### 参数类型推断
未来可以扩展参数类型推断功能：
- 根据参数名称推断类型（如 email、phone、period 等）
- 支持自定义类型映射规则

### 参数验证规则
- 支持参数格式验证（如邮箱格式、手机号格式、时间周期格式）
- 支持参数依赖关系定义

### 智能参数提取
- 从 userRequest 中提取可能的参数
- 支持多种参数格式（如 ${param}、{{param}} 等）

## 日志记录

组件使用 SLF4J 进行日志记录，包含以下级别的日志：

- **INFO**: 转换开始和完成
- **DEBUG**: 详细的转换过程
- **ERROR**: 异常情况

## 配置管理

Spring Boot 自动配置支持：

```java
@Configuration
public class McpPlanConfig {
    
    @Bean
    public McpPlanConfigConverter mcpPlanConfigConverter() {
        return new McpPlanConfigConverter();
    }
    
    @Bean
    public ParameterParser parameterParser() {
        return new ParameterParser();
    }
}
```

## 运行示例

使用 `SimpleTest` 或 `FinalTest` 类可以运行完整的示例：

```java
// 运行简单测试
SimpleTest.main(new String[0]);

// 运行完整测试
FinalTest.main(new String[0]);

public void runExamples() {
    example.demonstrateBasicConversion();
    example.demonstrateBatchConversion();
    example.demonstrateExceptionHandling();
}
``` 