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
import org.springframework.ai.document.Document;
import java.io.InputStream;
import static org.assertj.core.api.Assertions.assertThat;

class BsHtmlDocumentParserTest {

	@ParameterizedTest
	@ValueSource(strings = { "factbook.xml" })
	void should_parse_xml_file(String fileName) {

		DocumentParser parser = new BsHtmlDocumentParser("UTF-8", "", Parser.xmlParser().newInstance());
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
		Document document = parser.parse(inputStream).get(0);
		System.out.println(document.getText());
		assertThat(document.getMetadata()).isNotEmpty();
		org.jsoup.nodes.Document doc = (org.jsoup.nodes.Document) document.getMetadata().get("originalDocument");
		System.out.println(doc.select("country").first().text());
	}

	@ParameterizedTest
	@ValueSource(strings = { "example-utf8.html" })
	void should_parse_html_file(String fileName) {
		DocumentParser parser = new BsHtmlDocumentParser();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
		Document document = parser.parse(inputStream).get(0);
		System.out.println(document.getText());
		assertThat(document.getMetadata()).isNotEmpty();
		org.jsoup.nodes.Document doc = (org.jsoup.nodes.Document) document.getMetadata().get("originalDocument");
		System.out.println(doc.body());
		System.out.println("First heading: " + doc.select("h1").first().text());
	}

}