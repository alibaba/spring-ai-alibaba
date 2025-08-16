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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for archive-based {@link DocumentReader} implementations. Holds the
 * shared {@link Resource} and {@link DocumentParser} required for parsing entries from
 * various archive formats (ZIP, TAR, TGZ, etc.).
 *
 * @author aruato
 */
public abstract class AbstractArchiveDocumentReader<E extends ArchiveEntry> implements DocumentReader {

	/**
	 * Metadata key representing the source of the document.
	 */
	public static final String METADATA_SOURCE = "source";

	private final Resource resource;

	private final DocumentParser parser;

	private final String charset;

	protected AbstractArchiveDocumentReader(Resource resource, DocumentParser parser, String charset) {
		this.resource = resource;
		this.parser = parser;
		this.charset = charset;
	}

	@Override
	public List<Document> get() {
		try (InputStream raw = resource.getInputStream();
				ArchiveInputStream<E> ais = createArchiveStream(raw, charset)) {

			List<Document> docs = new ArrayList<>();
			E entry;
			while ((entry = ais.getNextEntry()) != null) {
				// ignore directories
				if (entry.isDirectory()) {
					continue;
				}

				// Read file content from the archive entry
				byte[] entryContent = ais.readAllBytes();

				// Parse entry content and enrich with metadata
				try (InputStream entryInputStream = new ByteArrayInputStream(entryContent)) {
					for (Document doc : parser.parse(entryInputStream)) {
						doc.getMetadata().put(METADATA_SOURCE, entry.getName());
						docs.add(doc);
					}
				}
			}
			return docs;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates an {@link ArchiveInputStream} for a specific archive format.
	 * <p>
	 * This method is responsible for wrapping the provided raw {@link InputStream} and
	 * applying decompression if necessary (e.g., for <code>.tar.gz</code> files).
	 * <p>
	 * Subclasses must implement this method to return a properly configured
	 * {@link ArchiveInputStream} suitable for reading entries of their specific archive
	 * format.
	 */
	protected abstract ArchiveInputStream<E> createArchiveStream(InputStream in, String charset) throws IOException;

}
