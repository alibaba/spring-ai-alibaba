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

package com.alibaba.cloud.ai.studio.core.rag.impl;

import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.CreateDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DeleteChunkRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DeleteDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.UpdateChunkRequest;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.CommonStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.DocumentIndexStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.file.UploadPolicy;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.entity.DocumentEntity;
import com.alibaba.cloud.ai.studio.core.base.mapper.DocumentMapper;
import com.alibaba.cloud.ai.studio.core.base.mq.MqMessage;
import com.alibaba.cloud.ai.studio.core.base.mq.MqProducerManager;
import com.alibaba.cloud.ai.studio.core.config.MqConfigProperties;
import com.alibaba.cloud.ai.studio.core.rag.DocumentService;
import com.alibaba.cloud.ai.studio.core.rag.KnowledgeBaseService;
import com.alibaba.cloud.ai.studio.core.rag.RagConstants;
import com.alibaba.cloud.ai.studio.core.rag.indices.IndexPipeline;
import com.alibaba.cloud.ai.studio.core.rag.vectorstore.VectorStoreFactory;
import com.alibaba.cloud.ai.studio.core.utils.common.BeanCopierUtils;
import com.alibaba.cloud.ai.studio.core.rag.DocumentChunkConverter;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of document service for managing documents in RAG (Retrieval-Augmented
 * Generation) system. Handles document CRUD operations, indexing, and chunk management.
 *
 * @since 1.0.0.3
 */

