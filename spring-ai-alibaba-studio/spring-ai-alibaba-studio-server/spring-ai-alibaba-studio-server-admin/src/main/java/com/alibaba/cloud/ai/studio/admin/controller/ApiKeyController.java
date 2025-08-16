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
import com.alibaba.cloud.ai.studio.runtime.domain.account.ApiKey;
import com.alibaba.cloud.ai.studio.core.base.service.ApiKeyService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Controller for managing API keys. Provides CRUD operations for API key management.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "apikey")
@RequestMapping("/console/v1/api-keys")
public class ApiKeyController {

	/** Service for handling API key operations */
	private final ApiKeyService apiKeyService;

	public ApiKeyController(ApiKeyService apiKeyService) {
		this.apiKeyService = apiKeyService;
	}

	/**
	 * Creates a new API key
	 * @param apiKey API key information
	 * @return Result containing the created API key ID
	 */
	@PostMapping()
	public Result<String> createApiKey(@RequestBody ApiKey apiKey) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(apiKey)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("apiKey"));
		}

		if (StringUtils.isBlank(apiKey.getDescription())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("description"));
		}

		Long id = apiKeyService.createApiKey(apiKey);
		return Result.success(context.getRequestId(), String.valueOf(id));
	}

	/**
	 * Updates an existing API key
	 * @param id API key ID
	 * @param apiKey Updated API key information
	 * @return Result indicating success
	 */
	@PutMapping("/{id}")
	public Result<String> updateApiKey(@PathVariable("id") Long id, @RequestBody ApiKey apiKey) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(id)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("id"));
		}

		if (StringUtils.isBlank(apiKey.getDescription())) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("description"));
		}

		apiKey.setId(id);
		apiKeyService.updateApiKey(apiKey);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Deletes an API key
	 * @param id API key ID to delete
	 * @return Result indicating success
	 */
	@DeleteMapping("/{id}")
	public Result<Void> deleteApiKey(@PathVariable("id") Long id) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(id)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("id"));
		}

		apiKeyService.deleteApiKey(id);
		return Result.success(context.getRequestId(), null);
	}

	/**
	 * Retrieves a specific API key
	 * @param id API key ID to retrieve
	 * @return Result containing the API key information
	 */
	@GetMapping("/{id}")
	public Result<ApiKey> getApiKey(@PathVariable("id") Long id) {
		RequestContext context = RequestContextHolder.getRequestContext();

		if (Objects.isNull(id)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("id"));
		}

		ApiKey apiKey = apiKeyService.getApiKey(id);
		return Result.success(context.getRequestId(), apiKey);
	}

	/**
	 * Lists API keys with pagination
	 * @param query Query parameters for pagination
	 * @return Result containing paginated list of API keys
	 */
	@GetMapping()
	public Result<PagingList<ApiKey>> listApiKeys(@ModelAttribute BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();

		PagingList<ApiKey> apiKeys = apiKeyService.listApiKeys(query);
		return Result.success(context.getRequestId(), apiKeys);
	}

}
