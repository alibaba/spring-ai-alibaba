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
package com.alibaba.cloud.ai.parser.bibtex;

import com.alibaba.cloud.ai.document.DocumentParser;
import com.alibaba.cloud.ai.parser.apache.pdfbox.PagePdfDocumentParser;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author HeYQ
 * @since 2025-01-02 23:15
 */

public class BibtexDocumentParser implements DocumentParser {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	// US-ASCII UTF-8
	private final String charsetName;

	private final Integer maxContentChars;

	private final Integer maxDocs;

	private final Pattern filePattern;

	private final DocumentParser parser;

	public BibtexDocumentParser() {
		this("UTF-8", null, null, null,
				new PagePdfDocumentParser(PdfDocumentReaderConfig.builder()
					.withPageTopMargin(0)
					.withPageBottomMargin(0)
					.withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
						.withNumberOfTopTextLinesToDelete(0)
						.withNumberOfBottomTextLinesToDelete(3)
						.withNumberOfTopPagesToSkipBeforeDelete(0)
						.build())
					.withPagesPerDocument(1)
					.build()));
	}

	public BibtexDocumentParser(String charsetName, Integer maxContentChars, Integer maxDocs, Pattern filePattern,
			DocumentParser parser) {
		this.charsetName = charsetName;
		this.maxContentChars = maxContentChars;
		this.maxDocs = maxDocs;
		this.filePattern = filePattern;
		this.parser = parser;
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		try (Reader reader = new InputStreamReader(inputStream, charsetName)) {
			List<Document> documentList = new ArrayList<>(10);
			BibTeXParser bibtexParser = new BibTeXParser();
			BibTeXDatabase database = bibtexParser.parse(reader);
			Map<Key, BibTeXEntry> entries = database.getEntries();
			if (entries.isEmpty()) {
				return documentList;
			}
			if (maxDocs != null && maxDocs > 0 && entries.size() > maxDocs) {
				entries = entries.entrySet()
					.stream()
					.limit(maxDocs)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
							(existing, replacement) -> existing));
			}
			for (BibTeXEntry entry : entries.values()) {
				Map<String, Object> metadata = new HashMap<>();
				metadata.put(entry.getType().getValue(), entry.getKey());
				for (Key key : entry.getFields().keySet()) {
					Value value = entry.getFields().get(key);
					metadata.put(key.getValue(), value.toUserString());
				}
				List<String> fileNames = new ArrayList<>();
				if (metadata.containsKey("file")) {
					String fileValue = metadata.get("file").toString();
					if (!Objects.isNull(filePattern)) {
						Matcher matcher = filePattern.matcher(metadata.get("file").toString());
						while (matcher.find()) {
							fileNames.add(matcher.group());
						}
					}
					else {
						Collections.addAll(fileNames, fileValue.split("[;,\\s]+"));
					}
				}
				StringBuilder content = new StringBuilder(metadata.getOrDefault("abstract", "").toString());
				if (!fileNames.isEmpty()) {
					for (String fileName : fileNames) {
						try (InputStream fileInputStream = new DefaultResourceLoader()
							.getResource("classpath:/" + fileName)
							.getInputStream()) {
							List<Document> docs = parser.parse(fileInputStream);
							if (!docs.isEmpty()) {
								content.append(docs.get(0).getText());
							}
						}
						catch (IOException e) {
							// Log the exception and continue with the next file
							logger.warn("Failed to read file: {}", fileName, e);
						}

					}
				}

				if (maxContentChars != null && maxContentChars > 0) {
					int endIndex = Math.min(maxContentChars, content.length());
					content = new StringBuilder(content.substring(0, endIndex));
				}

				Document document = new Document(content.toString(), metadata);
				documentList.add(document);
			}

			return documentList;
		}
		catch (Exception e) {
			logger.error("Error parsing input stream", e);
			throw new RuntimeException("Error parsing input stream", e);
		}

	}

}
