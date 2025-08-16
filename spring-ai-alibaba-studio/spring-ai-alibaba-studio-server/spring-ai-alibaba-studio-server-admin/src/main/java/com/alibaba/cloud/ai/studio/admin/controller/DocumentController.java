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

package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.CreateDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DeleteDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.Document;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.rag.DocumentService;
import com.alibaba.cloud.ai.studio.admin.annotation.ApiModelAttribute;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * REST controller for managing documents in knowledge bases. Provides CRUD operations and
 * document management functionality.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "rag_document")
@RequestMapping("/console/v1/knowledge-bases")
public class DocumentController {

	/** Service for handling document operations */
	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	/**
	 * Creates new documents in the knowledge base
	 * @param kbId Knowledge base ID
	 * @param request Document creation request
	 * @return Created document IDs
	 */
	@PostMapping(value = "/{kbId}/documents")
	public Result<List<String>> createDocuments(@PathVariable("kbId") String kbId,
			@RequestBody CreateDocumentRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(kbId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("kbId"));
		}

		if (Objects.isNull(request.getType())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("type"));
		}

		if (CollectionUtils.isEmpty(request.getFiles())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("files"));
		}

		request.setKbId(kbId);
		List<String> docIds = documentService.createDocuments(request);
		return Result.success(context.getRequestId(), docIds);
	}

	/**
	 * Updates an existing document
	 * @param kbId Knowledge base ID
	 * @param docId Document ID
	 * @param document Document to update
	 * @return result status
	 */
	@PutMapping("/{kbId}/documents/{docId}")
	public Result<Void> updateDocument(@PathVariable("kbId") String kbId, @PathVariable("docId") String docId,
			@RequestBody Document document) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (StringUtils.isBlank(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		document.setDocId(docId);
		documentService.updateDocument(document);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes a single document
	 * @param kbId Knowledge base ID
	 * @param docId Document ID
	 * @return result status
	 */
	@DeleteMapping("/{kbId}/documents/{docId}")
	public Result<Void> deleteDocument(@PathVariable("kbId") String kbId, @PathVariable("docId") String docId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(kbId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("kbId"));
		}

		if (Objects.isNull(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		documentService.deleteDocuments(DeleteDocumentRequest.builder().kbId(kbId).docIds(List.of(docId)).build());
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes multiple documents in batch
	 * @param kbId Knowledge base ID
	 * @param request Document deletion request
	 * @return result status
	 */
	@DeleteMapping("/{kbId}/documents/batch-delete")
	public Result<Void> batchDeleteDocuments(@PathVariable("kbId") String kbId,
			@RequestBody DeleteDocumentRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(kbId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("kbId"));
		}

		request.setKbId(kbId);
		documentService.deleteDocuments(request);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves a single document by ID
	 * @param kbId Knowledge base ID
	 * @param docId Document ID
	 * @return document info
	 */
	@GetMapping("/{kbId}/documents/{docId}")
	public Result<Document> getDocument(@PathVariable("kbId") String kbId, @PathVariable("docId") String docId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		Document document = documentService.getDocument(docId);
		return Result.success(context.getRequestId(), document);
	}

	/**
	 * Lists documents with pagination
	 * @param kbId Knowledge base ID
	 * @param query Query parameters
	 * @return Paginated list of documents
	 */
	@GetMapping("/{kbId}/documents")
	public Result<PagingList<Document>> listDocuments(@PathVariable("kbId") String kbId,
			@ApiModelAttribute DocumentQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(query)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query"));
		}

		PagingList<Document> documents = documentService.listDocuments(kbId, query);
		return Result.success(context.getRequestId(), documents);
	}

	/**
	 * Re-indexes a document with process and chunking configuration
	 * @param kbId Knowledge base ID
	 * @param docId Document ID
	 * @return result status
	 */
	@PutMapping("/{kbId}/documents/{docId}/re-index")
	public Result<Void> reIndexDocument(@PathVariable("kbId") String kbId, @PathVariable("docId") String docId,
			@RequestBody IndexDocumentRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (StringUtils.isBlank(kbId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("kbId"));
		}

		if (StringUtils.isBlank(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		request.setKbId(kbId);
		request.setDocId(docId);
		documentService.reIndexDocument(request);

		return Result.success(context.getRequestId(), null);
	}

}
