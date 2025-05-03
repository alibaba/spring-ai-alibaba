# SQLite Document Reader

SQLite Document Reader 是一个基于Spring AI的文档读取器实现，用于从SQLite数据库中读取数据并将其转换为文档格式。

SQLite Document Reader is a Spring AI-based document reader implementation that reads data from SQLite database and converts it into document format.

## 特性 | Features

- 使用纯JDBC实现，无需额外的连接池或ORM框架
- 支持自定义内容列和元数据列
- 完善的错误处理和资源自动关闭
- 支持自定义SQL查询
- 遵循Spring AI的Document接口规范

- Pure JDBC implementation, no additional connection pool or ORM framework required
- Pure JAVA SQLite JDBC Client without JNI. More info [SQLite4j](https://github.com/roastedroot/sqlite4j)
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
    
    <!-- SQLite JDBC Driver WITHOUT JNI-->
   <dependency>
       <groupId>io.roastedroot</groupId>
       <artifactId>sqlite4j</artifactId>
   </dependency>
</dependencies>
```

## 使用方法 | Usage

### 1. 创建SQLite资源配置 | Create SQLite Resource Configuration

```java
SQLiteResource resource = new SQLiteResource(
    "localhost",    // SQLite主机地址 | SQLite host address
    3306,          // SQLite端口号 | SQLite port number
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
SQLiteDocumentReader reader = new SQLiteDocumentReader(resource);
```

### 3. 获取文档 | Get Documents

```java
List<Document> documents = reader.get();
```

### 4. 处理文档 | Process Documents

```java
for (Document doc : documents) {
    // 获取文档内容 | Get document content
    String content = doc.getText();
    
    // 获取元数据 | Get metadata
    Map<String, Object> metadata = doc.getMetadata();
    
    // 进行后续处理 | Process further
}
```

## 配置说明 | Configuration

### SQLiteResource 参数 | Parameters

| 参数 Parameter | 说明 Description | 默认值 Default |
|------|------|--------|
| host | SQLite服务器地址 SQLite server address | 127.0.0.1 |
| port | SQLite服务器端口 SQLite server port | 3306 |
| database | 数据库名称 Database name | 无 None |
| username | 用户名 Username | root |
| password | 密码 Password | root |
| query | SQL查询语句 SQL query | 无 None |
| contentColumns | 文档内容字段列表 Document content field list | null (使用所有字段 use all fields) |
| metadataColumns | 元数据字段列表 Metadata field list | null (不使用元数据 no metadata) |

### 简化配置示例 | Simplified Configuration Example

使用默认host和port的配置示例：

```java
// 使用默认的host(127.0.0.1)和port(3306)
SQLiteResource resource = new SQLiteResource(
    "test_db",           // 数据库名称 | Database name
    "test_user",         // 用户名 | Username
    "test_password",     // 密码 | Password
    "SELECT * FROM articles",  // SQL查询语句 | SQL query
    Arrays.asList("title", "content"),  // 文档内容字段 | Document content fields
    Arrays.asList("id")                 // 元数据字段 | Metadata fields
);
```

使用所有默认连接参数的配置示例：

```java
// 使用所有默认连接参数 (host=127.0.0.1, port=3306, username=root, password=root)
SQLiteResource resource = new SQLiteResource(
    "test_db",           // 数据库名称 | Database name
    "SELECT * FROM articles",  // SQL查询语句 | SQL query
    Arrays.asList("title", "content"),  // 文档内容字段 | Document content fields
    Arrays.asList("id")                 // 元数据字段 | Metadata fields
);
```

## 注意事项 | Notes

1. 确保SQLite JDBC驱动已正确配置
   Ensure SQLite JDBC driver is properly configured

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
SQLiteResource resource = new SQLiteResource(
    "localhost",
    3306,
    "test_db",
    "test_user",
    "test_password",
    "SELECT id, title, content FROM articles WHERE status = 'published' LIMIT 100",
    Arrays.asList("title", "content"),  // 内容字段 | Content fields
    Arrays.asList("id")                 // 元数据字段 | Metadata fields
);

SQLiteDocumentReader reader = new SQLiteDocumentReader(resource);
List<Document> documents = reader.get();
```

### 自定义查询示例 | Custom Query Example

```java
SQLiteResource resource = new SQLiteResource(
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
