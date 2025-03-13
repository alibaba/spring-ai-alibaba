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
   * 用于控制随机性和多样性的程度。具体来说，temperature值控制了生成文本时对每个候选词的概率分布进行平滑的程度。较高的temperature值会降低概率分布的峰值，使得更多的低概率词被选择，生成结果更加多样化；而较低的temperature值则会增强概率分布的峰值，使得高概率词更容易被选择，生成结果更加确定。
   * 取值范围：[0, 2)，系统默认值0.85。不建议取值为0，无意义。
   */
  private @JsonProperty("temperature") Double temperature;

  /**
   * 生成时使用的随机数种子，用户控制模型生成内容的随机性。seed支持无符号64位整数。在使用seed时，模型将尽可能生成相同或相似的结果，但目前不保证每次生成的结果完全相同。
   */
  private @JsonProperty("seed") Integer seed;

  /**
   * 生成时，核采样方法的概率阈值。例如，取值为0.8时，仅保留累计概率之和大于等于0.8的概率分布中的token，作为随机采样的候选集。取值范围为（0,1.0)，取值越大，生成的随机性越高；取值越低，生成的随机性越低。默认值为0.8。注意，取值不要大于等于1
   */
  private @JsonProperty("top_p") Double topP;

  /**
   * 生成时，采样候选集的大小。例如，取值为50时，仅将单次生成中得分最高的50个token组成随机采样的候选集。取值越大，生成的随机性越高；取值越小，生成的确定性越高。注意：如果top_k参数为空或者top_k的值大于100，表示不启用top_k策略，此时仅有top_p策略生效，默认是空。
   */
  private @JsonProperty("top_k") Integer topK;

  /**
   * <ul>
   *   <li>stop参数用于实现内容生成过程的精确控制，在生成内容即将包含指定的字符串或token_ids时自动停止，生成内容不包含指定的内容。
   *       <p>例如，如果指定stop为"你好"，表示将要生成"你好"时停止；如果指定stop为[37763, 367]，表示将要生成"Observation"时停止。
   *   <li>stop参数支持以list方式传入字符串数组或者token_ids数组，支持使用多个stop的场景。
   * </ul>
   *
   * <q>说明 list模式下不支持字符串和token_ids混用，list模式下元素类型要相同。</q>
   */
  private @JsonProperty("stop") List<Object> stop;

  /**
   * 模型内置了互联网搜索服务，该参数控制模型在生成文本时是否参考使用互联网搜索结果。取值如下：
   *
   * <ul>
   *   <li>true：启用互联网搜索，模型会将搜索结果作为文本生成过程中的参考信息，但模型会基于其内部逻辑"自行判断"是否使用互联网搜索结果。
   *   <li>false（默认）：关闭互联网搜索。
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
   * 控制在流式输出模式下是否开启增量输出，即后续输出内容是否包含已输出的内容。设置为True时，将开启增量输出模式，后面输出不会包含已经输出的内容，您需要自行拼接整体输出；设置为False则会包含已输出的内容。
   */
  private @JsonProperty("incremental_output") Boolean incrementalOutput = true;

  /** 用于控制模型生成时的重复度。提高repetition_penalty时可以降低模型生成的重复度。1.0表示不做惩罚。默认为1.1。 */
  private @JsonProperty("repetition_penalty") Double repetitionPenalty;

  /** 模型可选调用的工具列表。目前仅支持function，并且即使输入多个function，模型仅会选择其中一个生成结果。模型根据tools参数内容可以生产函数调用的参数 */
  private @JsonProperty("tools") List<DashScopeApi.FunctionTool> tools;

  /**
   * 在使用tools参数时，用于控制模型调用指定工具。有三种取值：
   * "none"表示不调用工具。tools参数为空时，默认值为"none"。
   * "auto"表示模型判断是否调用工具，可能调用也可能不调用。tools参数不为空时，默认值为"auto"。
   * object结构可以指定模型调用指定工具。例如tool_choice={"type": "function", "function": {"name": "user_function"}}
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
   * Indicate if the request is multi model
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
