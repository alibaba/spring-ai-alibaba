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
 * Format conversion utility class providing Markdown to PDF conversion
 *
 * @author sixiyida
 * @since 2025/6/20
 */
public final class FormatConversionUtil {

	private static final Logger logger = LoggerFactory.getLogger(FormatConversionUtil.class);

	// HTTP connection timeout settings (milliseconds)
	private static final int RESOURCE_HTTP_CONNECT_TIMEOUT = 1000;

	private static final int RESOURCE_HTTP_READ_TIMEOUT = 1000;

	// Font path
	private static final String FONT_PATH = "report/fonts/AlibabaPuHuiTi-3-55-Regular.ttf";

	private static final String FONT_FAMILY = "AlibabaPuHuiTi";

	private static final int FONT_WEIGHT = 400; // Regular

	/**
	 * Converts HTML content to PDF byte array
	 * @param htmlContent HTML content
	 * @return Byte array of PDF content
	 * @throws RuntimeException If conversion fails
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
	 * Creates a PDF renderer builder
	 * @return Configured PdfRendererBuilder instance
	 */
	private static PdfRendererBuilder createPdfRendererBuilder() {
		PdfRendererBuilder builder = new PdfRendererBuilder();
		builder.useHttpStreamImplementation(
				new DefaultHttpStreamFactory(RESOURCE_HTTP_CONNECT_TIMEOUT, RESOURCE_HTTP_READ_TIMEOUT));
		builder.useFastMode();
		return builder;
	}

	/**
	 * Configures fonts
	 * @param builder PDF renderer builder
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
	 * Configures base URI
	 * @param builder PDF renderer builder
	 */
	private static void configureBaseUri(PdfRendererBuilder builder) {
		String baseUri = getBaseUri();
		if (baseUri != null) {
			builder.withHtmlContent(null, baseUri);
		}
	}

	/**
	 * Retrieves the base URI
	 * @return Base URI, returns null if retrieval fails
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
	 * Converts HTML content to PDF and saves to file
	 * @param htmlContent HTML content
	 * @param pdfFilePath Output PDF file path
	 * @throws RuntimeException If saving fails
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
	 * Directly converts Markdown content to PDF byte array
	 * @param markdownContent Markdown content
	 * @return Byte array of PDF content
	 */
	public static byte[] convertMarkdownToPdfBytes(String markdownContent) {
		// Convert Markdown to HTML
		String htmlContent = HtmlGenerationUtil.markdownToHtml(markdownContent);
		// Convert HTML to PDF
		return convertHtmlToPdfBytes(htmlContent);
	}

	/**
	 * Directly converts Markdown content to PDF and saves to file
	 * @param markdownContent Markdown content
	 * @param pdfFilePath Output PDF file path
	 */
	public static void convertMarkdownToPdfFile(String markdownContent, String pdfFilePath) {
		// Convert Markdown to HTML
		String htmlContent = HtmlGenerationUtil.markdownToHtml(markdownContent);
		// Convert HTML to PDF and save
		convertHtmlToPdfFile(htmlContent, pdfFilePath);
	}

}
