package com.alibaba.cloud.ai.example.manus.flow;

import org.springframework.ai.chat.messages.Message;
import java.util.List;

/**
 * Tool回调上下文，用于存储工具调用相关的上下文信息
 */
public class ToolCallbackContext {

	private String planId;

	private String toolName;

	private List<Message> messages;

	private String lastResult;

	public ToolCallbackContext(String planId, String toolName) {
		this.planId = planId;
		this.toolName = toolName;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	public String getLastResult() {
		return lastResult;
	}

	public void setLastResult(String lastResult) {
		this.lastResult = lastResult;
	}

}
