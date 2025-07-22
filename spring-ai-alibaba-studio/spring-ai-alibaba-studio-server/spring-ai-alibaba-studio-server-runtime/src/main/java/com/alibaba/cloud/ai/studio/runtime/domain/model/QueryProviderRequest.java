package com.alibaba.cloud.ai.studio.runtime.domain.model;

import lombok.Data;

/**
 * Request for querying provider information
 */
@Data
public class QueryProviderRequest {

	/**
	 * Provider name
	 */
	private String name;

}
