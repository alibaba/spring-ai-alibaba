# GptRepo Document Reader

GptRepo Document Reader 是一个专门用于读取和处理 Git 仓库内容的工具，它可以将仓库中的文件转换为结构化的文档格式，便于后续的 AI 处理和分析。

## 功能特点

- 支持递归读取整个 Git 仓库的内容
- 支持文件扩展名过滤
- 支持通过 `.gptignore` 文件排除特定文件
- 支持文件内容的合并或分散处理
- 支持自定义文档前导文本
- 提供丰富的文件元数据信息
- 支持自定义文件编码（默认 UTF-8）

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>gpt-repo-document-reader</artifactId>
    <version>${version}</version>
</dependency>
```

### 基本使用

```java
// 基本用法 - 读取所有文件
GptRepoDocumentReader reader = new GptRepoDocumentReader("/path/to/repo");
List<Document> documents = reader.get();

// 高级用法 - 使用过滤和合并选项
List<String> extensions = Arrays.asList("java", "py");
GptRepoDocumentReader reader = new GptRepoDocumentReader(
    "/path/to/repo",    // 仓库路径
    true,               // 合并所有文件内容
    extensions,         // 文件扩展名过滤
    "UTF-8"            // 文件编码
);
```

### 自定义前导文本

```java
String customPreamble = "Repository content analysis:\n";
GptRepoDocumentReader reader = new GptRepoDocumentReader(
    "/path/to/repo",
    true,
    extensions,
    "UTF-8",
    customPreamble     // 自定义前导文本
);
```

### 使用 .gptignore 文件

在仓库根目录创建 `.gptignore` 文件，使用类似 `.gitignore` 的语法：

```plaintext
*.log
*.tmp
build/
target/
```

## 文档结构

每个处理后的文档包含以下部分：

1. 前导文本（可自定义）
2. 文件内容部分：
   - 以 `----` 分隔符开始
   - 文件路径信息
   - 文件实际内容
3. `--END--` 结束标记

## 元数据信息

每个文档对象包含以下元数据：

- `source`: 仓库根路径
- `file_path`: 文件的相对路径
- `file_name`: 文件名
- `directory`: 文件所在目录

示例：
```java
Document doc = documents.get(0);
Map<String, Object> metadata = doc.getMetadata();
String fileName = (String) metadata.get("file_name");
String filePath = (String) metadata.get("file_path");
String directory = (String) metadata.get("directory");
```

## 高级配置

### 文件合并模式

```java
// 将所有文件内容合并为一个文档
GptRepoDocumentReader reader = new GptRepoDocumentReader(
    "/path/to/repo",
    true,   // 启用合并模式
    null,
    "UTF-8"
);
```

### 扩展名过滤

```java
// 只处理特定类型的文件
List<String> extensions = Arrays.asList("java", "py", "go");
GptRepoDocumentReader reader = new GptRepoDocumentReader(
    "/path/to/repo",
    false,
    extensions,
    "UTF-8"
);
```

## 注意事项

1. 确保有足够的内存处理大型仓库
2. 建议使用 `.gptignore` 排除不需要的文件和目录
3. 对于大型仓库，建议使用文件扩展名过滤
4. 合并模式可能会产生较大的文档对象


## 许可证

Apache License 2.0 