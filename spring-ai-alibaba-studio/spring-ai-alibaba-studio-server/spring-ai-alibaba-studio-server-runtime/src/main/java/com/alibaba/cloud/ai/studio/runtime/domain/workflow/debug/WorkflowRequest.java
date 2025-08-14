package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * workflow request.
 *
 * @author guning.lt
 * @since 1.0.0-M1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowRequest implements Serializable {

	/**
	 * ID of the app.
	 */
	@JsonProperty("app_id")
	private String appId;

	@JsonProperty("conversation_id")
	private String conversationId;

	@JsonProperty("request_id")
	private String requestId;

	@JsonProperty("messages")
	private List<ChatMessage> messages;

	@JsonProperty("stream")
	private Boolean stream = false;

	@JsonProperty("draft")
	private Boolean draft = false;

	@JsonProperty("input_params")
	private List<TaskRunParam> inputParams;

}
