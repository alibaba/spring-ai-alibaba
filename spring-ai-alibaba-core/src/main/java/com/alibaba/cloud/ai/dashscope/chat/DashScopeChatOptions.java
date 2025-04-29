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

import java.util.*;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallingOptions;
import org.springframework.util.Assert;

/**
 * @author nottyjay
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeChatOptions implements FunctionCallingOptions, ChatOptions {

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
   * Tool Function Callbacks to register with the ChatClient. For Prompt Options the
   * functionCallbacks are automatically enabled for the duration of the prompt execution. For
   * Default Options the functionCallbacks are registered but disabled by default. Use the
   * enableFunctions to set the functions from the registry to be used by the ChatClient chat
   * completion requests.
   */
  @JsonIgnore
  private List<FunctionCallback> functionCallbacks = new ArrayList<>();

  /**
   * List of functions, identified by their names, to configure for function calling in the chat
   * completion requests. Functions with those names must exist in the functionCallbacks registry.
   * The {@link #functionCallbacks} from the PromptOptions are automatically enabled for the
   * duration of the prompt execution.
   *
   * <p>Note that function enabled with the default options are enabled for all chat completion
   * requests. This could impact the token count and the billing. If the functions is set in a
   * prompt options, then the enabled functions are only active for the duration of this prompt
   * execution.
   */
  @JsonIgnore private Set<String> functions = new HashSet<>();

  /**
   * Indicates whether the request involves multiple models
   */
  private @JsonProperty("multi_model") Boolean multiModel = false;

  /**
   * If true, the Spring AI will not handle the function calls internally, but will proxy them to the client.
   * It is the client's responsibility to handle the function calls, dispatch them to the appropriate function, and return the results.
   * If false, the Spring AI will handle the function calls internally.
   */
  @JsonIgnore
  private Boolean proxyToolCalls;

  @JsonIgnore
  private Map<String, Object> toolContext;

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
    return null;
  }

  public Integer setMaxTokens() {
    return this.maxTokens;
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
  public Boolean getProxyToolCalls() {
    return this.proxyToolCalls;
  }

  @Override
  public void setProxyToolCalls(Boolean proxyToolCalls) {
    this.proxyToolCalls = proxyToolCalls;
  }

  @Override
  public List<FunctionCallback> getFunctionCallbacks() {
    return this.functionCallbacks;
  }

  @Override
  public void setFunctionCallbacks(List<FunctionCallback> functionCallbacks) {
    this.functionCallbacks = functionCallbacks;
  }

  @Override
  public Set<String> getFunctions() {
    return this.functions;
  }

  @Override
  public void setFunctions(Set<String> functions) {
    this.functions = functions;
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

  public Boolean getMultiModel() {
    return multiModel;
  }

  public void setMultiModel(Boolean multiModel) {
      this.  multiModel = multiModel;
  }

  public static DashscopeChatOptionsBuilder builder() {
    return new DashscopeChatOptionsBuilder();
  }
  @JsonIgnore
  private Boolean interruptible = false;

  public Boolean getInterruptible() {
    return interruptible;
  }

  public void setInterruptible(Boolean interruptible) {
    this.interruptible = interruptible;
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

    public DashscopeChatOptionsBuilder withFunctionCallbacks(
        List<FunctionCallback> functionCallbacks) {
      this.options.functionCallbacks = functionCallbacks;
      return this;
    }

    public DashscopeChatOptionsBuilder withFunction(String functionName) {
      Assert.hasText(functionName, "Function name must not be empty");
      this.options.functions.add(functionName);
      return this;
    }

    public DashscopeChatOptionsBuilder withFunctions(Set<String> functionNames) {
      Assert.notNull(functionNames, "Function names must not be null");
      this.options.functions = functionNames;
      return this;
    }

    public DashscopeChatOptionsBuilder withProxyToolCalls(Boolean proxyToolCalls) {
      this.options.proxyToolCalls = proxyToolCalls;
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

    public DashscopeChatOptionsBuilder withMultiModel(Boolean multiModel) {
      this.options.multiModel = multiModel;
      return this;
    }
    private Boolean interruptible;

    public DashscopeChatOptionsBuilder withInterruptible(Boolean interruptible) {
      this.options.interruptible = interruptible;
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
            .withFunctionCallbacks(fromOptions.getFunctionCallbacks())
            .withFunctions(fromOptions.getFunctions())
            .withRepetitionPenalty(fromOptions.getRepetitionPenalty())
            .withTools(fromOptions.getTools())
            .withToolContext(fromOptions.getToolContext())
            .withMultiModel(fromOptions.getMultiModel())
            .withProxyToolCalls(fromOptions.getProxyToolCalls())
            .withVlHighResolutionImages(fromOptions.getVlHighResolutionImages())
            .withInterruptible(fromOptions.getInterruptible())
            .build();
  }

  @Override
  public boolean equals(Object o) {

	if (this == o) return true;
	if (o == null || getClass() != o.getClass()) return false;
	DashScopeChatOptions that = (DashScopeChatOptions) o;

    return Objects.equals(model, that.model) && Objects.equals(stream, that.stream) && Objects.equals(temperature, that.temperature) && Objects.equals(seed, that.seed) && Objects.equals(topP, that.topP) && Objects.equals(topK, that.topK) && Objects.equals(stop, that.stop) && Objects.equals(enableSearch, that.enableSearch) && Objects.equals(responseFormat, that.responseFormat) && Objects.equals(incrementalOutput, that.incrementalOutput) && Objects.equals(repetitionPenalty, that.repetitionPenalty) && Objects.equals(tools, that.tools) && Objects.equals(toolChoice, that.toolChoice) && Objects.equals(vlHighResolutionImages, that.vlHighResolutionImages) && Objects.equals(functionCallbacks, that.functionCallbacks) && Objects.equals(functions, that.functions) && Objects.equals(multiModel, that.multiModel) && Objects.equals(toolContext, that.toolContext) && Objects.equals(proxyToolCalls, that.proxyToolCalls);
  }

  @Override
  public int hashCode() {

    return Objects.hash(model, stream, temperature, seed, topP, topK, stop, enableSearch, responseFormat, incrementalOutput, repetitionPenalty, tools, toolChoice, vlHighResolutionImages, functionCallbacks, functions, multiModel, toolContext, proxyToolCalls);
  }

  @Override
  public String toString() {

    return "DashScopeChatOptions: " + ModelOptionsUtils.toJsonString(this);
  }

}
