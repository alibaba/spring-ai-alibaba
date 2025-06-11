# Spring AI Alibaba Graph Plugin Nacos

这是一个用于 Spring AI Alibaba Graph 的 Nacos 配置管理插件，提供与 Nacos 配置中心的交互功能。

## 功能特性

- 🔧 读取 Nacos 配置
- ✏️ 发布/更新配置
- 🗑️ 删除配置
- 👂 监听配置变化
- 🔍 自动检测配置类型
- 🛡️ 支持用户认证
- 🌐 支持多命名空间

## 快速开始

### 1. 启动 Nacos 服务器

确保你有一个正在运行的 Nacos 服务器。如果没有，可以通过以下方式启动：

```bash
# 下载Nacos (以2.4.3为例)
wget https://github.com/alibaba/nacos/releases/download/2.4.3/nacos-server-2.4.3.tar.gz
tar -xzf nacos-server-2.4.3.tar.gz

# 启动Nacos (单机模式)
cd nacos/bin
./startup.sh -m standalone
```

### 2. 添加依赖

在你的`pom.xml`中添加：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-graph-plugin-nacos</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

### 3. 配置

#### 方法 1：环境变量（推荐）

```bash
# Nacos服务器地址
export NACOS_SERVER_ADDR=127.0.0.1:8848

# 命名空间（可选）
export NACOS_NAMESPACE=public

# 用户认证（可选）
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
```

### 使用插件

#### 创建插件实例

```java
// 方式1：使用默认构造函数（从环境变量获取配置）
NacosPlugin plugin1 = new NacosPlugin();

// 方式2：指定服务器地址（适用于多节点场景）
NacosPlugin plugin2 = new NacosPlugin("192.168.1.100:8848");

// 方式3：完整配置（推荐用于生产环境）
NacosPlugin plugin3 = new NacosPlugin("192.168.1.100:8848", "dev", "nacos", "nacos123");

// 方式4：使用Properties对象（最灵活）
Properties props = new Properties();
props.put("serverAddr", "192.168.1.100:8848");
props.put("namespace", "test");
props.put("username", "admin");
props.put("password", "password123");
NacosPlugin plugin4 = new NacosPlugin(props);
```

#### 在多节点场景中使用

```java
// 开发环境Nacos
NacosPlugin devPlugin = new NacosPlugin("dev-nacos:8848", "dev", "dev-user", "dev-pass");

// 生产环境Nacos
NacosPlugin prodPlugin = new NacosPlugin("prod-nacos:8848", "prod", "prod-user", "prod-pass");

public void manageConfig() {
    // 使用开发环境插件读取配置
    Map<String, Object> getParams = new HashMap<>();
    getParams.put("operation", "get");
    getParams.put("dataId", "application.properties");
    getParams.put("group", "DEFAULT_GROUP");

    Map<String, Object> result = devPlugin.execute(getParams);
    System.out.println("配置内容: " + result.get("content"));

    // 发布配置到生产环境
    Map<String, Object> publishParams = new HashMap<>();
    publishParams.put("operation", "publish");
    publishParams.put("dataId", "new-config");
    publishParams.put("group", "DEFAULT_GROUP");
    publishParams.put("content", "key=value");
    publishParams.put("type", "properties");

    prodPlugin.execute(publishParams);
}
```

#### Spring Boot 自动配置

```java
@Autowired
private NacosPlugin nacosPlugin; // 使用默认配置
```

## API 参考

### 输入参数

| 参数名    | 类型   | 必需 | 描述                                          |
| --------- | ------ | ---- | --------------------------------------------- |
| operation | String | 是   | 操作类型：get, publish, remove, listen        |
| dataId    | String | 是   | 配置 ID                                       |
| group     | String | 否   | 配置分组（默认：DEFAULT_GROUP）               |
| content   | String | 否   | 配置内容（publish 操作必需）                  |
| type      | String | 否   | 配置类型（text, json, yaml, properties, xml） |

### 操作类型

#### 1. get - 读取配置

```json
{
  "operation": "get",
  "dataId": "application.properties",
  "group": "DEFAULT_GROUP"
}
```

返回：

