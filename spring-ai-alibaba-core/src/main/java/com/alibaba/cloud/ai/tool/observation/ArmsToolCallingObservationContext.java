package com.alibaba.cloud.ai.tool.observation;

import io.micrometer.observation.Observation.Context;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.util.Assert;

public class ArmsToolCallingObservationContext extends Context {

  private ToolCall toolCall;

  private final String description;

  private final boolean returnDirect;

  private String toolResult;

  public static Builder builder() {
    return new Builder();
  }

  public ToolCall getToolCall() {
    return toolCall;
  }

  public String getDescription() {
    return description;
  }

  public boolean isReturnDirect() {
    return returnDirect;
  }

  public void setToolResult(String toolResult) {
    this.toolResult = toolResult;
  }

  public String getToolResult() {
    return toolResult;
  }

  public ArmsToolCallingObservationContext(ToolCall toolCall, String description, boolean returnDirect) {
    Assert.notNull(toolCall, "toolCall cannot be null");
    this.toolCall = toolCall;
    this.description = description;
    this.returnDirect = returnDirect;
  }

  public static final class Builder {

    private ToolCall toolCall;

    private String description;

    private boolean returnDirect;

    private Builder() {
    }

    public Builder toolCall(ToolCall toolCall) {
      this.toolCall = toolCall;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder returnDirect(boolean returnDirect) {
      this.returnDirect = returnDirect;
      return this;
    }

    public ArmsToolCallingObservationContext build() {
      return new ArmsToolCallingObservationContext(this.toolCall, this.description, this.returnDirect);
    }
  }
}
