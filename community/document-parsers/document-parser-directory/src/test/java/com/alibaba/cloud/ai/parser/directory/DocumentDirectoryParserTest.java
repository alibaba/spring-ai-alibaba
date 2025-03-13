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

package com.alibaba.cloud.ai.parser.directory;

import com.alibaba.cloud.ai.document.TextDocumentParser;
import org.junit.Test;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @author HeYQ
 * @author brianxiadong
 * @since 2025-02-07 19:21
 */

public class DocumentDirectoryParserTest {

	// Load all non-hidden files in a directory.
	@Test
	public void testAllNoHidden() {
		String path = "src/test/resources";

		DocumentDirectoryParser parser = new DocumentDirectoryParser.Builder(path)
			.documentParser(new TextDocumentParser())
			.build();
		List<Document> documents = parser.parse();
		for (Document document : documents) {
			System.out.println(document.getText());
		}
	}

	// Load all text files in a directory without recursion.
	@Test
	public void testAllTextNoHidden() {
		String path = "src/test/resources";

		DocumentDirectoryParser parser = new DocumentDirectoryParser.Builder(path)
			.documentParser(new TextDocumentParser())
			.glob("*.txt")
			.build();
		List<Document> documents = parser.parse();
		for (Document document : documents) {
			System.out.println(document.getText());
		}
	}

	// Recursively load all text files in a directory.
	@Test
	public void testAllTextRecursive() {
		String path = "src/test/resources";
		DocumentDirectoryParser parser = new DocumentDirectoryParser.Builder(path)
			.documentParser(new TextDocumentParser())
			.glob("*.txt")
			.recursive(true)
			.build();
		List<Document> documents = parser.parse();
		for (Document document : documents) {
			System.out.println(document.getText());
		}
	}

	// Load all files in a directory, except for py files.
	@Test
	public void testExceptNoHidden() {
		String path = "src/test/resources";

		DocumentDirectoryParser parser = new DocumentDirectoryParser.Builder(path)
			.documentParser(new TextDocumentParser())
			.exclude("*.py")
			.build();
		List<Document> documents = parser.parse();
		for (Document document : documents) {
			System.out.println(document.getText());
		}
	}

}
