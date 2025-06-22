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

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.swing.NaiveUserAgent.DefaultHttpStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * 格式转换工具类，提供Markdown到PDF的转换
 *
 * @author sixiyida
 * @since 2025/6/20
 */
public final class FormatConversionUtil {

	private static final Logger logger = LoggerFactory.getLogger(FormatConversionUtil.class);

	// HTTP连接超时设置（毫秒）
	private static final int RESOURCE_HTTP_CONNECT_TIMEOUT = 1000;

	private static final int RESOURCE_HTTP_READ_TIMEOUT = 1000;

	// 字体路径
	private static final String FONT_PATH = "report/fonts/AlibabaPuHuiTi-3-55-Regular.ttf";

	private static final String FONT_FAMILY = "AlibabaPuHuiTi";

	private static final int FONT_WEIGHT = 400; // Regular

	/**
	 * 将HTML内容转换为PDF字节数组
	 * @param htmlContent HTML内容
	 * @return PDF内容的字节数组
	 * @throws RuntimeException 如果转换失败
	 */
	public static byte[] convertHtmlToPdfBytes(String htmlContent) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = createPdfRendererBuilder();
			configureFont(builder);
			configureBaseUri(builder);

			builder.withHtmlContent(htmlContent, getBaseUri());
			builder.toStream(baos);
			builder.run();

			return baos.toByteArray();
		}
		catch (Exception e) {
			logger.error("Failed to convert HTML to PDF", e);
			throw new RuntimeException("Failed to convert HTML to PDF", e);
		}
	}

	/**
	 * 创建PDF渲染器构建器
	 * @return 配置好的PdfRendererBuilder实例
	 */
	private static PdfRendererBuilder createPdfRendererBuilder() {
		PdfRendererBuilder builder = new PdfRendererBuilder();
		builder.useHttpStreamImplementation(
				new DefaultHttpStreamFactory(RESOURCE_HTTP_CONNECT_TIMEOUT, RESOURCE_HTTP_READ_TIMEOUT));
		builder.useFastMode();
		return builder;
	}

	/**
	 * 配置字体
	 * @param builder PDF渲染器构建器
	 */
	private static void configureFont(PdfRendererBuilder builder) {
		try {
			Resource fontResource = new ClassPathResource(FONT_PATH);
			if (fontResource.exists()) {
				File fontFile = fontResource.getFile();
				builder.useFont(fontFile, FONT_FAMILY, FONT_WEIGHT, BaseRendererBuilder.FontStyle.NORMAL, true // 嵌入PDF
				);
				logger.info("Font loaded from classpath for PDF conversion");
			}
			else {
				logger.warn("AlibabaPuHuiTi font file not found in classpath, using default fonts");
			}
		}
		catch (Exception e) {
			logger.warn("Error loading font from classpath: {}", e.getMessage());
		}
	}

	/**
	 * 配置基础URI
	 * @param builder PDF渲染器构建器
	 */
	private static void configureBaseUri(PdfRendererBuilder builder) {
		String baseUri = getBaseUri();
		if (baseUri != null) {
			builder.withHtmlContent(null, baseUri);
		}
	}

	/**
	 * 获取基础URI
	 * @return 基础URI，如果获取失败则返回null
	 */
	private static String getBaseUri() {
		try {
			URL resourceUrl = FormatConversionUtil.class.getClassLoader().getResource("");
			if (resourceUrl != null) {
				String baseUri = resourceUrl.toString();
				logger.info("Using base URI for PDF conversion: {}", baseUri);
				return baseUri;
			}
		}
		catch (Exception e) {
			logger.warn("Error getting classpath base URL: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * 将HTML内容转换为PDF并保存到文件
	 * @param htmlContent HTML内容
	 * @param pdfFilePath 输出PDF文件路径
	 * @throws RuntimeException 如果保存失败
	 */
	public static void convertHtmlToPdfFile(String htmlContent, String pdfFilePath) {
		try (OutputStream os = new FileOutputStream(pdfFilePath)) {
			byte[] pdfBytes = convertHtmlToPdfBytes(htmlContent);
			os.write(pdfBytes);
			logger.info("HTML converted to PDF and saved to: {}", pdfFilePath);
		}
		catch (Exception e) {
			logger.error("Failed to save PDF to file", e);
			throw new RuntimeException("Failed to save PDF to file", e);
		}
	}

	/**
	 * 将Markdown内容直接转换为PDF字节数组
	 * @param markdownContent Markdown内容
	 * @return PDF内容的字节数组
	 */
	public static byte[] convertMarkdownToPdfBytes(String markdownContent) {
		// 将Markdown转换为HTML
		String htmlContent = HtmlGenerationUtil.markdownToHtml(markdownContent);
		// 将HTML转换为PDF
		return convertHtmlToPdfBytes(htmlContent);
	}

	/**
	 * 将Markdown内容直接转换为PDF并保存到文件
	 * @param markdownContent Markdown内容
	 * @param pdfFilePath 输出PDF文件路径
	 */
	public static void convertMarkdownToPdfFile(String markdownContent, String pdfFilePath) {
		// 将Markdown转换为HTML
		String htmlContent = HtmlGenerationUtil.markdownToHtml(markdownContent);
		// 将HTML转换为PDF并保存
		convertHtmlToPdfFile(htmlContent, pdfFilePath);
	}

}
