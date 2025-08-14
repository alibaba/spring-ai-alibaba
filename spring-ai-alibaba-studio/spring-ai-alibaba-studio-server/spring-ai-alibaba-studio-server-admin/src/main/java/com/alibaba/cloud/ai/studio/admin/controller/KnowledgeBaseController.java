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
import com.alibaba.cloud.ai.studio.runtime.domain.app.KnowledgeBaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentRetrieverQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.base.manager.DocumentRetrieverManager;
import com.alibaba.cloud.ai.studio.core.rag.KnowledgeBaseService;
import com.alibaba.cloud.ai.studio.admin.annotation.ApiModelAttribute;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.rag.Query;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * Controller for managing knowledge bases and document retrieval operations. Provides
 * REST endpoints for CRUD operations on knowledge bases and document retrieval.
 *
 * @since 1.0.0.3
 */
@RestController()
@Tag(name = "rag_knowledge")
@RequestMapping("/console/v1/knowledge-bases")
public class KnowledgeBaseController {

	/** Service for managing knowledge base operations */
	private final KnowledgeBaseService knowledgeBaseService;

	/** Manager for handling document retrieval operations */
	private final DocumentRetrieverManager documentRetrieverManager;

	public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
			DocumentRetrieverManager documentRetrieverManager) {
		this.knowledgeBaseService = knowledgeBaseService;
		this.documentRetrieverManager = documentRetrieverManager;
	}

	/**
	 * Creates a new knowledge base
	 * @param kb Knowledge base configuration
	 * @return Result containing the created knowledge base ID
	 */
	@PostMapping()
	public Result<String> createKnowledgeBase(@RequestBody KnowledgeBase kb) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(kb)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("knowledge_base"));
		}

		if (StringUtils.isBlank(kb.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		if (kb.getProcessConfig() == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("process_config"));
		}

		if (kb.getIndexConfig() == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("index_config"));
		}

		IndexConfig indexConfig = kb.getIndexConfig();
		if (StringUtils.isBlank(indexConfig.getEmbeddingProvider())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("embedding_provider"));
		}

		if (StringUtils.isBlank(indexConfig.getEmbeddingModel())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("embedding_model"));
		}

		String kbId = knowledgeBaseService.createKnowledgeBase(kb);
		return Result.success(context.getRequestId(), kbId);
	}

	/**
	 * Updates an existing knowledge base
	 * @param kbId ID of the knowledge base to update
	 * @param kb Updated knowledge base configuration
	 * @return Result indicating success
	 */
	@PutMapping("/{kbId}")
	public Result<String> updateKnowledgeBase(@PathVariable("kbId") String kbId, @RequestBody KnowledgeBase kb) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(kb)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("kb_id"));
		}

		if (StringUtils.isBlank(kbId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("knowledge base id"));
		}

		if (StringUtils.isBlank(kb.getName())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("name"));
		}

		kb.setKbId(kbId);
		knowledgeBaseService.updateKnowledgeBase(kb);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes a knowledge base
	 * @param kbId ID of the knowledge base to delete
	 * @return Result indicating success
	 */
	@DeleteMapping("/{kbId}")
	public Result<Void> deleteKnowledgeBase(@PathVariable("kbId") String kbId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(kbId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("knowledge base id"));
		}

		knowledgeBaseService.deleteKnowledgeBase(kbId);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves a knowledge base by ID
	 * @param kbId ID of the knowledge base to retrieve
	 * @return Result containing the knowledge base details
	 */
	@GetMapping("/{kbId}")
	public Result<KnowledgeBase> getKnowledgeBase(@PathVariable("kbId") String kbId) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(kbId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("knowledge base id"));
		}

		KnowledgeBase kb = knowledgeBaseService.getKnowledgeBase(kbId);
		return Result.success(context.getRequestId(), kb);
	}

	/**
	 * Lists knowledge bases with pagination
	 * @param query Query parameters for pagination
	 * @return Result containing paginated list of knowledge bases
	 */
	@GetMapping()
	public Result<PagingList<KnowledgeBase>> listKnowledgeBases(@ApiModelAttribute BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(query)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query"));
		}

		PagingList<KnowledgeBase> kbs = knowledgeBaseService.listKnowledgeBases(query);
		return Result.success(context.getRequestId(), kbs);
	}

	/**
	 * Retrieves knowledge bases by their IDs
	 * @param query Query containing list of knowledge base IDs
	 * @return Result containing list of knowledge bases
	 */
	@PostMapping("/query-by-codes")
	public Result<List<KnowledgeBase>> queryKnowledgeBasesByCodes(@RequestBody KnowledgeBaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(query) || CollectionUtils.isEmpty(query.getKbIds())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query or kb_ids"));
		}

		List<KnowledgeBase> knowledgeBases = knowledgeBaseService.listKnowledgeBases(query.getKbIds());
		return Result.success(context.getRequestId(), knowledgeBases);
	}

	/**
	 * Retrieves relevant document chunks based on query
	 * @param query Document retrieval query with search options
	 * @return Result containing list of relevant document chunks
	 */
	@PostMapping("/retrieve")
	public Result<List<DocumentChunk>> retrieve(@RequestBody DocumentRetrieverQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(query) || StringUtils.isBlank(query.getQuery())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("query"));
		}

		if (Objects.isNull(query.getSearchOptions()) || Objects.isNull(query.getSearchOptions().getKbIds())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("kbIds"));
		}

		if (query.getSearchOptions().getKbIds().size() == 1) {
			String kbId = query.getSearchOptions().getKbIds().get(0);
			KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(kbId);
			query.getSearchOptions().setTopK(knowledgeBase.getSearchConfig().getTopK());
		}

		query.getSearchOptions().setEnableSearch(true);
		List<DocumentChunk> documentChunks = documentRetrieverManager
			.retrieve(Query.builder().text(query.getQuery()).build(), query.getSearchOptions());
		return Result.success(context.getRequestId(), documentChunks);
	}

}
