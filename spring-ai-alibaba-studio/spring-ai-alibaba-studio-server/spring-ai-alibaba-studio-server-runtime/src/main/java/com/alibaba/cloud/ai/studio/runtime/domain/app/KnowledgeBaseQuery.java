package com.alibaba.cloud.ai.studio.runtime.domain.app;

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Query model for knowledge base operations
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeBaseQuery extends BaseQuery {

	/**
	 * List of knowledge base IDs to query
	 */
	@JsonProperty("kb_ids")
	private List<String> kbIds;

}
