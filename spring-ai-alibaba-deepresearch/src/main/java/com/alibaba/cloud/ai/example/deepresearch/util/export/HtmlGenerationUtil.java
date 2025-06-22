/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.util.export;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

/**
 * HTML生成工具类，提供Markdown到HTML的转换功能
 *
 * @author sixiyida
 * @since 2025/6/20
 */
public final class HtmlGenerationUtil {

	private static final Logger logger = LoggerFactory.getLogger(HtmlGenerationUtil.class);

	// 资源路径
	private static final String FONT_PATH = "report/fonts/AlibabaPuHuiTi-3-55-Regular.ttf";

	private static final String CSS_PATH = "report/css/github-markdown.css";

	private static final String FONT_FAMILY = "AlibabaPuHuiTi";

	// HTML模板部分
	private static final String HTML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" + "<head>\n"
			+ "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n";

	private static final String HTML_STYLE_START = "    <style type=\"text/css\">\n";

	private static final String HTML_STYLE_END = "    </style>\n" + "</head>\n" + "<body class=\"markdown-body\">\n";

	private static final String HTML_FOOTER = "\n</body>\n</html>";

	private static final Parser markdownParser;

	private static final HtmlRenderer htmlRenderer;

	static {
		List<Extension> extensions = List.of(TablesExtension.create());
		markdownParser = Parser.builder().extensions(extensions).build();
		htmlRenderer = HtmlRenderer.builder().extensions(extensions).build();
	}

	/**
	 * 将Markdown内容转换为HTML
	 * @param markdownContent Markdown内容
	 * @return 转换后的HTML内容
	 */
	public static String convertMarkdownToHtml(String markdownContent) {
		Node document = markdownParser.parse(markdownContent);
		return htmlRenderer.render(document);
	}

	/**
	 * 将HTML内容包装成完整的HTML文档
	 * @param htmlContent HTML内容
	 * @return 完整的HTML文档
	 */
	public static String wrapHtmlContent(String htmlContent) {
		StringBuilder html = new StringBuilder(HTML_HEADER);

		// 添加CSS链接 - 确保CSS始终被加载
		String cssUrl = getResourceUrl(CSS_PATH);
		if (cssUrl != null && !cssUrl.isEmpty()) {
			html.append("    <link rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssUrl).append("\"/>\n");
		}
		else {
			logger.warn("External CSS file not found. Styling may be affected.");
		}

		// 添加样式
		html.append(HTML_STYLE_START);

		// 添加字体
		String fontUrl = getResourceUrl(FONT_PATH);
		if (fontUrl != null && !fontUrl.isEmpty()) {
			html.append("        @font-face {\n")
				.append("            font-family: '")
				.append(FONT_FAMILY)
				.append("';\n")
				.append("            src: url('")
				.append(fontUrl)
				.append("') format('truetype');\n")
				.append("            font-weight: 400;\n")
				.append("            font-style: normal;\n")
				.append("        }\n");
		}

		// 结束样式，添加HTML内容和页脚
		html.append(HTML_STYLE_END).append(htmlContent).append(HTML_FOOTER);

		String result = html.toString();
		logger.debug("Generated HTML document with {} bytes", result.length());
		return result;
	}

	/**
	 * 获取资源URL
	 * @param resourcePath 资源路径
	 * @return 资源URL字符串，如果获取失败则返回null
	 */
	private static String getResourceUrl(String resourcePath) {
		try {
			URL resourceUrl = HtmlGenerationUtil.class.getClassLoader().getResource(resourcePath);
			if (resourceUrl != null) {
				String url = resourceUrl.toString();
				logger.info("Resource URL for {}: {}", resourcePath, url);
				return url;
			}
			else {
				logger.warn("Resource not found in classpath: {}", resourcePath);
			}
		}
		catch (Exception e) {
			logger.error("Error getting resource URL for {}: {}", resourcePath, e.getMessage());
		}
		return null;
	}

	/**
	 * 将Markdown内容转换为完整的HTML文档
	 * @param markdownContent Markdown内容
	 * @return 完整的HTML文档
	 */
	public static String markdownToHtml(String markdownContent) {
		String htmlContent = convertMarkdownToHtml(markdownContent);
		return wrapHtmlContent(htmlContent);
	}

}
