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
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link DocumentReader} implementation for parsing GZIP-compressed TAR files (TGZ).
 * <p>
 * This reader first decompresses the GZIP stream, then extracts each TAR archive entry
 * and converts it into {@link Document} objects using the provided
 * {@link DocumentParser}.
 * </p>
 * <p>
 * Supported extensions: .tgz, .tar.gz
 *
 * @author aruato
 */
public class TgzArchiveDocumentReader extends AbstractArchiveDocumentReader<TarArchiveEntry> {

	public TgzArchiveDocumentReader(String resourceUrl, DocumentParser parser) {
		this(new DefaultResourceLoader().getResource(resourceUrl), parser, UTF_8.name());
	}

	public TgzArchiveDocumentReader(Resource resource, DocumentParser parser) {
		super(resource, parser, UTF_8.name());
	}

	public TgzArchiveDocumentReader(String resourceUrl, DocumentParser parser, String charset) {
		this(new DefaultResourceLoader().getResource(resourceUrl), parser, charset);
	}

	public TgzArchiveDocumentReader(Resource resource, DocumentParser parser, String charset) {
		super(resource, parser, charset);
	}

	@Override
	protected ArchiveInputStream<TarArchiveEntry> createArchiveStream(InputStream in, String charset)
			throws IOException {
		InputStream gzipIn = new GzipCompressorInputStream(new BufferedInputStream(in));
		return new TarArchiveInputStream(new BufferedInputStream(gzipIn), charset);
	}

}
