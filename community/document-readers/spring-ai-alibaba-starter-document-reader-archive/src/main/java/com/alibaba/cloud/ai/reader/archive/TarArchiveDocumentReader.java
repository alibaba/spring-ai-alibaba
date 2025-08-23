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
package com.alibaba.cloud.ai.reader.archive;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.BufferedInputStream;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link DocumentReader} implementation for parsing TAR archive files.
 * <p>
 * This reader processes uncompressed TAR archives, extracting each entry and converting
 * it into {@link Document} objects using the provided {@link DocumentParser}.
 * </p>
 * <p>
 * Supported extensions: .tar
 *
 * @author aruato
 */
public class TarArchiveDocumentReader extends AbstractArchiveDocumentReader<TarArchiveEntry> {

	public TarArchiveDocumentReader(String resourceUrl, DocumentParser parser) {
		this(new DefaultResourceLoader().getResource(resourceUrl), parser, UTF_8.name());
	}

	public TarArchiveDocumentReader(Resource resource, DocumentParser parser) {
		super(resource, parser, UTF_8.name());
	}

	public TarArchiveDocumentReader(String resourceUrl, DocumentParser parser, String charset) {
		this(new DefaultResourceLoader().getResource(resourceUrl), parser, charset);
	}

	public TarArchiveDocumentReader(Resource resource, DocumentParser parser, String charset) {
		super(resource, parser, charset);
	}

	@Override
	protected ArchiveInputStream<TarArchiveEntry> createArchiveStream(InputStream in, String charset) {
		return new TarArchiveInputStream(new BufferedInputStream(in), charset);
	}

}
