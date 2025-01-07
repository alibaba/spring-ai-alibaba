package com.alibaba.cloud.ai.reader.arxiv.client;

/**
 * ArxivSortOrder指示根据指定的ArxivSortCriterion对搜索结果进行排序的顺序
 *
 * @see <a href="https://arxiv.org/help/api/user-manual#sort">arXiv API User's Manual:
 * sort order</a>
 * @author brianxiadong
 */
public enum ArxivSortOrder {

	/**
	 * 升序排序
	 */
	ASCENDING("ascending"),

	/**
	 * 降序排序
	 */
	DESCENDING("descending");

	private final String value;

	ArxivSortOrder(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}