package com.alibaba.cloud.ai.studio.runtime.domain.app;

import com.alibaba.cloud.ai.studio.runtime.enums.AppStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Query parameters for application search
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppQuery extends BaseQuery {

	/** Application ID */
	@JsonProperty("app_id")
	private String appId;

	/** Application type */
	@JsonProperty("type")
	private String type;

	/** Application status */
	private AppStatus status;

}
