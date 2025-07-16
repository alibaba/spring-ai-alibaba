/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.parser.pdf.tables;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import org.springframework.ai.document.Document;

/**
 * The purpose of this class is to extract tabular data from PDF files, compared to Apache
 * Pdfbox. Tabula is more recognizable. tabula-java:
 * <a href="https://github.com/tabulapdf/tabula-java">tabula-java</a> return a list of
 * {@link Document}
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class PdfTablesParser implements DocumentParser {

	/**
	 * The page number of the PDF file to be parsed. Default value is 1.
	 */
	private final Integer page;

	/**
	 * The metadata of the PDF file to be parsed.
	 */
	private final Map<String, String> metadata;

	public PdfTablesParser() {

		this(1);
	}

	public PdfTablesParser(Integer pageNumber) {

		this(pageNumber, Map.of());
	}

	public PdfTablesParser(Integer pageNumber, Map<String, String> metadata) {

		this.page = pageNumber;
		this.metadata = metadata;
	}

	@Override
	public List<Document> parse(InputStream inputStream) {

		try {
			return data2Document(parseTables(extraTableData(inputStream)));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected List<Table> extraTableData(InputStream in) throws Exception {

		PDDocument document = PDDocument.load(in);

		// check pdf files
		int numberOfPages = document.getNumberOfPages();
		if (numberOfPages < 0) {

			throw new RuntimeException("No page found in the PDF file.");
		}

		if (page > numberOfPages) {

			throw new RuntimeException("The page number is greater than the number of pages in the PDF file.");
		}

		SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();

		// extract page by page numbers.
		Page extract = new ObjectExtractor(document).extract(this.page);

		return sea.extract(extract);
	}

	protected List<String> parseTables(List<Table> data) {

		if (data.isEmpty()) {
			return Collections.emptyList();
		}

		return data.stream()
			.flatMap(table -> table.getRows()
				.stream()
				.map(cells -> cells.stream()
					.map(content -> content.getText().replace("\r", "").replace("\n", " "))
					.reduce((first, second) -> first + "|" + second)
					.orElse("") + "|"))
			.collect(Collectors.toList());
	}

	private List<Document> data2Document(List<String> data) {

		List<Document> documents = new ArrayList<>();

		if (data.isEmpty()) {
			return null;
		}

		for (String datum : data) {
			Document doc = new Document(datum);
			documents.add(addMetadata(doc));
		}

		return documents;
	}

	private Document addMetadata(Document document) {

		if (metadata.isEmpty()) {
			return document;
		}

		for (Map.Entry<String, String> entry : metadata.entrySet()) {
			document.getMetadata().put(entry.getKey(), entry.getValue());
		}

		return document;
	}

}
