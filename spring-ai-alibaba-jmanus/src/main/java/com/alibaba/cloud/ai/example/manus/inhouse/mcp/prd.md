目标:
    把Jmanus plan_template_version中的plan_json的数据转换成McpPlanConfig.

步骤拆解
    对象结构
        plan_json
            planId
            title
            userRequest
            steps

        planMcpConfigVO
            id:plan_json.planId
            name:plan_json.title
            description:plan_json.userRequest
            parameters:parserParameters(plan_json.steps) 返回对象是List<McpPlanParameterVO>
    parserParametersVO 大概的逻辑
        参数获取：
            从plan_json.steps获取 {}中的参数。例如  {name} {tel} 则认为参数有两个，是name,tel
        生成参数对象:McpPlanParameterVO
            name:对应参数获取中的值。如果是name就是name,是tel就是tel.
            type:默认是String.
            description
            required:默认为true
    
    对象转换工具类: McpPlanConfigConverter.
从plan_json转成mcpPlanConfigVO
        
    

## 实际PlanJson数据结构分析

### 示例数据
```json
{
  "planType": "simple",
  "currentPlanId": "planTemplate-1754276365157",
  "rootPlanId": "planTemplate-1754276365157",
  "title": "Plan for retrieving and saving Alibaba's stock information",
  "userRequest": "打开百度查询阿里巴巴最近一周的股票。生成markdown文件到本地",
  "directResponse": false,
  "steps": [
    {
      "stepRequirement": "[BROWSER_AGENT] Search for Alibaba's stock information for the last week",
      "terminateColumns": "Status"
    },
    {
      "stepRequirement": "[DEFAULT_AGENT] Save the searched information into a markdown file",
      "terminateColumns": "Status"
    }
  ],
  "planId": "planTemplate-1754276365157"
}
```

### 关键发现
1. **steps字段结构**: steps是对象数组，不是字符串数组
2. **参数提取位置**: 参数可能存在于stepRequirement字段中
3. **额外字段**: 包含planType、currentPlanId、rootPlanId、directResponse等字段

## 详细技术实现方案

### 1. 数据结构定义

#### 1.1 输入数据结构 (PlanJson)
```java
public class PlanJson {
    private String planType;         // 计划类型
    private String currentPlanId;    // 当前计划ID
    private String rootPlanId;       // 根计划ID
    private String title;            // 计划标题
    private String userRequest;      // 用户请求描述
    private boolean directResponse;  // 是否直接响应
    private List<PlanStep> steps;    // 执行步骤列表
    private String planId;           // 计划ID
}

public class PlanStep {
    private String stepRequirement;  // 步骤要求
    private String terminateColumns; // 终止列
}
```

#### 1.2 输出数据结构 (PlanMcpConfigVO)
```java
public class PlanMcpConfigVO {
    private String id;                           // 配置ID，对应planId
    private String name;                         // 配置名称，对应title
    private String description;                  // 配置描述，对应userRequest
    private List<PlanMcpParameterVO> parameters; // 参数列表
}
```

#### 1.3 参数对象 (PlanMcpParameterVO)
```java
public class PlanMcpParameterVO {
    private String name;        // 参数名称
    private String type;        // 参数类型，默认"String"
    private String description; // 参数描述
    private boolean required;   // 是否必需，默认true
}
```

### 2. 核心转换逻辑

#### 2.1 参数解析器 (ParameterParser)
```java
public class ParameterParser {
    
    /**
     * 从steps中解析参数
     * @param steps 步骤列表
     * @return 参数列表
     */
    public List<PlanMcpParameterVO> parseParameters(List<PlanStep> steps) {
        Set<String> paramNames = extractParameterNames(steps);
        return paramNames.stream()
                .map(this::createParameter)
                .collect(Collectors.toList());
    }
    
    /**
     * 提取参数名称
     * 从stepRequirement字段中提取 {paramName} 格式的参数
     */
    private Set<String> extractParameterNames(List<PlanStep> steps) {
        Set<String> paramNames = new HashSet<>();
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        
        for (PlanStep step : steps) {
            if (step.getStepRequirement() != null) {
                Matcher matcher = pattern.matcher(step.getStepRequirement());
                while (matcher.find()) {
                    paramNames.add(matcher.group(1));
                }
            }
        }
        return paramNames;
    }
    
    /**
     * 创建参数对象
     */
    private PlanMcpParameterVO createParameter(String paramName) {
        PlanMcpParameterVO parameter = new PlanMcpParameterVO();
        parameter.setName(paramName);
        parameter.setType("String");
        parameter.setDescription("参数: " + paramName);
        parameter.setRequired(true);
        return parameter;
    }
}
```

#### 2.2 转换器 (PlanMcpConfigConverter)
```java
public class PlanMcpConfigConverter {
    
    private final ParameterParser parameterParser;
    
    public PlanMcpConfigConverter() {
        this.parameterParser = new ParameterParser();
    }
    
    /**
     * 将PlanJson转换为PlanMcpConfigVO
     */
    public PlanMcpConfigVO convert(PlanJson planJson) {
        if (planJson == null) {
            throw new IllegalArgumentException("PlanJson不能为空");
        }
        
        PlanMcpConfigVO config = new PlanMcpConfigVO();
        config.setId(planJson.getPlanId());
        config.setName(planJson.getTitle());
        config.setDescription(planJson.getUserRequest());
        config.setParameters(parameterParser.parseParameters(planJson.getSteps()));
        
        return config;
    }
    
    /**
     * 批量转换
     */
    public List<PlanMcpConfigVO> convertBatch(List<PlanJson> planJsonList) {
        return planJsonList.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }
}
```

