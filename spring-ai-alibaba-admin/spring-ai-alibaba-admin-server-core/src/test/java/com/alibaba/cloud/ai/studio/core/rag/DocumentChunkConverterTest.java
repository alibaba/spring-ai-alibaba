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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.document.Document;
import static org.assertj.core.api.Assertions.assertThat;
import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.*;

class DocumentChunkConverterTest {

	@ParameterizedTest
	@CsvSource(value = { "biology.pdf,lesson-workspace", "biology.pdf,NULL", "NULL,lesson-workspace", "NULL,NULL" },
			nullValues = "NULL")
	void shouldPreserveDocumentNameAndWorkspaceIndependently(String name, String workspace) {
		DocumentChunk chunk = DocumentChunk.builder()
			.chunkId("chunk-one")
			.docId("lesson-one")
			.docName(name)
			.workspaceId(workspace)
			.text("Photosynthesis experiment instructions")
			.title("Experiment")
			.pageNumber(3)
			.enabled(true)
			.build();
		Document document = DocumentChunkConverter.toDocument(chunk);
		assertThat(document.getMetadata().get(KEY_DOC_NAME)).isEqualTo(name);
		assertThat(document.getMetadata().get(KEY_WORKSPACE_ID)).isEqualTo(workspace);
		if (name == null) {
			assertThat(document.getMetadata()).doesNotContainKey(KEY_DOC_NAME);
		}
		if (workspace == null) {
			assertThat(document.getMetadata()).doesNotContainKey(KEY_WORKSPACE_ID);
		}
		DocumentChunk retrieved = DocumentChunkConverter.toDocumentChunk(document);
		assertThat(retrieved.getDocName()).isEqualTo(name);
		assertThat(retrieved.getDocId()).isEqualTo("lesson-one");
		assertThat(retrieved.getChunkId()).isEqualTo("chunk-one");
		assertThat(retrieved.getText()).isEqualTo(chunk.getText());
		assertThat(retrieved.getPageNumber()).isEqualTo(3);
		assertThat(retrieved.getEnabled()).isTrue();
		assertThat(retrieved.getTitle()).isEqualTo("Experiment");
	}

}
