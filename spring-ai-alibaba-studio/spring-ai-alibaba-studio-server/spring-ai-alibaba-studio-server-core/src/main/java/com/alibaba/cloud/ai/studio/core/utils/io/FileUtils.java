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

package com.alibaba.cloud.ai.studio.core.utils.io;

import org.apache.commons.io.FilenameUtils;
import org.springframework.http.MediaType;

/**
 * Utility class for file operations. Provides methods for handling file-related
 * operations such as content type detection.
 *
 * @since 1.0.0.3
 */
public class FileUtils {

	/**
	 * Determines the content type (MIME type) based on file extension. Supports common
	 * file types including PDF, images, text, and data formats.
	 * @param filename The name of the file
	 * @return The corresponding MediaType value for the file extension
	 */
	public static String getContentType(String filename) {
		String extension = FilenameUtils.getExtension(filename).toLowerCase();
		return switch (extension) {
			case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
			case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
			case "png" -> MediaType.IMAGE_PNG_VALUE;
			case "gif" -> MediaType.IMAGE_GIF_VALUE;
			case "txt" -> MediaType.TEXT_PLAIN_VALUE;
			case "html" -> MediaType.TEXT_HTML_VALUE;
			case "xml" -> MediaType.APPLICATION_XML_VALUE;
			case "json" -> MediaType.APPLICATION_JSON_VALUE;
			default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
		};
	}

	/**
	 * get temp file path
	 * @param name file name
	 * @return temp file path
	 */
	public static String getTempFilePath(String name) {
		String path = System.getProperty("java.io.tmpdir");
		return String.format("%s/%s", path, name);
	}

	/**
	 * get unique file name
	 * @param name file name
	 * @return unique file name
	 */
	public static String getUniqueFileName(String name) {
		if (name.indexOf('.') == -1) {
			return name + "_" + System.currentTimeMillis();
		}

		String fileName = name.substring(0, name.lastIndexOf('.'));
		String fileExtension = name.substring(name.lastIndexOf('.'));
		return fileName + "_" + System.currentTimeMillis() + fileExtension;
	}

}
