<!--
  Copyright 2024-2025 the original author or authors.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Spring AI Mbox Document Reader

A Spring AI document reader implementation for reading and parsing Mbox format email files. Mbox is a common format for storing collections of email messages.

Spring AI 的 Mbox 文档读取器实现，用于读取和解析 Mbox 格式的邮件文件。Mbox 是一种用于存储电子邮件集合的常用格式。

## Features / 功能特点

- Supports standard Mbox format files / 支持标准 Mbox 格式文件
- Handles various email content types / 处理多种邮件内容类型：
  - Plain text emails / 纯文本邮件
  - HTML formatted emails / HTML 格式邮件
  - Multipart emails (text/html) / 多部分邮件（文本/HTML）
  - Emails with attachments (attachments are ignored) / 带附件的邮件（附件会被忽略）
- Extracts email metadata / 提取邮件元数据：
  - Subject / 主题
  - From address / 发件人地址
  - To address / 收件人地址
  - Date / 日期
  - Message ID / 消息ID
- Configurable message formatting / 可配置的消息格式化
- HTML content parsing with JSoup / 使用 JSoup 解析 HTML 内容
- Robust error handling / 健壮的错误处理

## Installation / 安装

Add the following dependency to your project / 在项目中添加以下依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-mbox-document-reader</artifactId>
    <version>${version}</version>
</dependency>
```

## Usage / 使用方法

### Basic Usage / 基本用法

```java
// Create a reader instance with default settings
// 使用默认设置创建阅读器实例
MboxDocumentReader reader = new MboxDocumentReader("/path/to/your.mbox");

// Read all emails
// 读取所有邮件
List<Document> documents = reader.get();

// Process each email
// 处理每封邮件
for (Document doc : documents) {
    // Get formatted content
    // 获取格式化的内容
    String content = doc.getContent();
    
    // Access metadata
    // 访问元数据
    Map<String, Object> metadata = doc.getMetadata();
    String subject = (String) metadata.get("subject");
    String from = (String) metadata.get("from");
    String to = (String) metadata.get("to");
    Date date = (Date) metadata.get("date");
}
```

### Advanced Usage / 高级用法

```java
// Custom message limit and format
// 自定义消息限制和格式
String customFormat = "Email Details:\nSubject: %4$s\nFrom: %2$s\nTo: %3$s\nDate: %1$s\n\nContent:\n%5$s";
MboxDocumentReader reader = new MboxDocumentReader(
    "/path/to/your.mbox",  // Mbox file path / Mbox文件路径
    10,                    // Maximum number of emails to read (0 for unlimited) / 最大读取邮件数（0表示无限制）
    customFormat          // Custom format string / 自定义格式字符串
);

List<Document> documents = reader.get();
```

## Message Format / 消息格式

The default message format is / 默认消息格式为：
```
Date: %s
From: %s
To: %s
Subject: %s
Content: %s
```

Format parameters / 格式参数：
1. `%1$s` - Date / 日期
2. `%2$s` - From address / 发件人地址
3. `%3$s` - To address / 收件人地址
4. `%4$s` - Subject / 主题
5. `%5$s` - Message content / 消息内容

## Error Handling / 错误处理

The reader uses runtime exceptions for error handling / 读取器使用运行时异常进行错误处理：
- Invalid file path or format / 无效的文件路径或格式
- Empty or malformed content / 空或格式错误的内容
- HTML parsing errors / HTML解析错误
- Date parsing errors / 日期解析错误

Example / 示例：
```java
try {
    List<Document> documents = reader.get();
} catch (RuntimeException e) {
    // Handle errors / 处理错误
    System.err.println("Failed to read mbox file: " + e.getMessage());
}
```

## Limitations / 限制

- Only supports UTF-8 encoding / 仅支持 UTF-8 编码
- Attachments are ignored / 附件会被忽略
- Only text and HTML content types are processed / 仅处理文本和 HTML 内容类型
- HTML content is converted to plain text / HTML 内容会被转换为纯文本
- Requires valid Mbox format with "From " line separators / 需要带有 "From " 行分隔符的有效 Mbox 格式

## License / 许可证

Licensed under the Apache License, Version 2.0 / 基于 Apache License 2.0 许可证 