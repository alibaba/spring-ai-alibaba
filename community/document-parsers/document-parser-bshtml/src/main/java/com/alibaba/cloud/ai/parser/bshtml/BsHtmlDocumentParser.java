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
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.ai.document.Document;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author HeYQ
 * @since 2025-01-02 18:26
 */

public class BsHtmlDocumentParser implements DocumentParser {

	private final String charsetName;

	private final String baseUri;

	private final Parser parser;

	public BsHtmlDocumentParser(Parser parser) {
		this("UTF-8", "", parser);
	}

	public BsHtmlDocumentParser(String charsetName, String baseUri) {
		this(charsetName, baseUri, null);
	}

	public BsHtmlDocumentParser() {
		this("UTF-8", "", Parser.htmlParser().newInstance());
	}

	public BsHtmlDocumentParser(String charsetName, String baseUri, Parser parser) {
		this.charsetName = charsetName;
		this.baseUri = baseUri;
		this.parser = parser;
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(inputStream, charsetName, baseUri, parser);
			String text = doc.text();
			String title = doc.title().isEmpty() ? "" : doc.title();
			Document document = new Document(text);
			Map<String, Object> metaData = document.getMetadata();
			metaData.put("title", title);
			metaData.put("source", baseUri);
			metaData.put("originalDocument", doc);

			return List.of(document);

		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
