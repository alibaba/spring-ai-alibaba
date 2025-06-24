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

package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convert the LLM node configuration in the Dify DSL to and from the LLMNodeData object.
 */
@Component
public class LLMNodeDataConverter extends AbstractNodeDataConverter<LLMNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.LLM.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<LLMNodeData>> getDialectConverters() {
		return Stream.of(LLMNodeDataConverter.LLMNodeConverter.values())
			.map(LLMNodeDataConverter.LLMNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum LLMNodeConverter {

		DIFY(new DialectConverter<>() {
			@SuppressWarnings("unchecked")
			@Override
			public LLMNodeData parse(Map<String, Object> data) {
				LLMNodeData nd = new LLMNodeData();
				// variable_selector -> inputs
				List<String> sel = (List<String>) data.get("variable_selector");
				if (sel != null && sel.size() == 2) {
					nd.setInputs(Collections.singletonList(new VariableSelector(sel.get(0), sel.get(1))));
				}

				// prompt_template (List of {role,text})
				List<Map<String, Object>> prompts = (List<Map<String, Object>>) data.get("prompt_template");
				if (prompts != null) {
					List<LLMNodeData.PromptTemplate> pt = prompts.stream()
						.map(m -> new LLMNodeData.PromptTemplate((String) m.get("role"), (String) m.get("text")))
						.collect(Collectors.toList());
					nd.setPromptTemplate(pt);
				}

				// model (mode,name,provider,completion_params)
				Map<String, Object> modelMap = (Map<String, Object>) data.get("model");
				if (modelMap != null) {
					LLMNodeData.ModelConfig mc = new LLMNodeData.ModelConfig();
					mc.setMode((String) modelMap.get("mode"))
						.setName((String) modelMap.get("name"))
						.setProvider((String) modelMap.get("provider"));

					Map<String, Object> cpMap = (Map<String, Object>) modelMap.get("completion_params");
					if (cpMap != null) {
						LLMNodeData.CompletionParams cp = new LLMNodeData.CompletionParams();
						if (cpMap.get("max_tokens") != null) {
							cp.setMaxTokens(((Number) cpMap.get("max_tokens")).intValue());
						}
						mc.setCompletionParams(cp);
					}
					nd.setModel(mc);
				}

				// memory_config
				Map<String, Object> memMap = (Map<String, Object>) data.get("memory_config");
				if (memMap != null) {
					LLMNodeData.MemoryConfig mem = new LLMNodeData.MemoryConfig();
					mem.setEnabled((Boolean) memMap.getOrDefault("enabled", false))
						.setWindowSize(((Number) memMap.getOrDefault("window_size", 20)).intValue())
						.setWindowEnabled((Boolean) memMap.getOrDefault("window_enabled", true))
						.setIncludeLastMessage((Boolean) memMap.getOrDefault("include_last_message", false))
						.setLastMessageTemplate((String) memMap.get("last_message_template"));
					nd.setMemoryConfig(mem);
				}

				// system_prompt_template
				nd.setSystemPromptTemplate((String) data.get("system_prompt_template"));

				// user_prompt_template
				nd.setUserPromptTemplate((String) data.get("user_prompt_template"));

				// system_prompt_template_key
				nd.setSystemPromptTemplateKey((String) data.get("system_prompt_template_key"));

				// user_prompt_template_key
				nd.setUserPromptTemplateKey((String) data.get("user_prompt_template_key"));

				// params (Map<String,Object>)
				Map<String, Object> pmap = (Map<String, Object>) data.get("params");
				if (pmap != null) {
					nd.setParams(new LinkedHashMap<>(pmap));
				}

				// params_key
				nd.setParamsKey((String) data.get("params_key"));

				// messages (List of {role,content})
				List<Map<String, Object>> mList = (List<Map<String, Object>>) data.get("messages");
				if (mList != null) {
					List<LLMNodeData.Message> msgObjs = mList.stream()
						.map(m -> new LLMNodeData.Message((String) m.get("role"), (String) m.get("content")))
						.collect(Collectors.toList());
					nd.setMessages(msgObjs);
				}

				// messages_key
				nd.setMessagesKey((String) data.get("messages_key"));

				// advisors (List of {name,prompt})
				List<Map<String, Object>> aList = (List<Map<String, Object>>) data.get("advisors");
				if (aList != null) {
					List<LLMNodeData.Advisor> advObjs = aList.stream()
						.map(m -> new LLMNodeData.Advisor((String) m.get("name"), (String) m.get("prompt")))
						.collect(Collectors.toList());
					nd.setAdvisors(advObjs);
				}

				// tool_callbacks (List of {name,args})
				List<Map<String, Object>> tList = (List<Map<String, Object>>) data.get("tool_callbacks");
				if (tList != null) {
					List<LLMNodeData.ToolCallback> cbObjs = tList.stream()
						.map(m -> new LLMNodeData.ToolCallback((String) m.get("name"),
								(Map<String, Object>) m.get("args")))
						.collect(Collectors.toList());
					nd.setToolCallbacks(cbObjs);
				}

				// output_key
				String nodeId = (String) data.get("id");
				String outputKey = (String) data.getOrDefault("output_key", LLMNodeData.defaultOutputKey(nodeId));
				nd.setOutputKey(outputKey);

				return nd;
			}

			@Override
			public Map<String, Object> dump(LLMNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();

				// variable_selector
				if (nd.getInputs() != null && !nd.getInputs().isEmpty()) {
					VariableSelector vs = nd.getInputs().get(0);
					m.put("variable_selector", List.of(vs.getNamespace(), vs.getName()));
				}

				// prompt_template
				if (nd.getPromptTemplate() != null && !nd.getPromptTemplate().isEmpty()) {
					List<Map<String, Object>> pt = nd.getPromptTemplate().stream().map(t -> {
						Map<String, Object> entry = new LinkedHashMap<>();
						entry.put("role", t.getRole());
						entry.put("text", t.getText());
						return entry;
					}).collect(Collectors.toList());
					m.put("prompt_template", pt);
				}

				// model
				if (nd.getModel() != null) {
					Map<String, Object> mc = new LinkedHashMap<>();
					mc.put("mode", nd.getModel().getMode());
					mc.put("name", nd.getModel().getName());
					mc.put("provider", nd.getModel().getProvider());
					if (nd.getModel().getCompletionParams() != null) {
						Map<String, Object> cpm = new LinkedHashMap<>();
						LLMNodeData.CompletionParams cp = nd.getModel().getCompletionParams();
						if (cp.getMaxTokens() != null) {
							cpm.put("max_tokens", cp.getMaxTokens());
						}
						// … 其他字段同理 …
						mc.put("completion_params", cpm);
					}
					m.put("model", mc);
				}

				// memory_config
				if (nd.getMemoryConfig() != null) {
					Map<String, Object> mm = new LinkedHashMap<>();
					LLMNodeData.MemoryConfig mem = nd.getMemoryConfig();
					mm.put("enabled", mem.getEnabled());
					mm.put("window_size", mem.getWindowSize());
					mm.put("window_enabled", mem.getWindowEnabled());
					mm.put("include_last_message", mem.getIncludeLastMessage());
					mm.put("last_message_template", mem.getLastMessageTemplate());
					m.put("memory_config", mm);
				}

				// system_prompt_template
				if (nd.getSystemPromptTemplate() != null) {
					m.put("system_prompt_template", nd.getSystemPromptTemplate());
				}

				// user_prompt_template
				if (nd.getUserPromptTemplate() != null) {
					m.put("user_prompt_template", nd.getUserPromptTemplate());
				}

				// system_prompt_template_key
				if (nd.getSystemPromptTemplateKey() != null) {
					m.put("system_prompt_template_key", nd.getSystemPromptTemplateKey());
				}

				// user_prompt_template_key
				if (nd.getUserPromptTemplateKey() != null) {
					m.put("user_prompt_template_key", nd.getUserPromptTemplateKey());
				}

				// params
				if (nd.getParams() != null && !nd.getParams().isEmpty()) {
					m.put("params", nd.getParams());
				}

				// params_key
				if (nd.getParamsKey() != null) {
					m.put("params_key", nd.getParamsKey());
				}

				// messages
				if (nd.getMessages() != null && !nd.getMessages().isEmpty()) {
					List<Map<String, Object>> ml = nd.getMessages().stream().map(msg -> {
						Map<String, Object> entry = new LinkedHashMap<>();
						entry.put("role", msg.getRole());
						entry.put("content", msg.getContent());
						return entry;
					}).collect(Collectors.toList());
					m.put("messages", ml);
				}

				// messages_key
				if (nd.getMessagesKey() != null) {
					m.put("messages_key", nd.getMessagesKey());
				}

				// advisors
				if (nd.getAdvisors() != null && !nd.getAdvisors().isEmpty()) {
					List<Map<String, Object>> al = nd.getAdvisors().stream().map(a -> {
						Map<String, Object> entry = new LinkedHashMap<>();
						entry.put("name", a.getName());
						entry.put("prompt", a.getPrompt());
						return entry;
					}).collect(Collectors.toList());
					m.put("advisors", al);
				}

				// tool_callbacks
				if (nd.getToolCallbacks() != null && !nd.getToolCallbacks().isEmpty()) {
					List<Map<String, Object>> tl = nd.getToolCallbacks().stream().map(tc -> {
						Map<String, Object> entry = new LinkedHashMap<>();
						entry.put("name", tc.getName());
						entry.put("args", tc.getArgs());
						return entry;
					}).collect(Collectors.toList());
					m.put("tool_callbacks", tl);
				}

				// output_key
				if (nd.getOutputKey() != null) {
					m.put("output_key", nd.getOutputKey());
				}

				return m;
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		}), CUSTOM(defaultCustomDialectConverter(LLMNodeData.class));

		private final DialectConverter<LLMNodeData> converter;

		LLMNodeConverter(DialectConverter<LLMNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<LLMNodeData> dialectConverter() {
			return converter;
		}

	}

}
