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
package com.alibaba.cloud.ai.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

public class MarkdownParser {

	public static String extractText(String markdownCode) {
		String code = extractRawText(markdownCode);
		return NewLineParser.format(code);
	}

	public static String extractRawText(String markdownCode) {
		String startDelimiter = "```";

		int startIndex = markdownCode.indexOf(startDelimiter);
		int endIndex = markdownCode.indexOf(startDelimiter, startIndex + startDelimiter.length());
		if (startIndex != -1 && endIndex != -1) {
			markdownCode = markdownCode.substring(startIndex, endIndex + startDelimiter.length());
		}
		else {
			return markdownCode;
		}

		Parser parser = Parser.builder().build();
		Node document = parser.parse(markdownCode);
		TextContentRenderer txr = TextContentRenderer.builder().build();
		String code = txr.render(document);
		return code;
	}

}
