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
package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.adapters;

import com.alibaba.cloud.ai.studio.admin.generator.model.App;
import com.alibaba.cloud.ai.studio.admin.generator.model.AppMetadata;
import com.alibaba.cloud.ai.studio.admin.generator.model.agent.Agent;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.Serializer;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.AgentTypeProvider;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent.AgentTypeProviderRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/25 18:31
 */
@Component("agentDSLAdapter")
public class AgentDSLAdapter implements DSLAdapter {

	private final Serializer serializer;

	private final ObjectMapper objectMapper;

	private final AgentTypeProviderRegistry providerRegistry;

	public AgentDSLAdapter(@Qualifier("yaml") Serializer serializer, AgentTypeProviderRegistry providerRegistry) {
		this.serializer = serializer;
		this.objectMapper = new ObjectMapper();
		this.providerRegistry = providerRegistry;
	}

	@Override
	public String exportDSL(App app) {
		if (!(app.getSpec() instanceof Agent agent)) {
			throw new IllegalArgumentException("App spec is not Agent");
		}
		Map<String, Object> body = dumpAgent(agent);
		body.put("name", app.getMetadata().getName());
		body.put("description", app.getMetadata().getDescription());
		body.put("mode", "agent");
		return serializer.dump(body);
	}

	@Override
	public App importDSL(String dsl) {
		Map<String, Object> data = serializer.load(dsl);
		validateDSLData(data);
		Map<String, Object> root = getAgentRoot(data);
		if (root == null)
			root = data;

		// 仅解析壳层字段，handle 原样透传
		AppMetadata metadata = mapToMetadata(data);

		Agent agent = new Agent();
		agent.setAgentClass(asString(firstNonBlank((String) root.get("type"), (String) root.get("agent_class"))));
		agent.setName(asString(root.get("name")));
		agent.setDescription(asString(root.get("description")));
		agent.setInstruction(asString(root.get("instruction")));
		agent.setInputKey(asString(root.get("input_key")));
		if (root.get("input_keys") instanceof List<?> iks) {
			agent.setInputKeys((List<String>) (List<?>) iks);
		}
		agent.setOutputKey(asString(root.get("output_key")));

		// 透传 handle（不感知字段）
		if (root.get("handle") instanceof Map<?, ?> h) {
			agent.setHandle((Map<String, Object>) h);
		}

		// 递归 sub_agents（只解析壳层 + 透传 handle）
		if (root.get("sub_agents") instanceof List<?> children) {
			List<Agent> subs = new java.util.ArrayList<>();
			for (Object o : children) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> childRoot = getAgentRoot((Map<String, Object>) m);
					if (childRoot == null)
						childRoot = (Map<String, Object>) m;
					Agent child = new Agent();
					child.setAgentClass(asString(
							firstNonBlank((String) childRoot.get("type"), (String) childRoot.get("agent_class"))));
					child.setName(asString(childRoot.get("name")));
					child.setDescription(asString(childRoot.get("description")));
					child.setInstruction(asString(childRoot.get("instruction")));
					child.setInputKey(asString(childRoot.get("input_key")));
					if (childRoot.get("input_keys") instanceof List<?> ciks) {
						child.setInputKeys((List<String>) (List<?>) ciks);
					}
					child.setOutputKey(asString(childRoot.get("output_key")));
					if (childRoot.get("handle") instanceof Map<?, ?> ch) {
						child.setHandle((Map<String, Object>) ch);
					}
					subs.add(child);
				}
			}
			agent.setSubAgents(subs);
		}

		return new App(metadata, agent);
	}

	private void validateDSLData(Map<String, Object> dslData) {
		if (dslData == null) {
			throw new IllegalArgumentException("invalid agent dsl: data is null");
		}
		Map<String, Object> root = getAgentRoot(dslData);
		if (root == null) {
			throw new IllegalArgumentException("invalid agent dsl: missing 'agent' object or flat agent fields");
		}
		String type = firstNonBlank((String) root.get("type"), (String) root.get("agent_class"));
		String name = (String) root.get("name");
		if (isBlank(type) || isBlank(name)) {
			throw new IllegalArgumentException("invalid agent dsl: 'type/agent_class' and 'name' are required");
		}
		// 针对不同 Agent 类型的校验
		validateAgentTypeSpecificConstraints(type, root);
	}

	private void validateAgentTypeSpecificConstraints(String type, Map<String, Object> root) {
		if (type == null || root == null) {
			return;
		}

		// 使用 AgentTypeProvider 进行校验
		AgentTypeProvider provider = providerRegistry.get(type);
		if (provider != null) {
			provider.validateDSL(root);
		}
	}

	private AppMetadata mapToMetadata(Map<String, Object> data) {
		Map<String, Object> root = getAgentRoot(data);
		if (root == null)
			root = data;
		AppMetadata metadata = new AppMetadata();
		metadata.setMode(AppMetadata.AGENT_MODE);
		metadata.setId(UUID.randomUUID().toString());
		metadata.setName((String) root.getOrDefault("name", "agent-" + metadata.getId()));
		metadata.setDescription((String) root.getOrDefault("description", ""));
		return metadata;
	}

	private Map<String, Object> metadataToMap(AppMetadata metadata) {
		Map<String, Object> data = new HashMap<>();
		data.put("name", metadata.getName());
		data.put("description", metadata.getDescription());
		data.put("mode", "agent");
		return data;
	}

	@Override
	public Boolean supportDialect(DSLDialectType dialectType) {
		return DSLDialectType.SAA_AGENT.equals(dialectType);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getAgentRoot(Map<String, Object> data) {
		Object a = data.get("agent");
		if (a instanceof Map) {
			return (Map<String, Object>) a;
		}
		// 兼容扁平结构：认为当前 data 自身即 agent
		if (data.containsKey("agent_class") || data.containsKey("type") || data.containsKey("name")) {
			return data;
		}
		return null;
	}

	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private String firstNonBlank(String a, String b) {
		if (!isBlank(a))
			return a;
		if (!isBlank(b))
			return b;
		return null;
	}

	private Integer toInteger(Object v) {
		if (v == null)
			return null;
		if (v instanceof Integer)
			return (Integer) v;
		if (v instanceof Long)
			return ((Long) v).intValue();
		if (v instanceof Number)
			return ((Number) v).intValue();
		if (v instanceof String) {
			try {
				return Integer.parseInt(((String) v).trim());
			}
			catch (Exception ignored) {
			}
		}
		return null;
	}

	private static String asString(Object o) {
		return o == null ? null : String.valueOf(o);
	}

	private Map<String, Object> dumpAgent(Agent agent) {
		Map<String, Object> m = new HashMap<>();

		// 基础属性
		if (agent.getAgentClass() != null) {
			m.put("type", agent.getAgentClass());
		}
		m.put("name", agent.getName());
		m.put("description", agent.getDescription());
		m.put("instruction", agent.getInstruction());
		m.put("input_key", agent.getInputKey());
		if (agent.getInputKeys() != null && !agent.getInputKeys().isEmpty()) {
			m.put("input_keys", agent.getInputKeys());
		}
		m.put("output_key", agent.getOutputKey());

		// LLM
		Map<String, Object> llm = new HashMap<>();
		if (agent.getModel() != null)
			llm.put("model", agent.getModel());
		if (agent.getChatOptions() != null && !agent.getChatOptions().isEmpty())
			llm.put("options", agent.getChatOptions());
		if (!llm.isEmpty())
			m.put("llm", llm);
		if (agent.getInstruction() != null)
			m.put("instruction", agent.getInstruction());
		if (agent.getMaxIterations() != null)
			m.put("max_iterations", agent.getMaxIterations());

		// 工具
		if (agent.getTools() != null)
			m.put("tools", agent.getTools());
		if (agent.getToolConfig() != null && agent.getToolConfig().get("resolver") instanceof String) {
			m.put("resolver", agent.getToolConfig().get("resolver"));
		}

		// hooks/state
		if (agent.getHooks() != null && !agent.getHooks().isEmpty()) {
			m.put("hooks", agent.getHooks());
		}
		if (agent.getStateConfig() != null && !agent.getStateConfig().isEmpty()) {
			Map<String, Object> state = new HashMap<>();
			state.put("strategies", agent.getStateConfig());
			m.put("state", state);
		}

		// 导出 handle（如果存在）
		if (agent.getHandle() != null && !agent.getHandle().isEmpty()) {
			m.put("handle", agent.getHandle());
		}

		// 递归 sub_agents
		if (agent.getSubAgents() != null && !agent.getSubAgents().isEmpty()) {
			List<Map<String, Object>> subs = agent.getSubAgents().stream().map(this::dumpAgent).map(x -> {
				Map<String, Object> item = new HashMap<>();
				item.put("agent", x);
				return item;
			}).collect(Collectors.toList());
			m.put("sub_agents", subs);
		}

		return m;
	}

}
