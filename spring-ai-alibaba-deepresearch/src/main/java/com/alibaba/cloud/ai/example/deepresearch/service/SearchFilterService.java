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

package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.example.deepresearch.util.SearchBeanUtil;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Sort the SearchService's returned results based on user-configured website blocklists
 * and allowlists.
 *
 * @author vlsmb
 * @since 2025/7/10
 */
public abstract class SearchFilterService {

	private final SearchBeanUtil searchBeanUtil;

	private static final Logger log = LoggerFactory.getLogger(SearchFilterService.class);

	public SearchFilterService(SearchBeanUtil searchBeanUtil) {
		this.searchBeanUtil = searchBeanUtil;
	}

	/**
	 * Retrieve the blocklist and allowlist for search result websites from configuration
	 * files, databases, or other sources.
	 * @return Map Object - Key: domain string, Value: weight score in [-1.0, 1.0]
	 * Positive weights indicate trust (1.0 = maximum trust) Negative weights indicate
	 * distrust (-1.0 = maximum distrust)
	 */
	protected abstract Map<String, Double> loadWebsiteWeight();

	private String extractDomain(String url) throws IllegalArgumentException, URISyntaxException {
		if (!StringUtils.hasText(url)) {
			throw new IllegalArgumentException("url must not be empty");
		}
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "http://" + url;
		}
		URI uri = new URI(url);
		String host = uri.getHost();
		if (!StringUtils.hasText(host)) {
			log.debug("url {}'s host is empty", url);
			throw new IllegalArgumentException("host must not be empty");
		}
		return host;
	}

	/**
	 * Sort the SearchService's returned results. To optimize for AI model processing,
	 * position search results with high trust weights at both ends of the message list,
	 * while placing items with lower weights toward the center.
	 * @param isEnabled Whether to reorder search results based on trust weights (default:
	 * true). If set to false, all returned weight fields become 0.0.
	 * @param result SearchService.SearchResult
	 * @return sort results
	 */
	public List<SearchContentWithWeight> sortSearchResult(Boolean isEnabled, SearchService.SearchResult result) {
		if (isEnabled != null && !isEnabled) {
			return result.results().stream().map(item -> new SearchContentWithWeight(item, 0.0)).toList();
		}
		Map<String, Double> weight = this.loadWebsiteWeight();
		List<SearchContentWithWeight> sortedResult = result.results().stream().map(item -> {
			try {
				String host = this.extractDomain(item.url());
				// Domains with unknown weights default to a neutral value of 0.
				return new SearchContentWithWeight(item, weight.getOrDefault(host, 0.0));
			}
			catch (Exception e) {
				log.debug("Exception occurred when extract domain: ", e);
				return new SearchContentWithWeight(item, 0.0);
			}
		}).sorted(Comparator.comparingDouble(SearchContentWithWeight::weight).reversed()).toList();
		// Reorganization List
		List<SearchContentWithWeight> reorderedResult = new ArrayList<>();
		List<SearchContentWithWeight> rightResult = new ArrayList<>();
		for (int index = 0; index < sortedResult.size(); index++) {
			if (index % 2 == 0) {
				reorderedResult.add(sortedResult.get(index));
			}
			else {
				rightResult.add(sortedResult.get(index));
			}
		}
		Collections.reverse(rightResult);
		return Stream.concat(reorderedResult.stream(), rightResult.stream()).toList();
	}

	/**
	 * Filter the SearchService's returned results by removing entries with weight values
	 * below 0. To optimize for AI model processing, position search results with high
	 * trust weights at both ends of the message list, while placing items with lower
	 * weights toward the center.
	 * @param isEnabled Whether to reorder search results based on trust weights (default:
	 * true). If set to false, all returned weight fields become 0.0.
	 * @param result SearchService.SearchResult
	 * @return filter results
	 */
	public List<SearchContentWithWeight> filterSearchResult(Boolean isEnabled, SearchService.SearchResult result) {
		return this.sortSearchResult(isEnabled, result).stream().filter(i -> i.weight() >= 0).toList();
	}

	/**
	 * Execute query and sort result.
	 * @param isEnabled Whether to reorder search results based on trust weights (default:
	 * true). If set to false, all returned weight fields become 0.0.
	 * @param searchEnum searchEnum
	 * @param query query
	 * @return result
	 */
	public List<SearchContentWithWeight> queryAndSort(Boolean isEnabled, SearchEnum searchEnum, String query) {
		Optional<SearchService> searchService = searchBeanUtil.getSearchService(searchEnum);
		if (searchService.isEmpty()) {
			return List.of();
		}
		return this.sortSearchResult(isEnabled, searchService.get().query(query).getSearchResult());
	}

	/**
	 * Execute query and filter result.
	 * @param isEnabled Whether to reorder search results based on trust weights (default:
	 * true). If set to false, all returned weight fields become 0.0.
	 * @param searchEnum searchEnum
	 * @param query query
	 * @return result
	 */
	public List<SearchContentWithWeight> queryAndFilter(Boolean isEnabled, SearchEnum searchEnum, String query) {
		Optional<SearchService> searchService = searchBeanUtil.getSearchService(searchEnum);
		if (searchService.isEmpty()) {
			return List.of();
		}
		return this.filterSearchResult(isEnabled, searchService.get().query(query).getSearchResult());
	}

	public record SearchContentWithWeight(SearchService.SearchContent content, Double weight) {

	}

}
