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
package com.alibaba.cloud.ai.parser.bshtml;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.jsoup.parser.Parser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for BsHtmlDocumentParser. This class tests the functionality of parsing HTML
 * and XML files using the JSoup library and converting them to Document objects.
 *
 * The tests verify that: 1. The parser can correctly extract text content from HTML and
 * XML files 2. The parser preserves metadata from the original documents 3. The parser
 * can handle different character encodings 4. The parser can work with different JSoup
 * parser configurations
 *
 * @author HeYQ
 * @author brianxiadong
 * @since 2025-01-02 18:26
 */
class BsHtmlDocumentParserTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Tests the parsing of XML files using the BsHtmlDocumentParser. This test verifies
	 * that: - The parser can correctly extract text from XML files - The metadata is
	 * properly preserved - The original JSoup document is accessible for further
	 * processing
	 * @param fileName The name of the XML file to parse from test resources
	 */
	@ParameterizedTest
	@ValueSource(strings = { "factbook.xml" })
	void should_parse_xml_file(String fileName) {
		// Create a parser configured for XML parsing with UTF-8 encoding
		DocumentParser parser = new BsHtmlDocumentParser("UTF-8", "", Parser.xmlParser().newInstance());

		// Load the XML file from the classpath resources
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		// Parse the XML file and get the first document
		List<Document> documents = parser.parse(inputStream);
		Document document = documents.get(0);

		// Log the extracted text for debugging
		logger.info("Extracted text from XML: {}", document.getText());

		// Verify that the document has text content
		assertThat(document.getText()).isNotEmpty();

		// Verify that metadata is present
		assertThat(document.getMetadata()).isNotEmpty();

		// Verify that the original JSoup document is accessible in metadata
		assertThat(document.getMetadata()).containsKey("originalDocument");

		// Access the original JSoup document for further processing
		org.jsoup.nodes.Document jsoupDoc = (org.jsoup.nodes.Document) document.getMetadata().get("originalDocument");

		// Verify that we can query the JSoup document
		String countryText = jsoupDoc.select("country").first().text();
		logger.info("First country element text: {}", countryText);

		// Verify the content of the first country element
		assertThat(countryText).contains("United States");
	}

	/**
	 * Tests the parsing of HTML files using the BsHtmlDocumentParser. This test verifies
	 * that: - The parser can correctly extract text from HTML files - The metadata is
	 * properly preserved - The original JSoup document is accessible for further
	 * processing - HTML elements can be queried using JSoup selectors
	 * @param fileName The name of the HTML file to parse from test resources
	 */
	@ParameterizedTest
	@ValueSource(strings = { "example-utf8.html" })
	void should_parse_html_file(String fileName) {
		// Create a parser with default configuration (HTML parser, UTF-8)
		DocumentParser parser = new BsHtmlDocumentParser();

		// Load the HTML file from the classpath resources
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		// Parse the HTML file and get the first document
		List<Document> documents = parser.parse(inputStream);
		Document document = documents.get(0);

		// Log the extracted text for debugging
		logger.info("Extracted text from HTML: {}", document.getText());

		// Verify that the document has text content
		assertThat(document.getText()).isNotEmpty();

		// Verify that metadata is present
		assertThat(document.getMetadata()).isNotEmpty();

		// Verify that the original JSoup document is accessible in metadata
		assertThat(document.getMetadata()).containsKey("originalDocument");

		// Access the original JSoup document for further processing
		org.jsoup.nodes.Document jsoupDoc = (org.jsoup.nodes.Document) document.getMetadata().get("originalDocument");

		// Log the body content for debugging
		logger.info("HTML body: {}", jsoupDoc.body());

		// Verify that we can query the JSoup document
		String firstHeading = jsoupDoc.select("h1").first().text();
		logger.info("First heading: {}", firstHeading);

		// Verify the content of the first heading
		assertThat(firstHeading).contains("Instead of drinking water");

		// Verify that the title is stored in metadata
		assertThat(document.getMetadata()).containsKey("title");
	}

}
