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

package com.alibaba.cloud.ai.studio.core.rag;

import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import org.apache.commons.collections.MapUtils;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.*;

/**
 * Utility class for converting between Document and DocumentChunk objects. Provides
 * methods to transform document data between different formats.
 *
 * @since 1.0.0.3
 */
public class DocumentChunkConverter {

	/**
	 * Converts a Spring AI Document to a DocumentChunk. Maps metadata fields and content
	 * from Document to DocumentChunk format.
	 * @param document The source Document object
	 * @return Converted DocumentChunk object
	 */
	public static DocumentChunk toDocumentChunk(Document document) {
		Map<String, Object> metadata = document.getMetadata();
		return DocumentChunk.builder()
			.chunkId(document.getId())
			.docId(MapUtils.getString(metadata, KEY_DOC_ID, null))
			.docName(MapUtils.getString(metadata, KEY_DOC_NAME, null))
			.title(MapUtils.getString(metadata, KEY_TITLE, null))
			.enabled(MapUtils.getBoolean(metadata, KEY_ENABLED, false))
			.text(document.getText())
			.score(document.getScore())
			.pageNumber(MapUtils.getInteger(metadata, "page_number", 0))
			.build();
	}

	/**
	 * Converts a DocumentChunk to a Spring AI Document. Maps fields from DocumentChunk to
	 * Document format with appropriate metadata.
	 * @param chunk The source DocumentChunk object
	 * @return Converted Document object
	 */
	public static Document toDocument(DocumentChunk chunk) {
		Map<String, Object> metadata = new HashMap<>();

		if (chunk.getPageNumber() != null) {
			metadata.put("page_number", chunk.getPageNumber());
		}

		if (chunk.getTitle() != null) {
			metadata.put(KEY_TITLE, chunk.getTitle());
		}

		if (chunk.getDocName() != null) {
			metadata.put(KEY_WORKSPACE_ID, chunk.getWorkspaceId());
		}

		if (chunk.getDocId() != null) {
			metadata.put(KEY_DOC_ID, chunk.getDocId());
		}

		if (chunk.getEnabled() != null) {
			metadata.put(KEY_ENABLED, chunk.getEnabled());
		}

		return org.springframework.ai.document.Document.builder()
			.text(chunk.getText())
			.id(chunk.getChunkId())
			.metadata(metadata)
			.build();
	}

}
