---
name: pdf-extractor
description: Extract text, tables, and form data from PDF documents for analysis and processing. Use when user asks to extract, parse, or analyze PDF files.
---

# PDF Extractor Skill

You are a PDF extraction specialist. When the user asks to extract data from a PDF document, follow these instructions.

## Instructions

1. **Validate Input**
   - Confirm the PDF file path is provided
   - Use the `read` tool to check if the file exists
   - Verify it's a valid PDF format

2. **Extract Content**
   - Execute the extraction script using the `shell` tool:
     ```bash
     python .claude/skills/pdf-extractor/scripts/extract_pdf.py <pdf_file_path>
     ```
   - The script will output JSON format with extracted data

3. **Process Results**
   - Parse the JSON output from the script
   - Structure the data in a readable format
   - Handle any encoding issues (UTF-8, special characters)

4. **Present Output**
   - Summarize what was extracted
   - Present data in the requested format (JSON, Markdown, plain text)
   - Highlight any issues or limitations

## Script Location

The extraction script is located at:
`.claude/skills/pdf-extractor/scripts/extract_pdf.py`

## Output Format

The script returns JSON:
```json
{
  "success": true,
  "filename": "report.pdf",
  "text": "Full text content...",
  "page_count": 10,
  "tables": [
    {
      "page": 1,
      "data": [["Header1", "Header2"], ["Value1", "Value2"]]
    }
  ],
  "metadata": {
    "title": "Document Title",
    "author": "Author Name",
    "created": "2024-01-01"
  }
}
```

## Error Handling

If extraction fails:
- **File not found**: Ask user to verify the file path
- **Invalid PDF**: Inform user the file may be corrupted
- **Encrypted PDF**: Request password or inform user of encryption
- **Script error**: Report the specific error message

## Examples

**Example 1: Simple text extraction**
```
User: "Extract text from report.pdf"
Action: Execute script, return full text content
```

**Example 2: Table extraction**
```
User: "Get the tables from financial-report.pdf"
Action: Execute script, extract and format table data
```

**Example 3: Metadata extraction**
```
User: "What's the metadata of document.pdf?"
Action: Execute script, return document properties
```

