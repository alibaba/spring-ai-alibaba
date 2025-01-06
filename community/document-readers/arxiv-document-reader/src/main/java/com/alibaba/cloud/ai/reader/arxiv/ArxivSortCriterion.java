package com.alibaba.cloud.ai.reader.arxiv;

/**
 * ArxivSortCriterion用于标识可以对搜索结果进行排序的属性
 * 
 * @see <a href="https://arxiv.org/help/api/user-manual#sort">arXiv API User's Manual: sort order</a>
 */
public enum ArxivSortCriterion {
    
    /**
     * 按相关性排序
     */
    RELEVANCE("relevance"),
    
    /**
     * 按最后更新日期排序
     */
    LAST_UPDATED_DATE("lastUpdatedDate"),
    
    /**
     * 按提交日期排序
     */
    SUBMITTED_DATE("submittedDate");

    private final String value;

    ArxivSortCriterion(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
} 