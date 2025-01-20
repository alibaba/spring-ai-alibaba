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

import java.io.InputStream;
import java.util.List;

/**
 * @author HeYQ
 * @since 2024-12-02 11:25
 */

public interface DocumentParser {

	/**
	 * Parses a given {@link InputStream} into a {@link Document}. The specific
	 * implementation of this method will depend on the type of the document being parsed.
	 * <p>
	 * Note: This method does not close the provided {@link InputStream} - it is the
	 * caller's responsibility to manage the lifecycle of the stream.
	 * @param inputStream The {@link InputStream} that contains the content of the
	 * {@link Document}.
	 * @return The parsed {@link Document}.
	 */
	List<Document> parse(InputStream inputStream);

}
