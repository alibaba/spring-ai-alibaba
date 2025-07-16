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
package com.alibaba.cloud.ai.document;

import org.springframework.ai.document.Document;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * @author HeYQ
 * @since 2024-12-08 21:13
 */
public class TextDocumentParser implements DocumentParser {

	private final Charset charset;

	public TextDocumentParser() {
		this(StandardCharsets.UTF_8);
	}

	public TextDocumentParser(Charset charset) {
		Assert.notNull(charset, "charset");
		this.charset = charset;
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		try {
			String text = new String(inputStream.readAllBytes(), charset);
			if (text.isBlank()) {
				throw new Exception();
			}
			return Collections.singletonList(new Document(text));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
