# Mem0 Memory Starter 测试说明

本文档描述了 `spring-ai-alibaba-starter-memory-mem0` 项目的测试结构和运行方法。

## 测试结构

### 单元测试

- `Mem0ChatMemoryAdvisorTest.java` - Mem0ChatMemoryAdvisor 的单元测试
- `Mem0MemoryStoreTest.java` - Mem0MemoryStore 的单元测试
- `Mem0ServiceClientTest.java` - Mem0ServiceClient 的单元测试
- `Mem0FilterExpressionConverterTest.java` - Mem0FilterExpressionConverter 的单元测试
- `Mem0ChatMemoryAutoConfigurationTest.java` - 自动配置的单元测试
- `Mem0ChatMemoryPropertiesTest.java` - 配置属性的单元测试

### 集成测试

- `Mem0ChatMemoryIntegrationTest.java` - 使用 TestContainers 的集成测试

### 测试工具

- `TestUtils.java` - 提供常用的测试辅助方法

## 运行测试

### 运行所有测试

```bash
mvn test
```

### 运行单元测试

```bash
mvn test -Dtest="*Test" -DfailIfNoTests=false
```

### 运行集成测试

```bash
mvn test -Dtest="*IntegrationTest" -DfailIfNoTests=false
```

### 运行特定测试类

```bash
mvn test -Dtest=Mem0ChatMemoryAdvisorTest
```

## 测试依赖

项目使用了以下测试依赖：

- **JUnit 5** - 测试框架
- **Mockito** - Mock 框架
- **AssertJ** - 断言库
- **TestContainers** - 容器化测试
- **Spring Boot Test** - Spring Boot 测试支持

## 测试配置

测试配置文件位于 `src/test/resources/application-test.yml`，包含：

- Mem0 服务器配置
- 客户端配置
- 日志配置

## 测试覆盖范围

### Mem0ChatMemoryAdvisor 测试

- 构造函数测试
- before() 方法测试（包含有效请求、空消息、空元数据等场景）
- after() 方法测试
- getOrder() 方法测试
- 常量测试
- 过滤器测试
- 无搜索结果测试

### Mem0MemoryStore 测试

- Builder 模式测试
- add() 方法测试
- delete() 方法测试（按ID和按过滤器）
- similaritySearch() 方法测试
- 异常处理测试
- 多文档操作测试

### Mem0ServiceClient 测试

- 构造函数测试
- 各种请求对象创建测试
- 配置测试
- 重置功能测试

### Mem0FilterExpressionConverter 测试

- 各种过滤器表达式转换测试
- 复杂嵌套表达式测试
- 不同数据类型测试
- 空值处理测试

### 自动配置测试

- 默认配置测试
- 自定义配置测试
- 条件配置测试
- Bean 创建测试

### 配置属性测试

- 默认值测试
- 属性设置测试
- 嵌套对象测试
- 各种配置组合测试

## 集成测试说明

集成测试使用 TestContainers 来模拟 Mem0 服务环境：

- 启动 Mem0 容器
- 动态配置连接属性
- 测试完整的组件集成
- 验证容器健康状态

## 注意事项

1. **Docker 环境**：集成测试需要 Docker 环境支持
2. **网络连接**：某些测试可能需要网络连接
3. **资源清理**：TestContainers 会自动清理测试容器
4. **测试隔离**：每个测试都是独立的，不会相互影响

## 故障排除

### 常见问题

1. **Docker 未运行**：确保 Docker 服务正在运行
2. **端口冲突**：如果端口被占用，TestContainers 会自动选择其他端口
3. **内存不足**：确保有足够的内存运行测试容器
4. **网络问题**：检查网络连接和防火墙设置

### 调试技巧

1. 查看测试日志：`mvn test -X`
2. 运行单个测试：`mvn test -Dtest=TestClassName#testMethodName`
3. 跳过集成测试：`mvn test -DskipITs=true`
4. 查看容器日志：在集成测试中检查容器日志

## 贡献指南

添加新测试时请遵循以下原则：

1. 使用描述性的测试方法名
2. 遵循 Given-When-Then 模式
3. 测试边界条件和异常情况
4. 使用 TestUtils 提供的辅助方法
5. 添加适当的注释和文档
