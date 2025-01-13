# GitBook Document Reader

GitBook Document Reader is a component of the Spring AI ecosystem designed to read and process documents from the GitBook platform. It converts GitBook documents into Spring AI Document objects, facilitating subsequent AI processing and analysis.

## Features

- Support for reading GitBook documents via API
- Automatic metadata extraction (title, description, path, etc.)
- Configurable metadata fields
- Custom API endpoint support
- Markdown content preservation

## Getting Started

### Maven Dependency

Add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>gitbook-document-reader</artifactId>
    <version>${version}</version>
</dependency>
```

### Basic Usage

```java
// Create a GitBook document reader instance with required parameters
GitbookDocumentReader reader = new GitbookDocumentReader(
    "your-api-token",
    "your-space-id"
);

// Get list of documents
List<Document> documents = reader.get();
```

### Advanced Configuration

```java
// Create reader with custom configuration
List<String> metadataFields = Arrays.asList("title", "description", "parent", "type");
GitbookDocumentReader reader = new GitbookDocumentReader(
    "your-api-token",
    "your-space-id",
    "custom-api-url",     // Optional custom API endpoint
    metadataFields        // Optional metadata fields to include
);
```

## Configuration Parameters

| Parameter | Description | Required | Default Value |
|-----------|-------------|----------|---------------|
| apiToken | GitBook API token for authentication | Yes | - |
| spaceId | ID of the GitBook space to read from | Yes | - |
| apiUrl | Custom API endpoint URL | No | Default GitBook API URL |
| metadataFields | List of metadata fields to include | No | null |

### Available Metadata Fields
- `title`: The page title
- `description`: The page description
- `parent`: The parent page information
- `type`: The page type
- `path`: The page path (always included)

## Important Notes

1. API Token is required for authentication
2. Space ID must be provided to identify the GitBook space
3. Each document's ID is set to the GitBook page ID
4. Empty content pages are automatically skipped
5. The `path` metadata field is always included regardless of metadata field configuration

## Example Code

```java
import com.alibaba.cloud.ai.reader.gitbook.GitbookDocumentReader;
import org.springframework.ai.document.Document;
import java.util.Arrays;
import java.util.List;

public class GitbookReaderExample {
    public static void main(String[] args) {
        // Create reader instance with metadata fields
        List<String> metadataFields = Arrays.asList("title", "description");
        GitbookDocumentReader reader = new GitbookDocumentReader(
            "your-api-token",
            "your-space-id",
            null,           // Use default API URL
            metadataFields
        );
        
        // Read documents
        List<Document> documents = reader.get();
        
        // Process document content and metadata
        for (Document doc : documents) {
            System.out.println("Document ID: " + doc.getId());
            System.out.println("Content: " + doc.getContent());
            System.out.println("Metadata: " + doc.getMetadata());
        }
    }
}
```

## License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). 