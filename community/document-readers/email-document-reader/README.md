# Email Document Reader

A Spring AI document reader implementation for parsing email files (EML format).

## Features

- Support for EML format email files
- Extracts email metadata (subject, from, to, date, etc.)
- Handles both plain text and HTML content
- Supports various character encodings (UTF-8, etc.)
- Handles Base64 and Quoted-Printable encoded content
- Processes attachments using Apache Tika (supports PDF, DOC, etc.)
- Compliant with Spring AI Document interface specification

## Dependencies

```xml
<dependencies>
    <!-- Spring AI Document Reader API -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>email-document-reader</artifactId>
    </dependency>
</dependencies>
```

## Usage

```java
// Create a reader instance with an EML file and enable attachment processing
EmlEmailDocumentReader reader = new EmlEmailDocumentReader("path/to/email.eml", true);

// Get documents (email body and attachments if any)
List<Document> documents = reader.get();

// Access email metadata
Document emailDoc = documents.get(0);
Map<String, Object> metadata = emailDoc.getMetadata();

String subject = (String) metadata.get("subject");
String from = (String) metadata.get("from");
String date = (String) metadata.get("date");

// Access email content
String content = emailDoc.getText();

// Access attachment content (if any)
if (documents.size() > 1) {
    Document attachmentDoc = documents.get(1);
    String filename = (String) attachmentDoc.getMetadata().get("filename");
    String attachmentContent = attachmentDoc.getText();
}
```

## Metadata Fields

The following metadata fields are available:

- `subject`: Email subject line
- `from`: Sender's email address
- `from_name`: Sender's display name (if available)
- `to`: Recipient's email address
- `to_name`: Recipient's display name (if available)
- `date`: Email date in RFC 822 format
- `content_type`: MIME content type of the email
- `filename`: Original filename (for attachments)
- `size`: File size in bytes (for attachments)

## License

Licensed under the Apache License, Version 2.0. 