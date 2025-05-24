package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.util.StringUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP Node: 调用 MCP Server
 */
public class McpNode implements NodeAction {

  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

  private final String url;
  private final String tool;
  private final Map<String, String> headers;
  private final Map<String, Object> params;
  private final String outputKey;

  private McpNode(Builder builder) {
    this.url = builder.url;
    this.tool = builder.tool;
    this.headers = builder.headers;
    this.params = builder.params;
    this.outputKey = builder.outputKey;
  }

  @Override
  public Map<String, Object> apply(OverAllState state) throws Exception {
    // 变量替换
    String finalUrl = replaceVariables(url, state);
    String finalTool = replaceVariables(tool, state);
    Map<String, String> finalHeaders = replaceVariables(headers, state);
    Map<String, Object> finalParams = replaceVariablesObj(params, state);

    // 构建 transport（用 builder + customizeRequest 设置 header）
    HttpClientSseClientTransport.Builder builder = HttpClientSseClientTransport.builder(finalUrl);
    if (finalHeaders != null && !finalHeaders.isEmpty()) {
      builder.customizeRequest(req -> finalHeaders.forEach(req::header));
    }
    HttpClientSseClientTransport transport = builder.build();

    // 构建同步客户端
    McpSyncClient client = McpClient.sync(transport).build();

    // 调用 MCP 工具
    Object result;
    try {
      McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(finalTool, finalParams);
      result = client.callTool(request);
    } catch (Exception e) {
      throw new McpNodeException("MCP 调用失败: " + e.getMessage(), e);
    }

    // 结果处理
    Map<String, Object> updatedState = new HashMap<>();
    updatedState.put("mcp_result", result);
    if (StringUtils.hasLength(this.outputKey)) {
      updatedState.put(this.outputKey, result);
    }
    return updatedState;
  }

  private String replaceVariables(String template, OverAllState state) {
    if (template == null)
      return null;
    Matcher matcher = VARIABLE_PATTERN.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1);
      Object value = state.value(key).orElse("");
      matcher.appendReplacement(result, value.toString());
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private Map<String, String> replaceVariables(Map<String, String> map, OverAllState state) {
    if (map == null)
      return null;
    Map<String, String> result = new HashMap<>();
    map.forEach((k, v) -> result.put(k, replaceVariables(v, state)));
    return result;
  }

  private Map<String, Object> replaceVariablesObj(Map<String, Object> map, OverAllState state) {
    if (map == null)
      return null;
    Map<String, Object> result = new HashMap<>();
    map.forEach((k, v) -> {
      if (v instanceof String) {
        result.put(k, replaceVariables((String) v, state));
      } else {
        result.put(k, v);
      }
    });
    return result;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String url;
    private String tool;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, Object> params = new HashMap<>();
    private String outputKey;

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder tool(String tool) {
      this.tool = tool;
      return this;
    }

    public Builder header(String name, String value) {
      this.headers.put(name, value);
      return this;
    }

    public Builder param(String name, Object value) {
      this.params.put(name, value);
      return this;
    }

    public Builder outputKey(String outputKey) {
      this.outputKey = outputKey;
      return this;
    }

    public McpNode build() {
      return new McpNode(this);
    }
  }

  public static class McpNodeException extends RuntimeException {
    public McpNodeException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}