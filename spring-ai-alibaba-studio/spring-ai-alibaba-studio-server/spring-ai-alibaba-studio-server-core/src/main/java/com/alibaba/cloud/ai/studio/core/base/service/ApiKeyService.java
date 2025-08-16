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

package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.account.ApiKey;

/**
 * Service interface for managing API keys. Provides operations for creating, updating,
 * deleting, and retrieving API keys.
 *
 * @since 1.0.0.3
 */
public interface ApiKeyService {

	/**
	 * Creates a new API key.
	 * @param apiKey The API key information to create
	 * @return The ID of the created API key
	 */
	Long createApiKey(ApiKey apiKey);

	/**
	 * Updates an existing API key.
	 * @param apiKey The API key information to update
	 */
	void updateApiKey(ApiKey apiKey);

	/**
	 * Deletes an API key by its ID.
	 * @param id The ID of the API key to delete
	 */
	void deleteApiKey(Long id);

	/**
	 * Lists API keys with pagination support.
	 * @param query The query parameters for filtering and pagination
	 * @return A paged list of API keys
	 */
	PagingList<ApiKey> listApiKeys(BaseQuery query);

	/**
	 * Retrieves an API key by its ID.
	 * @param id The ID of the API key to retrieve
	 * @return The API key information
	 */
	ApiKey getApiKey(Long id);

	/**
	 * Retrieves an API key by its key value.
	 * @param apiKey The API key value to look up
	 * @return The API key information
	 */
	ApiKey getApiKey(String apiKey);

}
