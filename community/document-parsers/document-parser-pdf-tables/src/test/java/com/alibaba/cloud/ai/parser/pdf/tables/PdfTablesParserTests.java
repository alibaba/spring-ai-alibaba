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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import org.springframework.ai.document.Document;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

class PdfTablesParserTests {

	private Resource resource;

	private Resource resource2;

	@BeforeEach
	void setUp() {

		resource = new DefaultResourceLoader().getResource("classpath:/pdf-tables.pdf");
		resource2 = new DefaultResourceLoader().getResource("classpath:/sample1.pdf");

		if (!resource.exists()) {
			throw new RuntimeException("Resource not found: " + resource);
		}
	}

	/**
	 * tabula-java use.
	 */
	@Test
	void PdfTableTest() throws IOException {

		InputStream in = new FileInputStream(resource.getFile());
		try (PDDocument document = PDDocument.load(in)) {
			SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
			PageIterator pi = new ObjectExtractor(document).extract();
			while (pi.hasNext()) {
				// iterate over the pages of the document
				Page page = pi.next();
				List<Table> table = sea.extract(page);
				// iterate over the tables of the page
				for (Table tables : table) {
					List<List<RectangularTextContainer>> rows = tables.getRows();
					// iterate over the rows of the table
					for (List<RectangularTextContainer> cells : rows) {
						// print all column-cells of the row plus linefeed
						for (RectangularTextContainer content : cells) {
							// Note: Cell.getText() uses \r to concat text chunk
							String text = content.getText().replace("\r", " ");
							System.out.print(text + "|");
						}
						System.out.println();
					}
				}
			}
		}

	}

	@Test
	void PdfTablesParseTest() throws IOException {

		String res = """
				|name|age|sex|
				|zhangsan|20|m|
				|lisi|21|w|
				|wangwu|22|m|
				|zhangliu|23|w|
				|songqi|24|w|
				""";

		InputStream in = new FileInputStream(resource.getFile());
		PdfTablesParser pdfTablesParser = new PdfTablesParser();
		List<Document> docs = pdfTablesParser.parse(in);

		StringBuilder sb = new StringBuilder();
		docs.subList(1, docs.size()).forEach(doc -> sb.append(doc.getText() + "\n"));

		Assert.equals(res, sb.toString());
	}

	@Test
	void PdfTablesParseTest2() throws IOException {

		String res = """
				Sample Date:|May 2001|
				Prepared by:|Accelio Present Applied Technology|
				Created and Tested Using:|•Accelio Present Central 5.4•Accelio Present Output Designer 5.4|
				Features Demonstrated:|•Primary bookmarks in a PDF file.•Secondary bookmarks in a PDF file.|
				""";

		InputStream in = new FileInputStream(resource2.getFile());
		PdfTablesParser pdfTablesParser = new PdfTablesParser();
		List<Document> docs = pdfTablesParser.parse(in);

		StringBuilder sb = new StringBuilder();
		docs.forEach(doc -> sb.append(doc.getText() + "\n"));

		Assert.equals(res, sb.toString());

	}

	@Test
	void PdfTablesParseTest3() throws IOException {

		String res = """
				|Filename|||escription|escription||
				|||||||
				ap_bookmark.IFD|The template design.||||||
				ap_bookmark.mdf|The template targeted for PDF output.||||||
				ap_bookmark.dat|A sample data file in DAT format.||||||
				ap_bookmark.bmk|A sample bookmark file.||||||
				ap_bookmark.pdf|Sample PDF output.||||||
				ap_bookmark_doc.pdf|A document describing the sample.||||||
				|To bookmark by|Use the command line parameter||
				|Invoices|-abmkap_bookmark.bmk -abmsinvoices||
				|Type|-abmkap_bookmark.bmk -abmstype||
				|Amount|-abmkap_bookmark.bmk -abmsamount||
				""";

		InputStream in = new FileInputStream(resource2.getFile());
		PdfTablesParser pdfTablesParser = new PdfTablesParser(3);
		List<Document> docs = pdfTablesParser.parse(in);

		StringBuilder sb = new StringBuilder();
		docs.forEach(doc -> sb.append(doc.getText() + "\n"));

		Assert.equals(res, sb.toString());

	}

}
