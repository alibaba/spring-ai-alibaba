package com.alibaba.cloud.ai.reader.arxiv.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * arXiv数据库搜索的规范
 *
 * @see <a href="https://arxiv.org/help/api/user-manual#query_details">arXiv API User's
 * Manual: Details of Query Construction</a>
 * @author brianxiadong
 */
public class ArxivSearch {

	private String query; // 查询字符串

	private List<String> idList; // 限制搜索的文章ID列表

	private Integer maxResults; // 最大返回结果数

	private ArxivSortCriterion sortBy; // 排序标准

	private ArxivSortOrder sortOrder; // 排序顺序

	public ArxivSearch() {
		this("", new ArrayList<>(), null, ArxivSortCriterion.RELEVANCE, ArxivSortOrder.DESCENDING);
	}

	public ArxivSearch(String query, List<String> idList, Integer maxResults, ArxivSortCriterion sortBy,
			ArxivSortOrder sortOrder) {
		this.query = query;
		this.idList = idList;
		this.maxResults = maxResults;
		this.sortBy = sortBy;
		this.sortOrder = sortOrder;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public List<String> getIdList() {
		return idList;
	}

	public void setIdList(List<String> idList) {
		this.idList = idList;
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

	public ArxivSortCriterion getSortBy() {
		return sortBy;
	}

	public void setSortBy(ArxivSortCriterion sortBy) {
		this.sortBy = sortBy;
	}

	public ArxivSortOrder getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(ArxivSortOrder sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * 返回用于API请求的搜索参数
	 */
	public Map<String, String> getUrlArgs() {
		Map<String, String> args = new HashMap<>();
		args.put("search_query", query);
		args.put("id_list", String.join(",", idList));
		args.put("sortBy", sortBy.getValue());
		args.put("sortOrder", sortOrder.getValue());
		return args;
	}

}