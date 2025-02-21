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
package com.alibaba.cloud.ai.parser.apache.pdfbox;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.pdf.layout.PDFLayoutTextStripperByArea;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Groups the parsed PDF pages into {@link Document}s. You can group one or more pages
 * into a single output document. Use {@link PdfDocumentReaderConfig} for customization
 * options. The default configuration is: - pagesPerDocument = 1 - pageTopMargin = 0 -
 * pageBottomMargin = 0
 *
 * @author HeYQ
 */
public class PagePdfDocumentParser implements DocumentParser {

	public static final String METADATA_START_PAGE_NUMBER = "page_number";

	public static final String METADATA_END_PAGE_NUMBER = "end_page_number";

	private static final String PDF_PAGE_REGION = "pdfPageRegion";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final PdfDocumentReaderConfig config;

	public PagePdfDocumentParser() {
		this(PdfDocumentReaderConfig.defaultConfig());
	}

	public PagePdfDocumentParser(PdfDocumentReaderConfig config) {
		this.config = config;
	}

	@Override
	public List<Document> parse(InputStream inputStream) {

		List<Document> readDocuments = new ArrayList<>();
		try {
			var pdfTextStripper = new PDFLayoutTextStripperByArea();

			int pageNumber = 0;
			int pagesPerDocument = 0;
			int startPageNumber = pageNumber;

			List<String> pageTextGroupList = new ArrayList<>();
			PDFParser pdfParser = new PDFParser(new org.apache.pdfbox.io.RandomAccessReadBuffer(inputStream));
			PDDocument document = pdfParser.parse();

			int totalPages = document.getDocumentCatalog().getPages().getCount();
			// if less than 10
			int logFrequency = totalPages > 10 ? totalPages / 10 : 1;
			// pages, print
			// each iteration
			int counter = 0;

			PDPage lastPage = document.getDocumentCatalog().getPages().iterator().next();
			for (PDPage page : document.getDocumentCatalog().getPages()) {
				lastPage = page;
				if (counter % logFrequency == 0 && counter / logFrequency < 10) {
					logger.info("Processing PDF page: {}", (counter + 1));
				}
				counter++;

				pagesPerDocument++;

				if (this.config.pagesPerDocument != PdfDocumentReaderConfig.ALL_PAGES
						&& pagesPerDocument >= this.config.pagesPerDocument) {
					pagesPerDocument = 0;

					var aggregatedPageTextGroup = pageTextGroupList.stream().collect(Collectors.joining());
					if (StringUtils.hasText(aggregatedPageTextGroup)) {
						readDocuments.add(toDocument(aggregatedPageTextGroup, startPageNumber, pageNumber));
					}
					pageTextGroupList.clear();

					startPageNumber = pageNumber + 1;
				}
				int x0 = (int) page.getMediaBox().getLowerLeftX();
				int xW = (int) page.getMediaBox().getWidth();

				int y0 = (int) page.getMediaBox().getLowerLeftY() + this.config.pageTopMargin;
				int yW = (int) page.getMediaBox().getHeight()
						- (this.config.pageTopMargin + this.config.pageBottomMargin);

				pdfTextStripper.addRegion(PDF_PAGE_REGION, new Rectangle(x0, y0, xW, yW));
				pdfTextStripper.extractRegions(page);
				var pageText = pdfTextStripper.getTextForRegion(PDF_PAGE_REGION);

				if (StringUtils.hasText(pageText)) {

					pageText = this.config.pageExtractedTextFormatter.format(pageText, pageNumber);

					pageTextGroupList.add(pageText);
				}
				pageNumber++;
				pdfTextStripper.removeRegion(PDF_PAGE_REGION);
			}
			if (!CollectionUtils.isEmpty(pageTextGroupList)) {
				readDocuments.add(toDocument(pageTextGroupList.stream().collect(Collectors.joining()), startPageNumber,
						pageNumber));
			}
			logger.info("Processing {} pages", totalPages);
			return readDocuments;

		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected Document toDocument(String docText, int startPageNumber, int endPageNumber) {
		Document doc = new Document(docText);
		doc.getMetadata().put(METADATA_START_PAGE_NUMBER, startPageNumber);
		if (startPageNumber != endPageNumber) {
			doc.getMetadata().put(METADATA_END_PAGE_NUMBER, endPageNumber);
		}
		return doc;
	}

}
