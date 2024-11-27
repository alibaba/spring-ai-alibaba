/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.reader;

import java.lang.reflect.Constructor;

import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.core.io.Resource;

public enum DocumentParser {

	TEXT_PARSER("text", "org.springframework.ai.reader.TextReader"),
	JSON_PARSER("json", "org.springframework.ai.reader.JsonReader"),
	PAGE_PDF_PARSER("pagePdf", "org.springframework.ai.reader.pdf.PagePdfDocumentReader"),
	PARAGRAPH_PDF_PARSER("paragraphPdf", "org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader"),
	DOCX_PARSER("docx", "com.alibaba.cloud.ai.reader.poi.PoiDocumentReader"),
	DOC_PARSER("doc", "com.alibaba.cloud.ai.reader.poi.PoiDocumentReader"),
	PPTX_PARSER("pptx", "com.alibaba.cloud.ai.reader.poi.PoiDocumentReader"),
	PPT_PARSER("ppt", "com.alibaba.cloud.ai.reader.poi.PoiDocumentReader"),
	XLSX_PARSER("xlsx", "com.alibaba.cloud.ai.reader.poi.PoiDocumentReader"),
	XLS_PARSER("xls", "com.alibaba.cloud.ai.reader.poi.PoiDocumentReader");

	private final String parserType;

	private final String parserClass;

	DocumentParser(String parserType, String parserClass) {
		this.parserType = parserType;
		this.parserClass = parserClass;
	}

	public DocumentReader getParser(Resource resource) {
		try {
			Class<?> readerClazz = Class.forName(parserClass);
			return (DocumentReader) readerClazz.getConstructor(Resource.class).newInstance(resource);
		}
		catch (Exception e) {
			throw new RuntimeException(String.format(
					"Initialize document parser of type '%s' failed. Please make you have the right document reader implementation in your classpath.",
					parserClass), e);
		}
	}

	public DocumentReader getParser(Resource resource, ExtractedTextFormatter formatter) {
		try {
			Class<?> readerClazz = Class.forName(parserClass);
			Constructor<?> constructor = readerClazz.getConstructor(Resource.class, ExtractedTextFormatter.class);
			return (DocumentReader) constructor.newInstance(resource, formatter);
		}
		catch (Exception e) {
			throw new RuntimeException(String.format(
					"Initialize document parser of type '%s' failed. Please make you have the right document reader implementation in your classpath.",
					parserClass), e);
		}
	}

	public static DocumentParser fromString(String parserType) {
		for (DocumentParser parser : DocumentParser.values()) {
			if (parser.parserType.equalsIgnoreCase(parserType)) {
				return parser;
			}
		}
		throw new RuntimeException("Unsupported document parser type: " + parserType);
	}

}
