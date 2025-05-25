# ChatGPT Data Document Reader

ChatGPT Data Document Reader 是一个用于加载和处理 ChatGPT 导出对话数据的文档读取器。它可以将 ChatGPT 的对话记录转换为 Spring AI 可以处理的 Document 对象。

ChatGPT Data Document Reader is a document reader for loading and processing exported ChatGPT conversation data. It converts ChatGPT conversation records into Document objects that can be processed by Spring AI.

## 功能特点 | Features

- 支持读取 ChatGPT 导出的 JSON 格式对话数据
- 自动解析对话内容、时间戳和角色信息
- 支持限制加载的对话数量
- 提供格式化的对话输出，包含时间戳和发送者信息
- 与 Spring AI 框架无缝集成

- Support reading ChatGPT exported JSON format conversation data
- Automatically parse conversation content, timestamps, and role information
- Support limiting the number of conversations to load
- Provide formatted conversation output with timestamps and sender information
- Seamless integration with Spring AI framework

## 使用方法 | Usage

### Maven 依赖 | Maven Dependency

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>chatgpt-data-document-reader</artifactId>
    <version>${version}</version>
</dependency>
```

### 代码示例 | Code Example

```java
// 初始化 reader，加载所有对话
// Initialize reader, load all conversations
ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader("/path/to/chatgpt/data.json");

// 或者指定只加载前 N 条对话
// Or specify to load only the first N conversations
ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader("/path/to/chatgpt/data.json", 10);

// 获取处理后的文档列表
// Get the processed document list
List<Document> documents = reader.get();
```

### 输出格式 | Output Format

每个对话记录将被格式化为以下格式：
Each conversation record will be formatted as follows:

```
对话标题 - 发送者角色 on 2024-01-20 14:30:00: 消息内容
Conversation Title - Sender Role on 2024-01-20 14:30:00: Message Content
```

## 注意事项 | Notes

1. 输入文件必须是有效的 JSON 格式 | Input file must be valid JSON format
2. JSON 文件应包含完整的 ChatGPT 对话数据结构 | JSON file should contain complete ChatGPT conversation data structure
3. 建议对大型对话文件使用 numLogs 参数限制加载数量 | Recommended to use numLogs parameter to limit loading quantity for large conversation files

## License | 许可证

本项目采用 Apache License 2.0 协议。详情请参见 LICENSE 文件。
This project is licensed under the Apache License 2.0. See the LICENSE file for details. 