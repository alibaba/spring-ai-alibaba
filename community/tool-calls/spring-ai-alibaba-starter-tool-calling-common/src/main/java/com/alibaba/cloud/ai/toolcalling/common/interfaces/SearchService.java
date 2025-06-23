/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.toolcalling.common.interfaces;

import java.util.List;

/**
 * Define an abstract SearchService interface that must be implemented by all search
 * plugins to provide basic query capabilities. Additionally, each plugin must fully
 * implement the Function interface to support its unique search functionality. Typically,
 * users can directly import a specific search plugin's starter for standard use. However,
 * when dynamic switching between multiple search plugins is required, this abstract
 * interface serves as the unified invocation point.
 *
 * @author vlsmb
 */
public interface SearchService {

	/**
	 * Each search plugin's implementation class must implement the core simple query
	 * functionality in the method.
	 */
	Response query(String query);

	/**
	 * Each plugin's Request class must implement this interface.
	 */
	interface Request {

		String getQuery();

	}

	/**
	 * Each plugin's Response class must implement this interface.
	 */
	interface Response {

		SearchResult getSearchResult();

	}

	record SearchResult(List<SearchContent> results) {

	}

	record SearchContent(String title, String content, String url) {

	}

}
