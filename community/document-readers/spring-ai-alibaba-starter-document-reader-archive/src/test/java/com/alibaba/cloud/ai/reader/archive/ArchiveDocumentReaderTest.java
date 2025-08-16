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
package com.alibaba.cloud.ai.reader.archive;

import com.alibaba.cloud.ai.document.TextDocumentParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArchiveDocumentReaderTest {

	@ParameterizedTest
	@ValueSource(strings = { "resources.tar" })
	void shouldParseTarFile(String fileName) {
		{
			Resource resource = new DefaultResourceLoader().getResource(fileName);
			DocumentReader archiveDocumentReader = new TarArchiveDocumentReader(resource, new TextDocumentParser(),
					"UTF-8");
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			Resource resource = new DefaultResourceLoader().getResource(fileName);
			DocumentReader archiveDocumentReader = new TarArchiveDocumentReader(resource, new TextDocumentParser());
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			DocumentReader archiveDocumentReader = new TarArchiveDocumentReader(fileName, new TextDocumentParser(),
					"UTF-8");
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			DocumentReader archiveDocumentReader = new TarArchiveDocumentReader(fileName, new TextDocumentParser());
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "resources.tar.gz" })
	void shouldParseTgzFile(String fileName) {
		{
			Resource resource = new DefaultResourceLoader().getResource(fileName);
			DocumentReader archiveDocumentReader = new TgzArchiveDocumentReader(resource, new TextDocumentParser(),
					"UTF-8");
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			Resource resource = new DefaultResourceLoader().getResource(fileName);
			DocumentReader archiveDocumentReader = new TgzArchiveDocumentReader(resource, new TextDocumentParser());
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			DocumentReader archiveDocumentReader = new TgzArchiveDocumentReader(fileName, new TextDocumentParser(),
					"UTF-8");
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			DocumentReader archiveDocumentReader = new TgzArchiveDocumentReader(fileName, new TextDocumentParser());
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "resources.zip" })
	void shouldParseZipFile(String fileName) {
		{
			Resource resource = new DefaultResourceLoader().getResource(fileName);
			DocumentReader archiveDocumentReader = new ZipArchiveDocumentReader(resource, new TextDocumentParser(),
					"UTF-8");
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			Resource resource = new DefaultResourceLoader().getResource(fileName);
			DocumentReader archiveDocumentReader = new ZipArchiveDocumentReader(resource, new TextDocumentParser());
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			DocumentReader archiveDocumentReader = new ZipArchiveDocumentReader(fileName, new TextDocumentParser(),
					"UTF-8");
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}

		{
			DocumentReader archiveDocumentReader = new ZipArchiveDocumentReader(fileName, new TextDocumentParser());
			List<Document> documents = archiveDocumentReader.get();
			assertNotNull(documents);
			assertFalse(documents.isEmpty());
			for (Document document : documents) {
				assertNotNull(document.getText());
				System.out.println(document);
			}
		}
	}

}
