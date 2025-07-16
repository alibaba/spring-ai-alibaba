/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dashscope.chat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author nottyjay
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeChatOptions implements ToolCallingChatOptions {

	// @formatter:off
  /** ID of the model to use. */
  @JsonProperty("model")
  private String model;

  @JsonIgnore private Boolean stream;

  /**
   * Used to control the degree of randomness and diversity.
   * Specifically, the temperature value smooths the probability distribution
   * of each candidate token during text generation. Higher temperature values
   * lower the peak of the distribution—allowing more low-probability tokens
   * to be selected and producing more diverse outputs—while lower temperature
   * values increase the peak—making high-probability tokens more likely and
   * resulting in more deterministic outputs.
   * Range: [0, 2), system default: 0.85. Setting to 0 is not recommended.
   */
  private @JsonProperty("temperature") Double temperature;

  /**
   * Random seed for generation, controlled by the user to affect reproducibility.
   * Seed supports unsigned 64‑bit integers. When a seed is provided, the model
   * will attempt to generate identical or similar results, though exact
   * reproducibility is not guaranteed.
   */
  private @JsonProperty("seed") Integer seed;

  /**
   * Nucleus (top-p) sampling threshold during generation. For example, with
   * top_p = 0.8, only tokens whose cumulative probability mass reaches at least
   * 0.8 are retained as candidates for sampling. Range: (0, 1.0), default: 0.8.
   * Higher values increase randomness; lower values increase determinism.
   * Note: do not set >= 1.0.
   */
  private @JsonProperty("top_p") Double topP;

  /**
   * Size of the sampling candidate pool (top-k). For example, top_k = 50 means
   * only the 50 highest-scoring tokens are considered for random sampling.
   * Larger values increase randomness; smaller values increase determinism.
   * Note: if top_k is null or > 100, top-k is disabled and only top-p applies.
   * Default is null (i.e., disabled).
   */
  private @JsonProperty("top_k") Integer topK;

  /**
   * <ul>
   *   <li>The stop parameter is used to precisely control the content generation process. It automatically stops the generation when the content is about to include the specified string or token_ids, and the generated content does not include the specified content.
   *       <p>For example, if stop is set to "Hello", the generation will stop when "Hello" is about to be generated; if stop is set to [37763, 367], the generation will stop when "Observation" is about to be generated.
   *   <li>The stop parameter supports passing in an array of strings or an array of token_ids in list mode, and it supports scenarios where multiple stop conditions are used.
   * </ul>
   *
   * <q>Note: In list mode, strings and token_ids cannot be mixed. The element types in list mode must be the same.</q>
   */
  private @JsonProperty("stop") List<Object> stop;

  /**
   * The model has a built - in internet search service. This parameter controls whether the model refers to and uses internet search results when generating text. The possible values are as follows:
   *
   * <ul>
   *   <li>true: Enable internet search. The model will use the search results as reference information during the text generation process. However, the model will "decide on its own" whether to use the internet search results based on its internal logic.
   *   <li>false (default): Disable internet search.
   * </ul>
   */
  private @JsonProperty("enable_search") Boolean enableSearch = false;

  /**
   * Models can specify the format of the returned content. Valid values: {"type": "text"} or {"type": "json_object"}
   * {@link DashScopeResponseFormat}
   */
  private @JsonProperty("response_format") DashScopeResponseFormat responseFormat;

  /**
   * @param maxTokens The maximum number of tokens to generate in the chat completion.
   * 	 * The total length of input tokens and generated tokens is limited by the model's context length.
   */
  private @JsonProperty("max_tokens") Integer maxTokens;

  /**
   * Controls whether to enable incremental output in streaming output mode, that is, whether the subsequent output content includes the previously output content. When set to true, the incremental output mode will be enabled, and the subsequent output will not include the previously output content. You need to concatenate the overall output yourself. When set to false, the subsequent output will include the previously output content.
   */
  private @JsonProperty("incremental_output") Boolean incrementalOutput = true;

  /**
   * Used to control the repetition degree during model generation. Increasing the repetition_penalty can reduce the repetition degree of the model generation. A value of 1.0 means no penalty. The default value is 1.1.
   */
  private @JsonProperty("repetition_penalty") Double repetitionPenalty;

  /**
   * A list of optional tools that the model can call. Currently, only functions are supported. Even if multiple functions are input, the model will only select one of them to generate results. The model can generate function call parameters based on the content of the tools parameter.
   */
  private @JsonProperty("tools") List<DashScopeApi.FunctionTool> tools;

  /**
   * Strategies for networked search. Takes effect only if the enable_search is true.
   */
  private @JsonProperty("search_options") DashScopeApi.SearchOptions searchOptions;

  /**
   * Whether to enable parallel tool calling。
   */
  private @JsonProperty("parallel_tool_calls") Boolean parallelToolCalls;

  /**
   * Optional HTTP headers to be added to the chat completion request.
   */
  @JsonIgnore
  private Map<String, String> httpHeaders = new HashMap<>();

  /**
   * When using the tools parameter, it is used to control the model to call a specified tool. There are three possible values:
   * "none" indicates not to call any tool. When the tools parameter is empty, the default value is "none".
   * "auto" indicates that the model decides whether to call a tool, which may or may not happen. When the tools parameter is not empty, the default value is "auto".
   * An object structure can specify the model to call a specific tool. For example, tool_choice={"type": "function", "function": {"name": "user_function"}}.
   */
  @JsonProperty("tool_choice")
  private Object toolChoice;

  /**
   * this is to change token limitation to 16384 for vl model, only support for vl models
   * including qwen-vl-max、qwen-vl-max-0809、qwen-vl-plus-0809.
   */
  private @JsonProperty("vl_high_resolution_images") Boolean vlHighResolutionImages;


  /**
   * Whether to enable the thinking process of the model.
   */
  private @JsonProperty("enable_thinking") Boolean enableThinking = false;

  /**
   * Collection of {@link ToolCallback}s to be used for tool calling in the chat completion requests.
   */
  @JsonIgnore
  private List<ToolCallback> toolCallbacks = new ArrayList<>();

  /**
   * Collection of tool names to be resolved at runtime and used for tool calling in the chat completion requests.
   */
  @JsonIgnore
  private Set<String> toolNames = new HashSet<>();

  /**
   * Whether to enable the tool execution lifecycle internally in ChatModel.
   */
  @JsonIgnore
  private Boolean internalToolExecutionEnabled;

  /**
   * Indicates whether the request involves multiple models
   */
  private @JsonProperty("multi_model") Boolean multiModel = false;

  @JsonIgnore
  private Map<String, Object> toolContext = new HashMap<>();;

  @Override
  public String getModel() {
    return model;
  }

  @Override
  public Double getFrequencyPenalty() {
    return null;
  }

  @Override
  public Integer getMaxTokens() {
    return maxTokens;
  }

  public void setMaxTokens(Integer maxTokens) {
    this.maxTokens = maxTokens;
  }

  @Override
  public Double getPresencePenalty() {
    return null;
  }

  @Override
  public List<String> getStopSequences() {
    return null;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public Boolean getStream() {
    return stream;
  }

  public void setStream(Boolean stream) {
    this.stream = stream;
  }

  @Override
  public Double getTemperature() {
    return this.temperature;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public void setSearchOptions(DashScopeApi.SearchOptions searchOptions) {
    this.searchOptions = searchOptions;
  }

  public DashScopeApi.SearchOptions getSearchOptions() {
    return searchOptions;
  }

  public Boolean getParallelToolCalls() {
    return parallelToolCalls;
  }

  public void setParallelToolCalls(Boolean parallelToolCalls) {
      this.parallelToolCalls = parallelToolCalls;
  }

  public void setHttpHeaders(Map<String, String> httpHeaders) {
    this.httpHeaders = httpHeaders;
  }

  public Map<String, String> getHttpHeaders() {
    return httpHeaders;
  }

  @Override
  public Double getTopP() {
    return this.topP;
  }

  @Override
  public ChatOptions copy() {
    return DashScopeChatOptions.fromOptions(this);
  }

  public void setTopP(Double topP) {
    this.topP = topP;
  }

  @Override
  public Integer getTopK() {
    return this.topK;
  }

  public void setTopK(Integer topK) {
    this.topK = topK;
  }

  public List<Object> getStop() {
    return stop;
  }

  public void setStop(List<Object> stop) {
    this.stop = stop;
  }

  public DashScopeResponseFormat getResponseFormat() {

	return responseFormat;
  }

  public void setResponseFormat(DashScopeResponseFormat responseFormat) {

	this.responseFormat = responseFormat;
  }

  public Boolean getEnableSearch() {
    return enableSearch;
  }
  public void setEnableSearch(Boolean enableSearch) {
    this.enableSearch = enableSearch;
  }

  public Double getRepetitionPenalty() {
    return repetitionPenalty;
  }

  public void setRepetitionPenalty(Double repetitionPenalty) {
    this.repetitionPenalty = repetitionPenalty;
  }

  public List<DashScopeApi.FunctionTool> getTools() {
    return tools;
  }

  public void setTools(List<DashScopeApi.FunctionTool> tools) {
    this.tools = tools;
  }

  public Object getToolChoice() {
    return toolChoice;
  }

  public void setToolChoice(Object toolChoice) {
    this.toolChoice = toolChoice;
  }

  public Integer getSeed() {
    return seed;
  }

  public void setSeed(Integer seed) {
    this.seed = seed;
  }

  @Override
  @JsonIgnore
  public List<ToolCallback> getToolCallbacks() {
    return this.toolCallbacks;
  }

  @Override
  @JsonIgnore
  public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
    Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
    Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
    this.toolCallbacks = toolCallbacks;
  }

  @Override
  @JsonIgnore
  public Set<String> getToolNames() {
    return this.toolNames;
  }

  @Override
  @JsonIgnore
  public void setToolNames(Set<String> toolNames) {
    Assert.notNull(toolNames, "toolNames cannot be null");
    Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
    toolNames.forEach(tool -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
    this.toolNames = toolNames;
  }

  @Override
  @JsonIgnore
  public Boolean getInternalToolExecutionEnabled() {
    return this.internalToolExecutionEnabled;
  }

  @Override
  @JsonIgnore
  public void setInternalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
    this.internalToolExecutionEnabled = internalToolExecutionEnabled;
  }

  @Override
  public Map<String,Object> getToolContext() {
    return this.toolContext;
  }

  @Override
  public void setToolContext(Map<String,Object> toolContext) {
    this.toolContext = toolContext;
  }

  public Boolean getIncrementalOutput() {
    return incrementalOutput;
  }

  public void setIncrementalOutput(Boolean incrementalOutput) {
    this.incrementalOutput = incrementalOutput;
  }

  public Boolean getVlHighResolutionImages() {
    return vlHighResolutionImages;
  }

  public void setVlHighResolutionImages(Boolean vlHighResolutionImages) {
      this.vlHighResolutionImages = vlHighResolutionImages;
  }

  public Boolean getEnableThinking() {
    return enableThinking;
  }

  public void setEnableThinking(Boolean enableThinking) {
    this.enableThinking = enableThinking;
  }

  public Boolean getMultiModel() {
    return multiModel;
  }

  public void setMultiModel(Boolean multiModel) {
      this.  multiModel = multiModel;
  }

  public static DashscopeChatOptionsBuilder builder() {
    return new DashscopeChatOptionsBuilder();
  }

  public static class DashscopeChatOptionsBuilder {

    private DashScopeChatOptions options;

    public DashscopeChatOptionsBuilder() {
      this.options = new DashScopeChatOptions();
    }

    public DashscopeChatOptionsBuilder withModel(String model) {
      this.options.model = model;
      return this;
    }

    public DashscopeChatOptionsBuilder withSearchOptions(DashScopeApi.SearchOptions searchOptions) {
      this.options.searchOptions = searchOptions;
      return this;
    }

    public DashscopeChatOptionsBuilder withParallelToolCalls(Boolean parallelToolCalls) {
      this.options.parallelToolCalls = parallelToolCalls;
      return this;
    }

    public DashscopeChatOptionsBuilder withHttpHeaders(Map<String, String> httpHeaders) {
      this.options.httpHeaders = httpHeaders;
      return this;
    }

    public DashscopeChatOptionsBuilder withMaxToken(Integer maxTokens) {
      this.options.maxTokens = maxTokens;
      return this;
    }

    public DashscopeChatOptionsBuilder withTemperature(Double temperature) {
      this.options.temperature = temperature;
      return this;
    }

    public DashscopeChatOptionsBuilder withTopP(Double topP) {
      this.options.topP = topP;
      return this;
    }

    public DashscopeChatOptionsBuilder withTopK(Integer topK) {
      this.options.topK = topK;
      return this;
    }

    public DashscopeChatOptionsBuilder withStop(List<Object> stop) {
      this.options.stop = stop;
      return this;
    }

    public DashscopeChatOptionsBuilder withResponseFormat(DashScopeResponseFormat responseFormat) {
      this.options.responseFormat = responseFormat;
      return this;
    }

    public DashscopeChatOptionsBuilder withEnableSearch(Boolean enableSearch) {
      this.options.enableSearch = enableSearch;
      return this;
    }

    public DashscopeChatOptionsBuilder withRepetitionPenalty(Double repetitionPenalty) {
      this.options.repetitionPenalty = repetitionPenalty;
      return this;
    }

    public DashscopeChatOptionsBuilder withTools(List<DashScopeApi.FunctionTool> tools) {
      this.options.tools = tools;
      return this;
    }

    public DashscopeChatOptionsBuilder withToolChoice(Object toolChoice) {
      this.options.toolChoice = toolChoice;
      return this;
    }

    public DashscopeChatOptionsBuilder withStream(Boolean stream) {
      this.options.stream = stream;
      return this;
    }

    public DashscopeChatOptionsBuilder withToolCallbacks(
            List<ToolCallback> toolCallbacks) {
      Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
      Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
      this.options.toolCallbacks = toolCallbacks;
      return this;
    }

    public DashscopeChatOptionsBuilder withToolNames(Set<String> toolNames) {
      Assert.notNull(toolNames, "toolNames cannot be null");
      Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
      toolNames.forEach(tool -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
      this.options.toolNames = toolNames;
      return this;
    }

    public DashscopeChatOptionsBuilder withToolName(String toolName) {
      Assert.hasText(toolName, "Tool name must not be empty");
      this.options.toolNames.add(toolName);
      return this;
    }

    public DashscopeChatOptionsBuilder withInternalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
      this.options.internalToolExecutionEnabled = internalToolExecutionEnabled;
      return this;
    }

    public DashscopeChatOptionsBuilder withSeed(Integer seed) {
      this.options.seed = seed;
      return this;
    }

    public DashscopeChatOptionsBuilder withIncrementalOutput(Boolean incrementalOutput) {
      this.options.incrementalOutput = incrementalOutput;
      return this;
    }

    public DashscopeChatOptionsBuilder withToolContext(Map<String, Object> toolContext) {
      if (this.options.toolContext == null) {
        this.options.toolContext = toolContext;
      }
      else {
        this.options.toolContext.putAll(toolContext);
      }
      return this;
    }

    public DashscopeChatOptionsBuilder withVlHighResolutionImages(Boolean vlHighResolutionImages) {
      this.options.vlHighResolutionImages = vlHighResolutionImages;
      return this;
    }

    public DashscopeChatOptionsBuilder withEnableThinking(Boolean enableThinking) {
      this.options.enableThinking = enableThinking;
      return this;
    }

    public DashscopeChatOptionsBuilder withMultiModel(Boolean multiModel) {
      this.options.multiModel = multiModel;
      return this;
    }

    public DashScopeChatOptions build() {
      return this.options;
    }
  }

  public static DashScopeChatOptions fromOptions(DashScopeChatOptions fromOptions){

    return DashScopeChatOptions.builder()
            .withModel(fromOptions.getModel())
            .withTemperature(fromOptions.getTemperature())
            .withMaxToken(fromOptions.getMaxTokens())
            .withTopP(fromOptions.getTopP())
            .withTopK(fromOptions.getTopK())
            .withSeed(fromOptions.getSeed())
            .withStop(fromOptions.getStop())
            .withResponseFormat(fromOptions.getResponseFormat())
            .withStream(fromOptions.getStream())
            .withEnableSearch(fromOptions.enableSearch)
            .withIncrementalOutput(fromOptions.getIncrementalOutput())
            .withToolCallbacks(fromOptions.getToolCallbacks())
            .withToolNames(fromOptions.getToolNames())
            .withInternalToolExecutionEnabled(fromOptions.getInternalToolExecutionEnabled())
            .withRepetitionPenalty(fromOptions.getRepetitionPenalty())
            .withTools(fromOptions.getTools())
            .withToolContext(fromOptions.getToolContext())
            .withMultiModel(fromOptions.getMultiModel())
            .withVlHighResolutionImages(fromOptions.getVlHighResolutionImages())
            .withEnableThinking(fromOptions.getEnableThinking())
            .withParallelToolCalls(fromOptions.getParallelToolCalls())
            .withSearchOptions(fromOptions.getSearchOptions())
            .withHttpHeaders(fromOptions.getHttpHeaders())
            .build();
  }

  @Override
  public boolean equals(Object o) {

	if (this == o) return true;
	if (o == null || getClass() != o.getClass()) return false;
	DashScopeChatOptions that = (DashScopeChatOptions) o;

    return Objects.equals(model, that.model) &&
            Objects.equals(stream, that.stream) &&
            Objects.equals(temperature, that.temperature) &&
            Objects.equals(seed, that.seed) &&
            Objects.equals(topP, that.topP) &&
            Objects.equals(topK, that.topK) &&
            Objects.equals(stop, that.stop) &&
            Objects.equals(enableSearch, that.enableSearch) &&
            Objects.equals(responseFormat, that.responseFormat) &&
            Objects.equals(incrementalOutput, that.incrementalOutput) &&
            Objects.equals(repetitionPenalty, that.repetitionPenalty) &&
            Objects.equals(tools, that.tools) &&
            Objects.equals(toolChoice, that.toolChoice) &&
            Objects.equals(vlHighResolutionImages, that.vlHighResolutionImages) &&
            Objects.equals(enableThinking, that.enableThinking) &&
            Objects.equals(toolCallbacks, that.toolCallbacks) &&
            Objects.equals(toolNames, that.toolNames) &&
            Objects.equals(internalToolExecutionEnabled, that.internalToolExecutionEnabled) &&
            Objects.equals(multiModel, that.multiModel) &&
            Objects.equals(searchOptions, that.searchOptions) &&
            Objects.equals(parallelToolCalls, that.parallelToolCalls) &&
            Objects.equals(httpHeaders, that.httpHeaders) &&
            Objects.equals(toolContext, that.toolContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(model, stream, temperature, seed, topP, topK, stop, enableSearch,
            responseFormat, incrementalOutput, repetitionPenalty, tools, toolChoice,
            vlHighResolutionImages, enableThinking, toolCallbacks, toolNames,
            internalToolExecutionEnabled, multiModel, searchOptions, parallelToolCalls, httpHeaders, toolContext);
  }

  @Override
  public String toString() {

    return "DashScopeChatOptions: " + ModelOptionsUtils.toJsonString(this);
  }

}