```json
{
  "operation": "get",
  "dataId": "application.properties",
  "group": "DEFAULT_GROUP",
  "content": "server.port=8080\napp.name=demo",
  "exists": true,
  "length": 28,
  "type": "properties"
}
```

#### 2. publish - 发布配置

```json
{
  "operation": "publish",
  "dataId": "database.yaml",
  "group": "DEFAULT_GROUP",
  "content": "url: jdbc:mysql://localhost:3306/test\nusername: root",
  "type": "yaml"
}
```

返回：

```json
{
  "operation": "publish",
  "dataId": "database.yaml",
  "group": "DEFAULT_GROUP",
  "content": "url: jdbc:mysql://localhost:3306/test\nusername: root",
  "type": "yaml",
  "success": true,
  "length": 52
}
```

#### 3. remove - 删除配置

```json
{
  "operation": "remove",
  "dataId": "old-config",
  "group": "DEFAULT_GROUP"
}
```

返回：

```json
{
  "operation": "remove",
  "dataId": "old-config",
  "group": "DEFAULT_GROUP",
  "success": true
}
```

#### 4. listen - 监听配置变化

```json
{
  "operation": "listen",
  "dataId": "application.properties",
  "group": "DEFAULT_GROUP"
}
```

返回：

```json
{
  "operation": "listen",
  "dataId": "application.properties",
  "group": "DEFAULT_GROUP",
  "success": true,
  "message": "Listener added successfully. Will monitor configuration changes."
}
```

## 配置类型自动检测

插件会自动检测配置内容的类型：

- **JSON**: 以`{`开头和`}`结尾，或以`[`开头和`]`结尾
- **YAML**: 包含`---`或`key: value`格式
- **Properties**: 包含`key=value`格式
- **XML**: 以`<`开头和`>`结尾
- **Text**: 其他格式

## 环境变量配置

| 环境变量            | 默认值           | 描述             |
| ------------------- | ---------------- | ---------------- |
| `NACOS_SERVER_ADDR` | `127.0.0.1:8848` | Nacos 服务器地址 |
| `NACOS_NAMESPACE`   | `public`         | 命名空间 ID      |
| `NACOS_USERNAME`    | -                | 用户名（可选）   |
| `NACOS_PASSWORD`    | -                | 密码（可选）     |

## 配置选项

| 配置项                                         | 默认值  | 描述                |
| ---------------------------------------------- | ------- | ------------------- |
| `spring.ai.alibaba.graph.plugin.nacos.enabled` | `false` | 是否启用 Nacos 插件 |

## 错误处理

插件提供完善的错误处理：

- **连接失败**：无法连接到 Nacos 服务器
- **认证失败**：用户名或密码错误
- **配置不存在**：尝试读取不存在的配置
- **权限不足**：没有操作配置的权限

## 开发指南

### 构建项目

```bash
mvn clean compile
```

### 运行测试

基本测试（不需要 Nacos 服务器）：

```bash
mvn test
```

集成测试（需要 Nacos 服务器）：

```bash
export NACOS_SERVER_ADDR=127.0.0.1:8848
mvn verify
```

### 使用示例

```java
// 创建插件实例
NacosPlugin plugin = new NacosPlugin();

// 发布JSON配置
Map<String, Object> params = new HashMap<>();
params.put("operation", "publish");
params.put("dataId", "user-service.json");
params.put("group", "MICROSERVICE");
params.put("content", "{\"timeout\": 5000, \"retries\": 3}");
params.put("type", "json");

Map<String, Object> result = plugin.execute(params);
System.out.println("发布结果: " + result.get("success"));

// 监听配置变化
Map<String, Object> listenParams = new HashMap<>();
listenParams.put("operation", "listen");
listenParams.put("dataId", "user-service.json");
listenParams.put("group", "MICROSERVICE");

plugin.execute(listenParams);
```

## 注意事项

1. 确保 Nacos 服务器正在运行并可访问
2. 监听功能会持续运行，直到应用程序关闭
3. 建议在生产环境中启用用户认证
4. 大型配置文件可能需要较长的传输时间
5. 配置变更会立即生效，请谨慎操作

## 许可证

Apache License 2.0 - 详见 [LICENSE](../../../LICENSE) 文件。
