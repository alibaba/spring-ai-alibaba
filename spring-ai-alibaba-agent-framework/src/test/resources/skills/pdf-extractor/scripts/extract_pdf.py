#!/usr/bin/env python3
"""
PDF Extractor Script
Extracts text, tables, and metadata from PDF files.
"""

import sys
import json

def extract_pdf(pdf_path):
    """
    Extract content from PDF file.
    This is a mock implementation for testing.
    """
    try:
        # Mock extraction result
        result = {
            "success": True,
            "filename": pdf_path,
            "text": "This is extracted text from the PDF document. It contains multiple paragraphs and sections.",
            "page_count": 5,
            "tables": [
                {
                    "page": 1,
                    "data": [
                        ["Product", "Price", "Quantity"],
                        ["Widget A", "$10.00", "100"],
                        ["Widget B", "$15.00", "50"]
                    ]
                },
                {
                    "page": 3,
                    "data": [
                        ["Month", "Revenue", "Expenses"],
                        ["January", "$50,000", "$30,000"],
                        ["February", "$55,000", "$32,000"]
                    ]
                }
            ],
            "metadata": {
                "title": "Sample PDF Document",
                "author": "Test Author",
                "created": "2024-01-01",
                "modified": "2024-01-15",
                "pages": 5
            }
        }
        
        return result
        
    except Exception as e:
        return {
            "success": False,
            "error": str(e),
            "filename": pdf_path
        }

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({
            "success": False,
            "error": "Usage: python extract_pdf.py <pdf_file_path>"
        }))
        sys.exit(1)
    
    pdf_path = sys.argv[1]
    result = extract_pdf(pdf_path)
    print(json.dumps(result, indent=2))