### 3. 异常处理

#### 3.1 自定义异常类
```java
public class PlanConversionException extends RuntimeException {
    public PlanConversionException(String message) {
        super(message);
    }
    
    public PlanConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### 3.2 异常处理策略
- 输入验证：检查planJson是否为null，必要字段是否为空
- 参数解析异常：当正则表达式匹配失败时的处理
- 数据转换异常：字段映射失败时的处理
- 步骤验证：检查steps是否为null或空

### 4. 单元测试用例

#### 4.1 基本转换测试
```java
@Test
public void testBasicConversion() {
    PlanJson planJson = new PlanJson();
    planJson.setPlanId("planTemplate-1754276365157");
    planJson.setTitle("Plan for retrieving and saving Alibaba's stock information");
    planJson.setUserRequest("打开百度查询阿里巴巴最近一周的股票。生成markdown文件到本地");
    
    List<PlanStep> steps = Arrays.asList(
        new PlanStep("[BROWSER_AGENT] Search for {company} stock information for the last {period}", "Status"),
        new PlanStep("[DEFAULT_AGENT] Save the searched information into a {fileType} file", "Status")
    );
    planJson.setSteps(steps);
    
    PlanMcpConfigConverter converter = new PlanMcpConfigConverter();
    PlanMcpConfigVO result = converter.convert(planJson);
    
    assertEquals("planTemplate-1754276365157", result.getId());
    assertEquals("Plan for retrieving and saving Alibaba's stock information", result.getName());
    assertEquals("打开百度查询阿里巴巴最近一周的股票。生成markdown文件到本地", result.getDescription());
    assertEquals(3, result.getParameters().size()); // company, period, fileType
}
```

#### 4.2 参数解析测试
```java
@Test
public void testParameterParsing() {
    List<PlanStep> steps = Arrays.asList(
        new PlanStep("请输入您的{name}", "Status"),
        new PlanStep("请输入您的{email}", "Status"),
        new PlanStep("确认您的{name}和{email}", "Status")
    );
    
    ParameterParser parser = new ParameterParser();
    List<PlanMcpParameterVO> parameters = parser.parseParameters(steps);
    
    assertEquals(2, parameters.size()); // name和email，去重
    assertTrue(parameters.stream().anyMatch(p -> "name".equals(p.getName())));
    assertTrue(parameters.stream().anyMatch(p -> "email".equals(p.getName())));
}
```

#### 4.3 无参数测试
```java
@Test
public void testNoParameters() {
    List<PlanStep> steps = Arrays.asList(
        new PlanStep("[BROWSER_AGENT] Search for Alibaba's stock information", "Status"),
        new PlanStep("[DEFAULT_AGENT] Save the information", "Status")
    );
    
    ParameterParser parser = new ParameterParser();
    List<PlanMcpParameterVO> parameters = parser.parseParameters(steps);
    
    assertEquals(0, parameters.size());
}
```

### 5. 性能优化考虑

#### 5.1 正则表达式优化
- 预编译正则表达式Pattern，避免重复编译
- 使用非贪婪匹配，提高匹配效率

#### 5.2 内存优化
- 使用Set去重参数名称
- 避免创建不必要的中间对象

### 6. 扩展性设计

#### 6.1 参数类型推断
未来可以扩展参数类型推断功能：
- 根据参数名称推断类型（如email、phone、period等）
- 支持自定义类型映射规则

#### 6.2 参数验证规则
- 支持参数格式验证（如邮箱格式、手机号格式、时间周期格式）
- 支持参数依赖关系定义

#### 6.3 智能参数提取
- 从userRequest中提取可能的参数
- 支持多种参数格式（如${param}、{{param}}等）

### 7. 配置管理

#### 7.1 默认配置
```java
@Configuration
public class PlanMcpConfig {
    
    @Bean
    public PlanMcpConfigConverter planMcpConfigConverter() {
        return new PlanMcpConfigConverter();
    }
    
    @Bean
    public ParameterParser parameterParser() {
        return new ParameterParser();
    }
}
```

### 8. 日志记录

#### 8.1 关键操作日志
- 转换开始和结束日志
- 参数解析结果日志
- 异常情况详细日志

### 9. 部署和监控

#### 9.1 健康检查
- 转换器可用性检查
- 参数解析性能监控

#### 9.2 指标收集
- 转换成功率
- 平均转换时间
- 参数解析统计

### 10. 实际应用场景分析

#### 10.1 参数提取策略
基于实际数据，参数提取需要考虑：
1. **步骤级参数**: 从stepRequirement中提取
2. **全局参数**: 从userRequest中提取
3. **智能推断**: 根据上下文推断参数类型

#### 10.2 示例转换结果
```json
{
  "id": "planTemplate-1754276365157",
  "name": "Plan for retrieving and saving Alibaba's stock information",
  "description": "打开百度查询阿里巴巴最近一周的股票。生成markdown文件到本地",
  "parameters": [
    {
      "name": "company",
      "type": "String",
      "description": "参数: company",
      "required": true
    },
    {
      "name": "period",
      "type": "String", 
      "description": "参数: period",
      "required": true
    },
    {
      "name": "fileType",
      "type": "String",
      "description": "参数: fileType", 
      "required": true
    }
  ]
}
```

