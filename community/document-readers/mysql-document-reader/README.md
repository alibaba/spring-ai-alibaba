# MySQL Document Reader

MySQL Document Reader 是一个基于Spring AI的文档读取器实现，用于从MySQL数据库中读取数据并将其转换为文档格式。

MySQL Document Reader is a Spring AI-based document reader implementation that reads data from MySQL database and converts it into document format.

## 特性 | Features

- 使用纯JDBC实现，无需额外的连接池或ORM框架
- 支持自定义内容列和元数据列
- 完善的错误处理和资源自动关闭
- 支持自定义SQL查询
- 遵循Spring AI的Document接口规范

- Pure JDBC implementation, no additional connection pool or ORM framework required
- Support for custom content and metadata columns
- Comprehensive error handling and automatic resource cleanup
- Support for custom SQL queries
- Compliant with Spring AI Document interface specification

## 依赖要求 | Dependencies

```xml
<dependencies>
    <!-- Spring AI Core -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-core</artifactId>
    </dependency>
    
    <!-- MySQL JDBC Driver -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
</dependencies>
```

## 使用方法 | Usage

### 1. 创建MySQL资源配置 | Create MySQL Resource Configuration

```java
MySQLResource resource = new MySQLResource(
    "localhost",    // MySQL主机地址 | MySQL host address
    3306,          // MySQL端口号 | MySQL port number
    "your_db",     // 数据库名称 | Database name
    "username",     // 用户名 | Username
    "password",     // 密码 | Password
    "SELECT * FROM your_table",  // SQL查询语句 | SQL query
    Arrays.asList("title", "content"),  // 文档内容字段 | Document content fields
    Arrays.asList("id", "created_at")   // 元数据字段 | Metadata fields
);
```

### 2. 创建文档读取器 | Create Document Reader

```java
MySQLDocumentReader reader = new MySQLDocumentReader(resource);
```

### 3. 获取文档 | Get Documents

```java
List<Document> documents = reader.get();
```

### 4. 处理文档 | Process Documents

```java
for (Document doc : documents) {
    // 获取文档内容 | Get document content
    String content = doc.getContent();
    
    // 获取元数据 | Get metadata
    Map<String, Object> metadata = doc.getMetadata();
    
    // 进行后续处理 | Process further
}
```

## 配置说明 | Configuration

### MySQLResource 参数 | Parameters

| 参数 Parameter | 说明 Description | 默认值 Default |
|------|------|--------|
| host | MySQL服务器地址 MySQL server address | 无 None |
| port | MySQL服务器端口 MySQL server port | 无 None |
| database | 数据库名称 Database name | 无 None |
| username | 用户名 Username | 无 None |
| password | 密码 Password | 无 None |
| query | SQL查询语句 SQL query | 无 None |
| contentColumns | 文档内容字段列表 Document content field list | null (使用所有字段 use all fields) |
| metadataColumns | 元数据字段列表 Metadata field list | null (不使用元数据 no metadata) |

## 注意事项 | Notes

1. 确保MySQL JDBC驱动已正确配置
   Ensure MySQL JDBC driver is properly configured

2. 建议使用合适的WHERE条件和LIMIT子句限制查询结果集大小
   Recommend using appropriate WHERE conditions and LIMIT clauses to restrict result set size

3. 对于大型结果集，注意内存使用情况
   For large result sets, be mindful of memory usage

4. 敏感信息（如密码）建议使用配置文件或环境变量管理
   Sensitive information (like passwords) should be managed using configuration files or environment variables

5. 建议在生产环境中使用连接池管理数据库连接
   Recommend using connection pools for database connections in production environment

## 示例 | Examples

### 基本查询示例 | Basic Query Example

```java
MySQLResource resource = new MySQLResource(
    "localhost",
    3306,
    "test_db",
    "test_user",
    "test_password",
    "SELECT id, title, content FROM articles WHERE status = 'published' LIMIT 100",
    Arrays.asList("title", "content"),  // 内容字段 | Content fields
    Arrays.asList("id")                 // 元数据字段 | Metadata fields
);

MySQLDocumentReader reader = new MySQLDocumentReader(resource);
List<Document> documents = reader.get();
```

### 自定义查询示例 | Custom Query Example

```java
MySQLResource resource = new MySQLResource(
    "localhost",
    3306,
    "blog_db",
    "blog_user",
    "blog_password",
    """
    SELECT 
        p.id, 
        p.title, 
        p.content,
        u.username as author,
        p.created_at
    FROM posts p
    JOIN users u ON p.author_id = u.id
    WHERE p.status = 'published'
    AND p.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
    """,
    Arrays.asList("title", "content", "author"),     // 内容字段 | Content fields
    Arrays.asList("id", "created_at")               // 元数据字段 | Metadata fields
);
```

## 许可证 | License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 