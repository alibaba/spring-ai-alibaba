package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 调试请求
 */
@Data
public class TaskResumeRequest implements Serializable {

	@JsonProperty("app_id")
	private String appId;

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("conversation_id")
	private String conversationId;

	@JsonProperty("resume_node_id")
	private String resumeNodeId;

	@JsonProperty("resume_parent_id")
	private String resumeParentId;

	@JsonProperty("input_params")
	private List<CommonParam> inputParams;

}
