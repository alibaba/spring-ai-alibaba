package com.alibaba.cloud.ai.studio.runtime.domain.app;

import com.alibaba.cloud.ai.studio.runtime.enums.AppStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a version of an application in the workspace
 */
@Data
public class ApplicationVersion implements Serializable {

	/** Workspace identifier */
	@JsonProperty("workspace_id")
	private String workspaceId;

	/** Application identifier */
	@JsonProperty("app_id")
	private String appId;

	/** Current status of the application version */
	private AppStatus status;

	/** Application configuration */
	private String config;

	/** Version number, etc 1, 2, 3 */
	private String version;

	/** Version description */
	private String description;

	/** Creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/** Creator's identifier */
	private String creator;

	/** Last modifier's identifier */
	private String modifier;

}
