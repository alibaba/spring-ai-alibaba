# Spring AI Alibaba Graph Plugins

这个目录包含了 Spring AI Alibaba Graph 项目的插件集合。每个插件都是一个独立的 Maven 模块，提供特定的功能扩展。

## 插件列表

### 1. Nacos Plugin (`spring-ai-alibaba-graph-plugin-nacos`)

Nacos 配置管理插件，提供与 Nacos 配置中心的交互功能。

**功能特性：**

- 读取 Nacos 配置
- 发布/更新配置
- 删除配置
- 监听配置变化
- 自动检测配置类型
- 支持用户认证
- 支持多命名空间

**快速开始：**

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-graph-plugin-nacos</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

```java
// 创建插件实例 - 支持多种配置方式
NacosPlugin plugin1 = new NacosPlugin(); // 使用环境变量配置
NacosPlugin plugin2 = new NacosPlugin("192.168.1.100:8848"); // 指定服务器地址
NacosPlugin plugin3 = new NacosPlugin("192.168.1.100:8848", "dev", "user", "pass"); // 完整配置

// 读取配置
Map<String, Object> params = new HashMap<>();
params.put("operation", "get");
params.put("dataId", "application.properties");
params.put("group", "DEFAULT_GROUP");

Map<String, Object> result = plugin1.execute(params);
```

详细文档请参考：[Nacos Plugin README](spring-ai-alibaba-graph-plugin-nacos/README.md)

### 2. Weather Plugin (`spring-ai-alibaba-graph-plugin-weather`)

天气信息查询插件，使用 WeatherAPI.com 提供实时天气数据。

**功能特性：**

- 获取实时天气信息
- 支持多种位置格式（城市名、坐标、机场代码等）
- 支持中文城市名称
- 详细的天气数据（温度、湿度、风速、紫外线指数等）
- 完善的错误处理机制

**快速开始：**

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-graph-plugin-weather</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

```java
// 创建插件实例
WeatherPlugin plugin = new WeatherPlugin();

// 查询天气
Map<String, Object> params = new HashMap<>();
params.put("location", "Beijing");

Map<String, Object> result = plugin.execute(params);
System.out.println("温度: " + result.get("temperature") + "°C");
```

**环境配置：**

```bash
# 设置WeatherAPI.com的API密钥
export WEATHER_API_KEY=your_actual_api_key_here
```

详细文档请参考：[Weather Plugin README](spring-ai-alibaba-graph-plugin-weather/README.md)

## 开发新插件

### 1. 创建新的 Maven 模块

```bash
mkdir spring-ai-alibaba-graph-plugin-{plugin-name}
cd spring-ai-alibaba-graph-plugin-{plugin-name}
```

### 2. 创建基本的项目结构

```
spring-ai-alibaba-graph-plugin-{plugin-name}/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/alibaba/cloud/ai/graph/plugin/{plugin-name}/
│   │   │       ├── {PluginName}Plugin.java
│   └── test/
│       └── java/
│           └── com/alibaba/cloud/ai/graph/plugin/{plugin-name}/
│               └── {PluginName}PluginTest.java
└── README.md
```

### 3. 实现 GraphPlugin 接口

```java
public class MyPlugin implements GraphPlugin {

    @Override
    public String getId() {
        return "my-plugin";
    }

    @Override
    public String getName() {
        return "My Plugin";
    }

    @Override
    public String getDescription() {
        return "Description of my plugin functionality";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        // 定义输入参数schema
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("required", new String[] { "param1" });

        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> param1 = new HashMap<>();
        param1.put("type", "string");
        param1.put("description", "Parameter description");
        properties.put("param1", param1);

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) throws Exception {
        // 实现插件逻辑
        Map<String, Object> result = new HashMap<>();
        // ... 处理逻辑
        return result;
    }
}
```

### 4. 编写测试

```java
class MyPluginTest {

    private MyPlugin plugin;

    @BeforeEach
    void setUp() {
        plugin = new MyPlugin();
    }

    @Test
    void testGetId() {
        assertEquals("my-plugin", plugin.getId());
    }

    @Test
    void testExecute() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "test-value");

        Map<String, Object> result = plugin.execute(params);

        assertNotNull(result);
        // 验证结果
    }

    @Test
    void testExecuteWithInvalidParams() {
        Map<String, Object> params = new HashMap<>();
        // 缺少必需参数

        assertThrows(IllegalArgumentException.class, () -> {
            plugin.execute(params);
        });
    }
}
```

### 5. 创建文档

为每个插件创建详细的 README 文档，包括：

- 功能描述
- 安装说明
- 配置选项
- 使用示例
- API 参考
- 故障排除

## 插件开发最佳实践

### 参数验证

- 始终验证必需参数
- 提供清晰的错误消息
- 支持参数类型检查

### 错误处理

- 使用适当的异常类型
- 提供有意义的错误消息
- 记录详细的错误日志

### 文档编写

- 提供完整的 API 参考
- 包含使用示例
- 说明配置要求

### 测试覆盖

- 编写单元测试
- 包含边界情况测试
- 添加集成测试（如果适用）

## 构建和测试

### 构建所有插件

```bash
mvn clean compile
```

### 运行测试

```bash
mvn test
```

### 运行集成测试

```bash
mvn verify
```

### 安装到本地仓库

```bash
mvn install
```

## 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/new-plugin`)
3. 提交更改 (`git commit -am 'Add new plugin'`)
4. 推送到分支 (`git push origin feature/new-plugin`)
5. 创建 Pull Request

### 提交要求

- 遵循代码风格规范
- 提供完整的测试覆盖
- 更新相关文档
- 确保所有测试通过

## 许可证

Apache License 2.0 - 详见 [LICENSE](../../LICENSE) 文件。
