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
package com.alibaba.cloud.ai.parser.yaml;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.document.Document;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author HeYQ
 * @since 2025-05-02 18:47
 */
class YamlDocumentParserTest {

	@ParameterizedTest
	@ValueSource(strings = { "test-file.yaml", "test-file.yml" })
	void should_parse_yaml_and_yml_files(String fileName) {
		DocumentParser parser = new YamlDocumentParser();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		List<Document> documents = parser.parse(inputStream);
		Document document = documents.get(0);

		System.out.println(document.getText());

		assertThat(document.getMetadata()).isNotEmpty();
		assertThat(document.getMetadata().get("format")).isEqualTo("YAML");
		assertThat(document.getMetadata().get("source")).isEqualTo("YAML input stream");
	}

	@Test
	void should_parse_yaml_files_stateless() {
		DocumentParser parser = new YamlDocumentParser();
		InputStream inputStream1 = getClass().getClassLoader().getResourceAsStream("test-file.yaml");
		InputStream inputStream2 = getClass().getClassLoader().getResourceAsStream("test-file.yaml");

		List<Document> documents1 = parser.parse(inputStream1);
		List<Document> documents2 = parser.parse(inputStream2);

		Document doc1 = documents1.get(0);
		Document doc2 = documents2.get(0);

		assertThat(doc1.getText()).isEqualToIgnoringWhitespace(doc2.getText());
		assertThat(doc1.getMetadata()).isEqualTo(doc2.getMetadata());
	}

}
