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

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DeleteChunkRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexDocumentRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.UpdateChunkRequest;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.rag.DocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * Controller for managing document chunks in RAG system. Provides CRUD operations for
 * document chunks and supports batch operations.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "rag_chunk")
@RequestMapping("/console/v1/documents")
public class DocumentChunkController {

	/** Service for handling document operations */
	private final DocumentService documentService;

	public DocumentChunkController(DocumentService documentService) {
		this.documentService = documentService;
	}

	/**
	 * Creates a new document chunk
	 * @param docId Document ID
	 * @param chunk Chunk data
	 * @return Created chunk ID
	 */
	@PostMapping(value = "/{docId}/chunks")
	public Result<String> createDocumentChunk(@PathVariable("docId") String docId, @RequestBody DocumentChunk chunk) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		if (Objects.isNull(chunk.getText())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("text"));
		}

		chunk.setDocId(docId);
		String chunkId = documentService.createDocumentChunk(chunk);
		return Result.success(context.getRequestId(), chunkId);
	}

	/**
	 * Updates an existing document chunk
	 * @param docId Document ID
	 * @param chunkId Chunk ID
	 * @param chunk Updated chunk data
	 */
	@PutMapping("/{docId}/chunks/{chunkId}")
	public Result<Void> updateDocumentChunk(@PathVariable("docId") String docId,
			@PathVariable("chunkId") String chunkId, @RequestBody DocumentChunk chunk) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (StringUtils.isBlank(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		if (StringUtils.isBlank(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("chunkId"));
		}

		if (StringUtils.isBlank(chunk.getText())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("text"));
		}

		chunk.setDocId(docId);
		chunk.setChunkId(chunkId);
		documentService.updateDocumentChunk(chunk);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes a single document chunk
	 * @param docId Document ID
	 * @param chunkId Chunk ID to delete
	 */
	@DeleteMapping("/{docId}/chunks/{chunkId}")
	public Result<Void> deleteDocumentChunk(@PathVariable("docId") String docId,
			@PathVariable("chunkId") String chunkId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (StringUtils.isBlank(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		if (StringUtils.isBlank(chunkId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("chunkId"));
		}

		documentService
			.deleteDocumentChunks(DeleteChunkRequest.builder().docId(docId).chunkIds(List.of(chunkId)).build());
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Batch deletes multiple document chunks
	 * @param docId Document ID
	 * @param request Delete request containing chunk IDs
	 */
	@DeleteMapping("/{docId}/chunks/batch-delete")
	public Result<Void> deleteDocumentChunk(@PathVariable("docId") String docId,
			@RequestBody DeleteChunkRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (StringUtils.isBlank(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		if (CollectionUtils.isEmpty(request.getChunkIds())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("chunkIds"));
		}

		request.setDocId(docId);
		documentService.deleteDocumentChunks(request);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Lists document chunks with pagination
	 * @param docId Document ID
	 * @param query Pagination query parameters
	 * @return Paginated list of document chunks
	 */
	@GetMapping("/{docId}/chunks")
	public Result<PagingList<DocumentChunk>> listDocumentChunks(@PathVariable("docId") String docId,
			@ModelAttribute BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(query)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query"));
		}

		PagingList<DocumentChunk> documents = documentService.listDocumentChunks(docId, query);
		return Result.success(context.getRequestId(), documents);
	}

	/**
	 * Previews document chunks before indexing
	 * @param docId Document ID
	 * @param request Indexing request parameters
	 * @return List of preview chunks
	 */
	@PostMapping("/{docId}/chunks/preview")
	public Result<List<DocumentChunk>> previewDocumentChunks(@PathVariable("docId") String docId,
			@RequestBody IndexDocumentRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		request.setDocId(docId);
		List<DocumentChunk> chunks = documentService.previewDocumentChunks(request);
		return Result.success(context.getRequestId(), chunks);
	}

	/**
	 * Updates the enabled status of document chunks
	 * @param docId Document ID
	 * @param request Request containing chunk IDs and status
	 */
	@PutMapping("/{docId}/chunks/update-status")
	public Result<Void> updateDocumentChunk(@PathVariable("docId") String docId,
			@RequestBody UpdateChunkRequest request) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(docId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("docId"));
		}

		if (CollectionUtils.isEmpty(request.getChunkIds())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("chunkIds"));
		}

		request.setDocId(docId);
		documentService.updateChunkEnabledStatus(request);
		return Result.success(context.getRequestId(), null);
	}

}
