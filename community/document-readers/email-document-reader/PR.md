# Email Document Reader PR
# 邮件文档读取器 PR

## Description | 描述

This PR adds a new document reader implementation for parsing email files (EML format) in Spring AI. The implementation provides comprehensive support for email parsing, including:

本 PR 为 Spring AI 添加了一个新的文档读取器实现，用于解析邮件文件（EML 格式）。该实现提供了全面的邮件解析支持，包括：

1. Email Content Processing | 邮件内容处理
   - Support for both HTML and plain text content | 支持 HTML 和纯文本内容
   - Proper handling of multipart email messages | 正确处理多部分邮件消息
   - HTML content parsing with text extraction | HTML 内容解析和文本提取
   - Character encoding support (UTF-8, etc.) | 字符编码支持（UTF-8 等）

2. Metadata Extraction | 元数据提取
   - Email headers (subject, from, to, date) | 邮件头信息（主题、发件人、收件人、日期）
   - Sender and recipient information with name parsing | 发件人和收件人信息，包括姓名解析
   - Content type and other email attributes | 内容类型和其他邮件属性
   - Support for Base64 and Q-encoded header values | 支持 Base64 和 Q 编码的头部值

3. Attachment Handling | 附件处理
   - Attachment content extraction using Apache Tika | 使用 Apache Tika 提取附件内容
   - Support for various document formats (PDF, DOC, etc.) | 支持多种文档格式（PDF、DOC 等）
   - Metadata preservation (filename, size, content type) | 元数据保留（文件名、大小、内容类型）
   - Temporary file handling with proper cleanup | 临时文件处理和正确清理

4. Robust Error Handling | 健壮的错误处理
   - Graceful handling of malformed emails | 优雅处理格式错误的邮件
   - Comprehensive logging | 全面的日志记录
   - Resource cleanup in error cases | 错误情况下的资源清理

## Implementation Details | 实现细节

### Core Classes | 核心类

1. `EmlEmailDocumentReader`
   - Main document reader implementation | 主要的文档读取器实现
   - Handles email file reading and document generation | 处理邮件文件读取和文档生成
   - Supports configuration for attachment processing and content preferences | 支持附件处理和内容偏好配置

2. `EmailParser`
   - Utility class for email parsing | 邮件解析工具类
   - Handles header parsing and content extraction | 处理头部解析和内容提取
   - Supports various email encodings and formats | 支持各种邮件编码和格式

3. `EmailElement`
   - Base class for email components | 邮件组件的基类
   - Specialized implementations for different email parts | 不同邮件部分的专门实现
   - Clean abstraction for email metadata | 邮件元数据的清晰抽象

### Test Coverage | 测试覆盖

Comprehensive test suite covering: | 全面的测试套件覆盖：
- Plain text emails | 纯文本邮件
- HTML emails | HTML 邮件
- Multipart emails | 多部分邮件
- Emails with attachments | 带附件的邮件
- Encoded headers (Base64, Q-encoded) | 编码的头部（Base64、Q 编码）
- Error cases and edge conditions | 错误情况和边界条件

## Dependencies | 依赖

```xml
<dependencies>
    <!-- Spring AI Core -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-core</artifactId>
    </dependency>
    
    <!-- Jakarta Mail API -->
    <dependency>
        <groupId>jakarta.mail</groupId>
        <artifactId>jakarta.mail-api</artifactId>
    </dependency>
    
    <!-- Eclipse Angus Mail Implementation -->
    <dependency>
        <groupId>org.eclipse.angus</groupId>
        <artifactId>jakarta.mail</artifactId>
    </dependency>

    <!-- Spring AI Document Reader API -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-document-reader</artifactId>
    </dependency>
    
    <!-- Java Mail API -->
    <dependency>
        <groupId>javax.mail</groupId>
        <artifactId>javax.mail-api</artifactId>
    </dependency>
    
    <!-- Java Mail Implementation -->
    <dependency>
        <groupId>com.sun.mail</groupId>
        <artifactId>javax.mail</artifactId>
    </dependency>

    <!-- Tika Document Parser -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-tika</artifactId>
    </dependency>
</dependencies>
```

## Testing | 测试

All test cases pass successfully: | 所有测试用例成功通过：
- `should_read_pull_request_email` | 读取拉取请求邮件
- `should_read_html_recruitment_email` | 读取 HTML 招聘邮件
- `should_read_code_review_comment_email` | 读取代码审查评论邮件
- `should_read_email_with_attachments` | 读取带附件的邮件
- `should_decode_q_encoded_subject` | 解码 Q 编码的主题
- `should_handle_base64_encoded_email_headers` | 处理 Base64 编码的邮件头

## Code Quality | 代码质量

- Follows Spring coding conventions | 遵循 Spring 编码规范
- Comprehensive JavaDoc documentation | 全面的 JavaDoc 文档
- Proper error handling and logging | 适当的错误处理和日志记录
- Resource management with try-with-resources | 使用 try-with-resources 进行资源管理
- Clean code structure with clear responsibilities | 清晰的代码结构和明确的职责

## License | 许可证

Licensed under the Apache License, Version 2.0. | 基于 Apache License 2.0 许可证。 