# GitLab Document Reader

[English](#english) | [中文](#chinese)

<a name="english"></a>
## English

GitLab Document Reader is a Spring AI document reader implementation that allows you to read issues and repository files from GitLab projects and convert them into documents. It supports both public repositories and provides flexible filtering options.

### Features

#### GitLab Issue Reader
- Read issues from GitLab projects or groups
- Filter issues by:
  - State (open, closed, all)
  - Labels
  - Milestone
  - Author
  - Assignee
  - Created/Updated date ranges
  - And more...
- Support for issue metadata including:
  - State
  - URL
  - Labels
  - Creation date
  - Author
  - Assignee

#### GitLab Repository Reader
- Read files from GitLab repositories
- Support for:
  - Single file reading
  - Directory traversal
  - Recursive file listing
  - File pattern filtering (glob patterns)
- File metadata including:
  - File path
  - File name
  - Size
  - URL
  - Last commit ID
  - Content SHA256

### Usage

#### Reading Issues

Basic usage to read all open issues:
```java
GitLabIssueReader reader = new GitLabIssueReader(
    "https://gitlab.com",
    "namespace",
    "project-name"
);
List<Document> documents = reader.get();
```

Advanced filtering with configuration:
```java
GitLabIssueConfig config = GitLabIssueConfig.builder()
    .state(GitLabIssueState.CLOSED)
    .labels(Arrays.asList("bug", "critical"))
    .createdAfter(LocalDateTime.now().minusDays(30))
    .build();

GitLabIssueReader reader = new GitLabIssueReader(
    "https://gitlab.com",
    "namespace",
    "project-name",
    null,
    config
);
List<Document> documents = reader.get();
```

#### Reading Repository Files

Basic usage to read a single file:
```java
GitLabRepositoryReader reader = new GitLabRepositoryReader(
    "https://gitlab.com",
    "namespace",
    "project-name"
);
List<Document> documents = reader.setRef("main")
    .setFilePath("README.md")
    .get();
```

Reading all markdown files recursively:
```java
GitLabRepositoryReader reader = new GitLabRepositoryReader(
    "https://gitlab.com",
    "namespace",
    "project-name"
);
List<Document> documents = reader.setRef("main")
    .setPattern("**/*.md")
    .setRecursive(true)
    .get();
```

### Dependencies

Add the following dependency to your project:

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>gitlab-document-reader</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

The GitLab Document Reader internally uses GitLab4J API for GitLab integration, which is automatically managed through transitive dependencies.

### Limitations

- Only supports public repositories
- Rate limits apply based on GitLab's API restrictions
- File size limits apply based on GitLab's API restrictions

### License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

---

<a name="chinese"></a>
## 中文

GitLab Document Reader 是一个 Spring AI 文档读取器实现，可以从 GitLab 项目中读取 issues 和仓库文件并将它们转换为文档。它支持公开仓库访问，并提供灵活的过滤选项。

### 功能特性

#### GitLab Issue 读取器
- 从 GitLab 项目或群组中读取 issues
- 支持多种过滤条件：
  - 状态（开放、关闭、全部）
  - 标签
  - 里程碑
  - 作者
  - 指派人
  - 创建/更新时间范围
  - 更多...
- 支持的 issue 元数据包括：
  - 状态
  - URL
  - 标签
  - 创建时间
  - 作者
  - 指派人

#### GitLab 仓库读取器
- 读取 GitLab 仓库中的文件
- 支持功能：
  - 单文件读取
  - 目录遍历
  - 递归文件列表
  - 文件模式过滤（glob 模式）
- 文件元数据包括：
  - 文件路径
  - 文件名
  - 大小
  - URL
  - 最后提交 ID
  - 内容 SHA256

### 使用方法

#### 读取 Issues

基本用法（读取所有开放的 issues）：
```java
GitLabIssueReader reader = new GitLabIssueReader(
    "https://gitlab.com",
    "namespace",
    "project-name"
);
List<Document> documents = reader.get();
```

使用高级配置进行过滤：
```java
GitLabIssueConfig config = GitLabIssueConfig.builder()
    .state(GitLabIssueState.CLOSED)
    .labels(Arrays.asList("bug", "critical"))
    .createdAfter(LocalDateTime.now().minusDays(30))
    .build();

GitLabIssueReader reader = new GitLabIssueReader(
    "https://gitlab.com",
    "namespace",
    "project-name",
    null,
    config
);
List<Document> documents = reader.get();
```

#### 读取仓库文件

基本用法（读取单个文件）：
```java
GitLabRepositoryReader reader = new GitLabRepositoryReader(
    "https://gitlab.com",
    "namespace",
    "project-name"
);
List<Document> documents = reader.setRef("main")
    .setFilePath("README.md")
    .get();
```

递归读取所有 markdown 文件：
```java
GitLabRepositoryReader reader = new GitLabRepositoryReader(
    "https://gitlab.com",
    "namespace",
    "project-name"
);
List<Document> documents = reader.setRef("main")
    .setPattern("**/*.md")
    .setRecursive(true)
    .get();
```

### 依赖配置

在项目中添加以下依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>gitlab-document-reader</artifactId>
    <version>${spring-ai-alibaba.version}</version>
</dependency>
```

GitLab Document Reader 内部使用 GitLab4J API 进行 GitLab 集成，这些依赖会通过传递依赖自动管理。

### 使用限制

- 仅支持公开仓库
- 受 GitLab API 速率限制约束
- 受 GitLab API 文件大小限制约束

### 许可证

本项目采用 Apache License 2.0 许可证 - 详见 LICENSE 文件。 