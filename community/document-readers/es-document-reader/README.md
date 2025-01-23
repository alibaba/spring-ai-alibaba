# Spring AI Alibaba Elasticsearch Document Reader

This module provides a document reader implementation for Elasticsearch, allowing you to retrieve documents from Elasticsearch indices for use with Spring AI.

## Features

- Read documents from Elasticsearch indices
- Support for basic authentication
- Customizable query field
- Configurable maximum results
- Support for both simple retrieval and query-based search

## Usage

### Maven Dependency

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>es-document-reader</artifactId>
    <version>${version}</version>
</dependency>
```

### Basic Configuration

```java
ElasticsearchConfig config = new ElasticsearchConfig();
config.setHost("localhost");
config.setPort(9200);
config.setIndex("your-index");
config.setQueryField("content");  // Field to search in
config.setMaxResults(10);         // Maximum number of results to return

// Optional authentication
config.setUsername("your-username");
config.setPassword("your-password");

ElasticsearchDocumentReader reader = new ElasticsearchDocumentReader(config);
```

### Reading Documents

```java
// Read all documents
List<Document> documents = reader.read();

// Read documents matching a query
List<Document> queryResults = reader.readWithQuery("your search query");
```

## Configuration Properties

| Property    | Description                           | Default Value |
|------------|---------------------------------------|---------------|
| host       | Elasticsearch host                    | -             |
| port       | Elasticsearch port                    | 9200          |
| index      | Index name to query                   | -             |
| queryField | Field to search in                    | "content"     |
| username   | Username for authentication (optional) | -             |
| password   | Password for authentication (optional) | -             |
| maxResults | Maximum number of results to return    | 10            |

## Testing

The module includes comprehensive tests using TestContainers to spin up a temporary Elasticsearch instance. To run the tests:

```bash
mvn test
```

## Requirements

- Java 17 or later
- Elasticsearch 8.x
- Docker (for running tests) 