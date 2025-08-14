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

package com.alibaba.cloud.ai.studio.core.rag.indices;

import com.alibaba.cloud.ai.studio.runtime.enums.ChunkType;
import com.alibaba.cloud.ai.studio.runtime.enums.DocumentType;
import com.alibaba.cloud.ai.studio.runtime.enums.UploadType;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.ProcessConfig;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import com.alibaba.cloud.ai.studio.core.base.manager.OssManager;
import com.alibaba.cloud.ai.studio.core.rag.reader.TextDocumentReader;
import com.alibaba.cloud.ai.studio.core.rag.splitter.RegexTextSplitter;
import com.alibaba.cloud.ai.studio.core.rag.vectorstore.VectorStoreFactory;
import com.alibaba.cloud.ai.studio.core.utils.io.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pipeline for processing and indexing knowledge base documents. Handles document
 * parsing, transformation, and storage in vector store.
 *
 * @since 1.0.0.3
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseIndexPipeline implements IndexPipeline {

	/** Factory for creating vector store instances */
	private final VectorStoreFactory vectorStoreFactory;

	/** Application configuration properties */
	private final StudioProperties properties;

	/** oss manager */
	private final OssManager ossManager;

	/**
	 * Parses documents based on their format (PDF, DOC, MD, TXT, etc.)
	 * @param document The document to parse
	 * @return List of parsed documents
	 */
	public List<Document> parse(com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document document) {
		List<Document> documents;
		String format = document.getFormat();

		String path;
		if (document.getType() == DocumentType.OSS
				|| UploadType.OSS.getValue().equalsIgnoreCase(properties.getUploadMethod())) {
			path = FileUtils.getTempFilePath(document.getDocId());
			ossManager.downloadFile(document.getPath(), path);
		}
		else if (document.getType() == DocumentType.FILE) {
			path = properties.getStoragePath() + File.separator + document.getPath();
		}
		else {
			throw new RuntimeException("unsupported document type: " + document.getType());
		}

		File file = new File(path);
		if (!file.exists()) {
			throw new RuntimeException("file does not exist, path: " + path);
		}

		format = StringUtils.lowerCase(format);
		switch (format) {
			case "pdf": {
				DocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(file));
				documents = reader.get();
				break;
			}
			case "doc", "docx", "ppt", "pptx": {
				DocumentReader reader = new TikaDocumentReader(new FileSystemResource(file));
				documents = reader.get();
				break;
			}
			case "md", "markdown": {
				DocumentReader reader = new MarkdownDocumentReader(new FileSystemResource(file),
						MarkdownDocumentReaderConfig.defaultConfig());
				documents = reader.get();
				break;
			}
			case "txt": {
				DocumentReader reader = new TextDocumentReader(new FileSystemResource(file));
				documents = reader.get();
				break;
			}
			default:
				throw new IllegalArgumentException("unsupported format: " + format);
		}

		log.info("{} documents parsed", documents.size());

		return documents;
	}

	/**
	 * Transforms documents by splitting them into chunks
	 * @param documents Documents to transform
	 * @param processConfig Configuration for the transformation process
	 * @return List of transformed documents
	 */
	public List<Document> transform(List<Document> documents, ProcessConfig processConfig) {

		// TODO now use this simple chunk splitter first
		ChunkType chunkType = processConfig.getChunkType();
		TextSplitter splitter = null;
		if (Objects.requireNonNull(chunkType) == ChunkType.REGEX) {
			String regex = processConfig.getRegex();
			if (StringUtils.isBlank(regex)) {
				throw new IllegalArgumentException("regex cannot be empty");
			}

			splitter = new RegexTextSplitter(regex, processConfig.getChunkOverlap());
		}
		else {
			splitter = new TokenTextSplitter(processConfig.getChunkSize(), processConfig.getChunkOverlap(), 1, 10000,
					false);
		}
		List<Document> transformedDocs = splitter.apply(documents);

		log.info("{} documents transformed", transformedDocs.size());
		return transformedDocs;
	}

	/**
	 * Stores document chunks in the vector store with metadata
	 * @param chunks Document chunks to store
	 * @param indexConfig Index configuration
	 * @param metadata Additional metadata to attach to chunks
	 */
	@Override
	public void store(List<Document> chunks, IndexConfig indexConfig, Map<String, Object> metadata) {
		Assert.notNull(chunks, "chunks cannot be null");

		log.info("embedding and save to vector store, chunks: {}, indexConfig: {}", chunks.size(), indexConfig);
		chunks.forEach(chunk -> {
			chunk.getMetadata().putAll(metadata);
		});

		VectorStore vectorStore = vectorStoreFactory.getVectorStoreService().getVectorStore(indexConfig);
		vectorStore.add(chunks);
	}

}
