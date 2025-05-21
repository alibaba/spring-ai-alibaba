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
