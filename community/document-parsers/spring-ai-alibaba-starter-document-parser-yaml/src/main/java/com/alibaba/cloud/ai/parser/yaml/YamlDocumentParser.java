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
import org.springframework.ai.document.Document;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author HeYQ
 * @since 2025-05-02 18:30
 */
public class YamlDocumentParser implements DocumentParser {

	private final boolean strict;

	private final Class<? extends Object> beanClass;

	public YamlDocumentParser() {
		this(false, null);
	}

	public YamlDocumentParser(boolean strict) {
		this(strict, null);
	}

	public YamlDocumentParser(Class<? extends Object> beanClass) {
		this(false, beanClass);
	}

	public YamlDocumentParser(boolean strict, Class<? extends Object> beanClass) {
		this.strict = strict;
		this.beanClass = beanClass;
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		try {
			// Create LoaderOptions for configuring the parser
			LoaderOptions loaderOptions = new LoaderOptions();
			loaderOptions.setAllowDuplicateKeys(false);

			// Set up the constructor if a specific Java class is provided
			Constructor constructor = null;
			if (beanClass != null) {
				constructor = new Constructor(beanClass, loaderOptions);
			}
			else {
				constructor = new Constructor(loaderOptions);
			}

			// Build the Yaml instance using the constructor and options
			Yaml yaml = new Yaml(constructor);

			// Load the YAML content
			Object data = yaml.load(inputStream);

			// Convert parsed data to string representation for the Document
			String textContent = convertToString(data);

			// Create and populate the Document object
			Document document = new Document(textContent);
			Map<String, Object> metaData = document.getMetadata();
			metaData.put("format", "YAML");
			metaData.put("source", "YAML input stream");
			metaData.put("originalData", data);

			return Collections.singletonList(document);
		}
		catch (Exception e) {
			if (strict) {
				throw new RuntimeException("Failed to parse YAML content", e);
			}
			else {
				return Collections.emptyList();
			}
		}
	}

	/**
	 * Converts arbitrary object (Map, Collection, or primitive) into a readable String
	 * format.
	 */
	private String convertToString(Object obj) {
		if (obj == null) {
			return "";
		}
		else if (obj instanceof String) {
			return (String) obj;
		}
		else if (obj instanceof Map) {
			return ((Map<?, ?>) obj).entrySet()
				.stream()
				.map(e -> e.getKey() + ": " + convertToString(e.getValue()))
				.collect(Collectors.joining("\n"));
		}
		else if (obj instanceof Collection) {
			return ((Collection<?>) obj).stream().map(this::convertToString).collect(Collectors.joining(", "));
		}
		else {
			return Objects.toString(obj);
		}
	}

}