@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, DocumentEntity> implements DocumentService {

	/** Message queue producer manager for handling async operations */
	private final MqProducerManager mqProducerManager;

	/** Message queue configuration properties */
	private final MqConfigProperties mqConfigProperties;

	/** Service for managing knowledge bases */
	private final KnowledgeBaseService knowledgeBaseService;

	/** Factory for creating vector stores */
	private final VectorStoreFactory vectorStoreFactory;

	/** Pipeline for processing and indexing documents */
	private final IndexPipeline knowledgeBaseIndexPipeline;

	/** Producer for document indexing messages */
	@Qualifier("documentIndexProducer")
	private final Producer documentIndexProducer;

	public DocumentServiceImpl(MqProducerManager mqProducerManager, MqConfigProperties mqConfigProperties,
			KnowledgeBaseService knowledgeBaseService, VectorStoreFactory vectorStoreFactory,
			IndexPipeline knowledgeBaseIndexPipeline,
			@Qualifier("documentIndexProducer") Producer documentIndexProducer) {
		this.mqProducerManager = mqProducerManager;
		this.mqConfigProperties = mqConfigProperties;
		this.knowledgeBaseService = knowledgeBaseService;
		this.vectorStoreFactory = vectorStoreFactory;
		this.knowledgeBaseIndexPipeline = knowledgeBaseIndexPipeline;
		this.documentIndexProducer = documentIndexProducer;
	}

	/**
	 * Creates new documents in the system
	 * @param request Document creation request containing file information
	 * @return List of created document IDs
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public List<String> createDocuments(CreateDocumentRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		// add docs to db
		List<Document> documents = buildDocuments(request);
		List<DocumentEntity> entities = new ArrayList<>();
		List<String> docIds = new ArrayList<>();
		for (Document document : documents) {
			DocumentEntity entity = BeanCopierUtils.copy(document, DocumentEntity.class);
			String docId = IdGenerator.idStr();
			entity.setDocId(docId);
			entity.setWorkspaceId(workspaceId);
			entity.setEnabled(true);
			entity.setIndexStatus(DocumentIndexStatus.UPLOADED);
			entity.setStatus(CommonStatus.NORMAL);

			if (document.getMetadata() != null) {
				entity.setMetadata(JsonUtils.toJson(document.getMetadata()));
			}

			if (document.getProcessConfig() != null) {
				entity.setProcessConfig(JsonUtils.toJson(document.getProcessConfig()));
			}

			entity.setGmtCreate(new Date());
			entity.setGmtModified(new Date());
			entity.setCreator(context.getAccountId());
			entity.setModifier(context.getAccountId());

			entities.add(entity);

			docIds.add(docId);
			document.setKbId(document.getKbId());
			document.setDocId(docId);
		}

		this.saveBatch(entities);

		// update kb docs count
		KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(request.getKbId());
		knowledgeBase.setTotalDocs(knowledgeBase.getTotalDocs() + entities.size());
		knowledgeBaseService.updateKnowledgeBase(knowledgeBase);

		// send mq message for doc index
		List<MqMessage> messages = new ArrayList<>();
		for (Document document : documents) {
			messages.add(MqMessage.builder()
				.topic(mqConfigProperties.getDocumentIndexTopic())
				.tag("doc")
				.keys(List.of(document.getDocId()))
				.body(JsonUtils.toJson(document))
				.build());
		}

		mqProducerManager.sendAsync(documentIndexProducer, messages,
				sendResult -> LogUtils.info("send document mq message, messageId: {}", sendResult.getMessageId()),
				e -> LogUtils.error("Failed to send document mq message", e));

		return docIds;
	}

	/**
	 * Retrieves a document by its ID
	 * @param docId Document ID
	 * @return Document information
	 */
	@Override
	public Document getDocument(String docId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();
		DocumentEntity entity = getDocumentById(workspaceId, docId);
		if (entity == null) {
			throw new BizException(ErrorCode.DOCUMENT_NOT_FOUND.toError());
		}

		return toDocumentDTO(entity);
	}

	/**
	 * Lists documents in a knowledge base with pagination
	 * @param kbId Knowledge base ID
	 * @param query Query parameters including pagination and filters
	 * @return Paginated list of documents
	 */
	@Override
	public PagingList<Document> listDocuments(String kbId, DocumentQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		Page<DocumentEntity> page = new Page<>(query.getCurrent(), query.getSize());
		LambdaQueryWrapper<DocumentEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(DocumentEntity::getWorkspaceId, workspaceId).eq(DocumentEntity::getKbId, kbId);
		if (StringUtils.isNotBlank(query.getName())) {
			queryWrapper.like(DocumentEntity::getName, query.getName());
		}
		if (query.getIndexStatus() != null) {
			queryWrapper.eq(DocumentEntity::getIndexStatus, query.getIndexStatus().getStatus());
		}
		queryWrapper.ne(DocumentEntity::getStatus, CommonStatus.DELETED.getStatus());
		queryWrapper.orderByDesc(DocumentEntity::getId);

		IPage<DocumentEntity> pageResult = this.page(page, queryWrapper);

		List<Document> documents;
		if (CollectionUtils.isEmpty(pageResult.getRecords())) {
			documents = new ArrayList<>();
		}
		else {
			documents = pageResult.getRecords().stream().map(this::toDocumentDTO).toList();
		}

		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), documents);
	}

	/**
	 * Updates document information
	 * @param document Document to update
	 */
	@Override
	public void updateDocument(Document document) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		DocumentEntity entity = getDocumentById(workspaceId, document.getDocId());
		if (entity == null) {
			throw new BizException(ErrorCode.DOCUMENT_NOT_FOUND.toError());
		}

		entity.setName(document.getName());
		entity.setModifier(context.getAccountId());
		entity.setGmtModified(new Date());

		this.updateById(entity);
	}

	/**
	 * Updates document enabled status
	 * @param docId Document ID
	 * @param enabled New enabled status
	 */
	@Override
	public void updateDocumentEnabledStatus(String docId, Boolean enabled) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context == null ? null : context.getWorkspaceId();

		LambdaUpdateWrapper<DocumentEntity> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(DocumentEntity::getDocId, docId)
			.eq(workspaceId != null, DocumentEntity::getWorkspaceId, workspaceId)
			.set(DocumentEntity::getEnabled, enabled);

		this.update(updateWrapper);

		// TODO update all document chunks status
	}

	/**
	 * Updates document indexing status
	 * @param docId Document ID
	 * @param indexStatus New indexing status
	 */
	@Override
	public void updateDocumentIndexStatus(String docId, DocumentIndexStatus indexStatus) {
		LambdaUpdateWrapper<DocumentEntity> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(DocumentEntity::getDocId, docId).set(DocumentEntity::getIndexStatus, indexStatus.getStatus());

		this.update(updateWrapper);
	}

	/**
	 * Deletes documents from the system
	 * @param request Delete request containing document IDs
	 */
	@Override
	public void deleteDocuments(DeleteDocumentRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		if (CollectionUtils.isEmpty(request.getDocIds())) {
			return;
		}

		// delete chunks
		deleteChunksByDocId(request.getKbId(), request.getDocIds());

		// delete from db
		LambdaUpdateWrapper<DocumentEntity> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(DocumentEntity::getWorkspaceId, workspaceId);
		if (request.getDocIds().size() == 1) {
			updateWrapper.eq(DocumentEntity::getDocId, request.getDocIds().get(0));
		}
		else {
			updateWrapper.in(DocumentEntity::getDocId, request.getDocIds());
		}
		updateWrapper.set(DocumentEntity::getStatus, CommonStatus.DELETED.getStatus());
		updateWrapper.set(DocumentEntity::getGmtModified, new Date());
		updateWrapper.set(DocumentEntity::getModifier, context.getAccountId());
		this.update(updateWrapper);

		// update kb docs count
		KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(request.getKbId());

		long count = knowledgeBase.getTotalDocs() - request.getDocIds().size();
		count = Math.max(0, count);
		knowledgeBase.setTotalDocs(count);
		knowledgeBaseService.updateKnowledgeBase(knowledgeBase);
	}

	/**
	 * Gets the knowledge base associated with a document
	 * @param docId Document ID
	 * @return Knowledge base information
	 */
	@Override
	public KnowledgeBase getKnowledgeBase(String docId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		DocumentEntity entity = getDocumentById(context.getWorkspaceId(), docId);
		if (entity == null) {
			throw new BizException(ErrorCode.DOCUMENT_NOT_FOUND.toError());
		}

		return knowledgeBaseService.getKnowledgeBase(entity.getKbId());
	}

	/**
	 * Creates a new document chunk
	 * @param chunk Chunk information
	 * @return Created chunk ID
	 */
	@Override
	public String createDocumentChunk(DocumentChunk chunk) {
		RequestContext context = RequestContextHolder.getRequestContext();
		DocumentEntity entity = getDocumentById(context.getWorkspaceId(), chunk.getDocId());

		KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(entity.getKbId());
		VectorStore vectorStore = vectorStoreFactory.getVectorStoreService()
			.getVectorStore(knowledgeBase.getIndexConfig());

		String id = IdGenerator.uuid();
		chunk.setChunkId(id);
		chunk.setEnabled(true);
		chunk.setDocId(entity.getDocId());
		chunk.setDocName(entity.getName());
		chunk.setWorkspaceId(context.getWorkspaceId());

		vectorStore.add(List.of(DocumentChunkConverter.toDocument(chunk)));
		return id;
	}

	/**
	 * Updates an existing document chunk
	 * @param chunk Chunk information to update
	 */
	@Override
	public void updateDocumentChunk(DocumentChunk chunk) {
		RequestContext context = RequestContextHolder.getRequestContext();
		DocumentEntity entity = getDocumentById(context.getWorkspaceId(), chunk.getDocId());

		KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(entity.getKbId());
		vectorStoreFactory.getVectorStoreService().updateDocumentChunks(knowledgeBase.getIndexConfig(), List.of(chunk));
	}

	/**
	 * Deletes document chunks
	 * @param request Delete request containing chunk IDs
	 */
	@Override
	public void deleteDocumentChunks(DeleteChunkRequest request) {
		KnowledgeBase knowledgeBase = getKnowledgeBase(request.getDocId());
		VectorStore vectorStore = vectorStoreFactory.getVectorStoreService()
			.getVectorStore(knowledgeBase.getIndexConfig());
		vectorStore.delete(request.getChunkIds());
	}

	/**
	 * Deletes all chunks associated with specified documents
	 * @param kbId Knowledge base ID
	 * @param docIds List of document IDs
	 */
	@Override
	public void deleteChunksByDocId(String kbId, List<String> docIds) {
		KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(kbId);
		VectorStore vectorStore = vectorStoreFactory.getVectorStoreService()
			.getVectorStore(knowledgeBase.getIndexConfig());

		var b = new FilterExpressionBuilder();
		var exp = b
			.and(b.eq(RagConstants.KEY_WORKSPACE_ID, knowledgeBase.getWorkspaceId()),
					b.eq(RagConstants.KEY_DOC_ID, docIds))
			.build();
		vectorStore.delete(exp);
	}

	/**
	 * Lists document chunks with pagination
	 * @param docId Document ID
	 * @param query Query parameters including pagination
	 * @return Paginated list of document chunks
	 */
	@Override
	public PagingList<DocumentChunk> listDocumentChunks(String docId, BaseQuery query) {
		KnowledgeBase knowledgeBase = getKnowledgeBase(docId);

		var b = new FilterExpressionBuilder();
		var exp = b
			.and(b.eq(RagConstants.KEY_WORKSPACE_ID, knowledgeBase.getWorkspaceId()),
					b.eq(RagConstants.KEY_DOC_ID, docId))
			.build();

		int current = query.getCurrent() <= 0 ? 1 : query.getCurrent();
		int size = query.getSize() <= 0 ? 10 : query.getSize();

		return vectorStoreFactory.getVectorStoreService()
			.listDocumentChunks(knowledgeBase.getIndexConfig(),
					SearchRequest.builder().from((current - 1) * size).topK(size).filterExpression(exp).build());
	}

	/**
	 * Previews document chunks before indexing
	 * @param request Index request containing document information
	 * @return List of preview chunks
	 */
	@Override
	public List<DocumentChunk> previewDocumentChunks(IndexDocumentRequest request) {
		String docId = request.getDocId();
		Document document = getDocument(docId);

		// 1. parse pdf
		List<org.springframework.ai.document.Document> parsedDocuments = knowledgeBaseIndexPipeline.parse(document);

		// 2. split chunks
		List<org.springframework.ai.document.Document> chunks = knowledgeBaseIndexPipeline.transform(parsedDocuments,
				request.getProcessConfig());

		if (CollectionUtils.isEmpty(chunks)) {
			return List.of();
		}

		return chunks.stream().map(DocumentChunkConverter::toDocumentChunk).toList();
	}

	/**
	 * Updates chunk enabled status
	 * @param request Update request containing chunk IDs and new status
	 */
	@Override
	public void updateChunkEnabledStatus(UpdateChunkRequest request) {
		KnowledgeBase knowledgeBase = getKnowledgeBase(request.getDocId());
		vectorStoreFactory.getVectorStoreService()
			.updateDocumentChunkStatus(knowledgeBase.getIndexConfig(), request.getChunkIds(), request.getEnabled());
	}

	/**
	 * Re-indexes a document
	 * @param request Index request containing document information
	 */
	@Override
	public void reIndexDocument(IndexDocumentRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String workspaceId = context.getWorkspaceId();

		// delete all chunks first
		KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(request.getKbId());
		deleteChunksByDocId(knowledgeBase.getKbId(), List.of(request.getDocId()));

		// update doc status
		DocumentEntity entity = getDocumentById(workspaceId, request.getDocId());
		entity.setIndexStatus(DocumentIndexStatus.UPLOADED);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		if (request.getProcessConfig() != null) {
			entity.setProcessConfig(JsonUtils.toJson(request.getProcessConfig()));
		}
		this.updateById(entity);

		// send mq message for doc index
		Document document = toDocumentDTO(entity);
		List<MqMessage> messages = List.of(MqMessage.builder()
			.topic(mqConfigProperties.getDocumentIndexTopic())
			.tag("doc")
			.keys(List.of(document.getDocId()))
			.body(JsonUtils.toJson(document))
			.build());

		mqProducerManager.sendAsync(documentIndexProducer, messages,
				sendResult -> LogUtils.info("send document mq message, messageId: {}", sendResult.getMessageId()),
				e -> LogUtils.error("Failed to send document mq message", e));
	}

	/**
	 * Retrieves a document entity by ID
	 * @param workspaceId Workspace ID
	 * @param docId Document ID
	 * @return Document entity or null if not found
	 */
	private DocumentEntity getDocumentById(String workspaceId, String docId) {
		LambdaQueryWrapper<DocumentEntity> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(DocumentEntity::getDocId, docId)
			.eq(DocumentEntity::getWorkspaceId, workspaceId)
			.ne(DocumentEntity::getStatus, CommonStatus.DELETED.getStatus());

		Optional<DocumentEntity> entityOptional = this.getOneOpt(queryWrapper);
		return entityOptional.orElse(null);
	}

	/**
	 * Converts document entity to DTO
	 * @param entity Document entity
	 * @return Document DTO
	 */
	private Document toDocumentDTO(DocumentEntity entity) {
		if (entity == null) {
			return null;
		}

		Document document = BeanCopierUtils.copy(entity, Document.class);
		if (entity.getMetadata() != null) {
			document.setMetadata(JsonUtils.fromJson(entity.getMetadata(), Document.Metadata.class));
		}

		return document;
	}

	/**
	 * Builds document objects from upload request
	 * @param request Document creation request
	 * @return List of document objects
	 */
	@NotNull
	private static List<Document> buildDocuments(CreateDocumentRequest request) {
		List<Document> documents = new ArrayList<>();
		for (UploadPolicy file : request.getFiles()) {
			Document document = Document.builder()
				.kbId(request.getKbId())
				.type(request.getType())
				.name(file.getName())
				.path(file.getPath())
				.format(file.getExtension())
				.size(file.getSize())
				.metadata(Document.Metadata.builder().contentType(file.getContentType()).build())
				.build();

			documents.add(document);
		}
		return documents;
	}

}
