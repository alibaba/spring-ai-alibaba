package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.WorkflowStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * workflow response.
 *
 * @author guning.lt
 * @since 1.0.0-M1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse implements Serializable {

	@JsonProperty("request_id")
	private String requestId;

	@JsonProperty("conversation_id")
	private String conversationId;

	@JsonProperty("task_id")
	private String taskId;

	@JsonProperty("node_id")
	private String nodeId;

	@JsonProperty("node_name")
	private String nodeName;

	@JsonProperty("node_type")
	private String nodeType;

	@JsonProperty("node_status")
	private String nodeStatus;

	@JsonProperty("node_msg_seq_id")
	private Integer nodeMsgSeqId;

	@JsonProperty("node_is_completed")
	private Boolean nodeIsCompleted;

	private WorkflowStatus status;

	private ChatMessage message;

	private Error error;

	@JsonIgnore
	public boolean isSuccess() {
		return error == null;
	}

}
