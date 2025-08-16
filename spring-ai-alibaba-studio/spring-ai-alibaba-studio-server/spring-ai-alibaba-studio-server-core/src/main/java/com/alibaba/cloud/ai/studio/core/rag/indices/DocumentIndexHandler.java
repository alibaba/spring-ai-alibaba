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

import com.alibaba.cloud.ai.studio.runtime.enums.DocumentIndexStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.ProcessConfig;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.mq.MqConsumerHandler;
import com.alibaba.cloud.ai.studio.core.base.mq.MqConsumerManager;
import com.alibaba.cloud.ai.studio.core.base.mq.MqMessage;
import com.alibaba.cloud.ai.studio.core.config.MqConfigProperties;
import com.alibaba.cloud.ai.studio.core.rag.DocumentService;
import com.alibaba.cloud.ai.studio.core.rag.KnowledgeBaseService;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.*;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Handler for document indexing operations. Processes documents through parsing,
 * chunking, and vector storage.
 *
 * @since 1.0.0.3
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexHandler implements MqConsumerHandler<MqMessage> {

	/** Message queue configuration properties */
	private final MqConfigProperties mqConfigProperties;

	/** Message queue consumer manager */
	private final MqConsumerManager mqConsumerManager;

	/** Service for knowledge base operations */
	private final KnowledgeBaseService knowledgeBaseService;

	/** Service for document operations */
	private final DocumentService documentService;

	/** Pipeline for knowledge base indexing operations */
	private final KnowledgeBaseIndexPipeline knowledgeBaseIndexPipeline;

	/**
	 * Initialize the handler by subscribing to the document index topic
	 */
	@PostConstruct
	public void init() {
		mqConsumerManager.subscribe(mqConfigProperties.getDocumentIndexGroup(),
				mqConfigProperties.getDocumentIndexTopic(), this);
	}

	/**
	 * Handle incoming message by processing the document
	 * @param message The message containing document data
	 */
	@Override
	public void handle(MqMessage message) {
		LogUtils.monitor("DocumentIndexHandler", "handle", System.currentTimeMillis(), SUCCESS, message, null);

		Document document = JsonUtils.fromJson(message.getBody(), Document.class);
		process(document);
	}

	/**
	 * Process a document through the indexing pipeline: 1. Parse the document 2. Split
	 * into chunks 3. Create embeddings and store in vector store
	 * @param document The document to process
	 */
	private void process(Document document) {
		long start = System.currentTimeMillis();
		DocumentIndexStatus status;
		try {
			documentService.updateDocumentIndexStatus(document.getDocId(), DocumentIndexStatus.PROCESSING);

			KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(document.getKbId());

			// Parse document
			List<org.springframework.ai.document.Document> parsedDocuments = knowledgeBaseIndexPipeline.parse(document);

			// Split into chunks
			ProcessConfig processConfig = document.getProcessConfig();
			if (processConfig == null) {
				processConfig = knowledgeBase.getProcessConfig();
			}
			List<org.springframework.ai.document.Document> chunks = knowledgeBaseIndexPipeline
				.transform(parsedDocuments, processConfig);

			// Create embeddings and store in vector store
			Map<String, Object> metadata = Map.of(KEY_WORKSPACE_ID, knowledgeBase.getWorkspaceId(), KEY_DOC_ID,
					document.getDocId(), KEY_ENABLED, document.getEnabled(), KEY_DOC_NAME, document.getName());

			knowledgeBaseIndexPipeline.store(chunks, knowledgeBase.getIndexConfig(), metadata);

			status = DocumentIndexStatus.PROCESSED;
			LogUtils.monitor("DocumentIndexHandler", "process", start, SUCCESS, document, null);
		}
		catch (Exception e) {
			status = DocumentIndexStatus.FAILED;
			LogUtils.monitor("DocumentIndexHandler", "process", start, FAIL, document, e.getMessage(), e);
		}

		documentService.updateDocumentIndexStatus(document.getDocId(), status);
	}

}
