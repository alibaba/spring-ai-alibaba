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

import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.util.Base64;

/**
 * Utility class for handling media data operations. Provides methods for encoding media
 * resources into base64 format.
 *
 * @since 1.0.0.3
 */
public class MediaDataUtils {

	/**
	 * Encodes a resource into a base64 data URL string.
	 * @param mimeType The MIME type of the resource
	 * @param resource The resource to be encoded
	 * @return A base64 encoded data URL string
	 * @throws RuntimeException if the resource cannot be read
	 */
	public static String base64encode(MimeType mimeType, Resource resource) {
		try {
			byte[] bytes = resource.getContentAsByteArray();
			String encodedData = Base64.getEncoder().encodeToString(bytes);
			return "data:" + mimeType + ";base64," + encodedData;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
