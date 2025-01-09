# HuggingFace File System Document Reader

HuggingFace文件系统文档阅读器是一个专门用于读取和解析HuggingFace数据集文件的组件。

## 功能特点

- 支持读取JSON Lines格式文件
- 支持GZIP压缩文件的自动解压
- 自动跳过无效的JSON行
- 提供文档元数据支持
- 与Spring AI文档体系无缝集成

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>huggingface-fs-document-reader</artifactId>
    <version>${version}</version>
</dependency>
```

### 使用示例

```java
// 创建文档阅读器
HuggingFaceFSDocumentReader reader = new HuggingFaceFSDocumentReader("/path/to/your/file.jsonl");

// 读取文档
List<Document> documents = reader.get();

// 处理文档
for (Document doc : documents) {
    // 获取文档内容
    String content = doc.getContent();
    
    // 获取源文件路径（元数据）
    String source = doc.getMetadata().get(HuggingFaceFSDocumentReader.SOURCE);
    
    // 进行其他处理...
}
```

### 支持的文件格式

1. 普通JSONL文件：
```json
{"text": "文档内容1", "label": "标签1"}
{"text": "文档内容2", "label": "标签2"}
```

2. GZIP压缩的JSONL文件：
- 文件扩展名为`.gz`
- 包含压缩的JSONL内容

## 异常处理

- 文件不存在时会抛出适当的异常
- 无效的JSON行会被自动跳过
- 提供了友好的错误信息

## 最佳实践

1. 文件命名：
   - 普通文件使用`.jsonl`扩展名
   - 压缩文件使用`.jsonl.gz`扩展名

2. JSON格式：
   - 每行一个完整的JSON对象
   - 使用UTF-8编码
   - 避免使用特殊字符

3. 性能考虑：
   - 对于大文件，建议使用GZIP压缩
   - 注意内存使用，避免一次加载过大的文件

## 贡献指南

欢迎提交Issue和Pull Request来帮助改进这个组件。在提交代码时，请确保：

1. 添加适当的单元测试
2. 遵循代码规范
3. 更新相关文档

## 许可证

本项目采用 Apache License 2.0 许可证。详见 [LICENSE](LICENSE) 文件。 