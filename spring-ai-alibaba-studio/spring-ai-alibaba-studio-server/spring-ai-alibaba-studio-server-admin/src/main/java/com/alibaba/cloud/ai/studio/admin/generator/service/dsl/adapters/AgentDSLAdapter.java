package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.adapters;

import com.alibaba.cloud.ai.studio.admin.generator.model.App;
import com.alibaba.cloud.ai.studio.admin.generator.model.AppMetadata;
import com.alibaba.cloud.ai.studio.admin.generator.model.agent.Agent;
import com.alibaba.cloud.ai.studio.admin.generator.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Workflow;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.Serializer;
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
@Component
public class AgentDSLAdapter extends AbstractDSLAdapter {

    private final Serializer serializer;
    private final ObjectMapper objectMapper;

    public AgentDSLAdapter(@Qualifier("yaml") Serializer serializer) {
        this.serializer = serializer;
        this.objectMapper = new ObjectMapper();
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
        if (root == null) root = data;

        AppMetadata metadata = mapToMetadata(data);
        Agent agent = parseAgent(root);
        return new App(metadata, agent);
    }

    @Override
    public void validateDSLData(Map<String, Object> dslData) {
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
        // 针对 parallel 的基本校验（与 ParallelAgent 约束对齐）
        if ("parallel".equalsIgnoreCase(type)) {
            Object subs = root.get("sub_agents");
            if (!(subs instanceof List)) {
                throw new IllegalArgumentException("Parallel agent requires 'sub_agents' (array)");
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subAgents = (List<Map<String, Object>>) subs;
            int size = subAgents.size();
            if (size < 2 || size > 10) {
                throw new IllegalArgumentException("Parallel agent requires 2-10 sub_agents, got: " + size);
            }
            // 校验子 agent 的 output_key 唯一
            Set<String> keys = new HashSet<>();
            Set<String> dup = new HashSet<>();
            for (Map<String, Object> sa : subAgents) {
                Map<String, Object> sar = getAgentRoot(sa);
                if (sar == null) sar = sa;
                String ok = (String) sar.get("output_key");
                if (ok != null && !keys.add(ok)) {
                    dup.add(ok);
                }
            }
            if (!dup.isEmpty()) {
                throw new IllegalArgumentException("Duplicate output keys among sub_agents: " + dup);
            }
        }
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public AppMetadata mapToMetadata(Map<String, Object> data) {
        Map<String, Object> root = getAgentRoot(data);
        if (root == null) root = data;
        AppMetadata metadata = new AppMetadata();
        metadata.setMode(AppMetadata.AGENT_MODE);
        metadata.setId(UUID.randomUUID().toString());
        metadata.setName((String) root.getOrDefault("name", "agent-" + metadata.getId()));
        metadata.setDescription((String) root.getOrDefault("description", ""));
        return metadata;
    }

    @Override
    public Map<String, Object> metadataToMap(AppMetadata metadata) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", metadata.getName());
        data.put("description", metadata.getDescription());
        data.put("mode", "agent");
        return data;
    }

    // 实现其他必要的抽象方法（返回null或空实现，因为Agent模式不需要这些）
    @Override
    public Workflow mapToWorkflow(Map<String, Object> data) {
        return null;
    }

    @Override
    public Map<String, Object> workflowToMap(Workflow workflow) {
        return new HashMap<>();
    }

    @Override
    public ChatBot mapToChatBot(Map<String, Object> data) {
        return null;
    }

    @Override
    public Map<String, Object> chatbotToMap(ChatBot chatbot) {
        return new HashMap<>();
    }

    @Override
    public Boolean supportDialect(DSLDialectType dialectType) {
        return DSLDialectType.AGENT.equals(dialectType);
    }

    // ----------------- helpers -----------------

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
        if (!isBlank(a)) return a;
        if (!isBlank(b)) return b;
        return null;
    }

    private Integer toInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Long) return ((Long) v).intValue();
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt(((String) v).trim()); } catch (Exception ignored) {}
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Agent parseAgent(Map<String, Object> root) {
        Agent agent = new Agent();

        // 基础属性
        String type = firstNonBlank((String) root.get("type"), (String) root.get("agent_class"));
        if (type != null) {
            agent.setAgentClass(type.toLowerCase(Locale.ROOT));
        }
        agent.setName((String) root.get("name"));
        agent.setDescription((String) root.get("description"));
        agent.setOutputKey((String) root.get("output_key"));
        agent.setInputKey((String) root.get("input_key"));

        // LLM 与推理
        // 兼容 llm: { model, options } 与顶层 model/chat_options
        Object llmObj = root.get("llm");
        if (llmObj instanceof Map) {
            Map<String, Object> llm = (Map<String, Object>) llmObj;
            if (llm.get("model") instanceof String) {
                agent.setModel((String) llm.get("model"));
            }
            if (llm.get("options") instanceof Map) {
                agent.setChatOptions((Map<String, Object>) llm.get("options"));
            }
            // chat_client_bean 先透传进 chatOptions，供生成器判定优先级
            if (llm.get("chat_client_bean") instanceof String) {
                Map<String, Object> opts = Optional.ofNullable(agent.getChatOptions()).orElseGet(HashMap::new);
                opts.put("chat_client_bean", llm.get("chat_client_bean"));
                agent.setChatOptions(opts);
            }
        } else {
            if (root.get("model") instanceof String) agent.setModel((String) root.get("model"));
            if (root.get("chat_options") instanceof Map) agent.setChatOptions((Map<String, Object>) root.get("chat_options"));
        }
        agent.setInstruction((String) root.get("instruction"));
        Integer maxIter = toInteger(root.get("max_iterations"));
        if (maxIter != null) agent.setMaxIterations(maxIter);

        // 工具
        if (root.get("tools") instanceof List) {
            agent.setTools(((List<?>) root.get("tools")).stream().map(String::valueOf).collect(Collectors.toList()));
        }
        // resolver 也记录在 toolConfig，供后续决定优先级（resolver 优先于 tools）
        if (root.get("resolver") instanceof String) {
            Map<String, Object> tc = Optional.ofNullable(agent.getToolConfig()).orElseGet(HashMap::new);
            tc.put("resolver", root.get("resolver"));
            agent.setToolConfig(tc);
        }

        // 并行/流程配置（统一沉淀到 flowConfig）
        Map<String, Object> flowCfg = Optional.ofNullable(agent.getFlowConfig()).orElseGet(HashMap::new);
        if ("parallel".equalsIgnoreCase(agent.getAgentClass())) {
            flowCfg.put("type", "parallel");
            if (root.get("merge") instanceof Map) {
                Map<String, Object> merge = (Map<String, Object>) root.get("merge");
                Object strategy = merge.get("strategy");
                if (strategy instanceof String) {
                    flowCfg.put("merge_strategy", ((String) strategy).toLowerCase(Locale.ROOT));
                }
                if (merge.get("separator") instanceof String) {
                    flowCfg.put("separator", merge.get("separator"));
                }
            }
            Integer mc = toInteger(root.get("max_concurrency"));
            if (mc != null) flowCfg.put("max_concurrency", mc);
        } else {
            // 非 parallel 也记个类型，便于生成器分发
            flowCfg.put("type", agent.getAgentClass());
        }
        if (!flowCfg.isEmpty()) {
            agent.setFlowConfig(flowCfg);
        }

        // hooks 直接透传
        if (root.get("hooks") instanceof Map) {
            agent.setHooks((Map<String, Object>) root.get("hooks"));
        }

        // state：优先 schema 的 state.strategies，其次兼容 state_config
        if (root.get("state") instanceof Map) {
            Map<String, Object> state = (Map<String, Object>) root.get("state");
            if (state.get("strategies") instanceof Map) {
                Map<String, String> strategies = new HashMap<>();
                ((Map<?, ?>) state.get("strategies")).forEach((k, v) -> strategies.put(String.valueOf(k), String.valueOf(v)));
                agent.setStateConfig(strategies);
            }
        } else if (root.get("state_config") instanceof Map) {
            Map<String, String> sc = new HashMap<>();
            ((Map<?, ?>) root.get("state_config")).forEach((k, v) -> sc.put(String.valueOf(k), String.valueOf(v)));
            agent.setStateConfig(sc);
        }

        // 递归 sub_agents
        if (root.get("sub_agents") instanceof List) {
            List<Agent> subs = new ArrayList<>();
            for (Object o : (List<?>) root.get("sub_agents")) {
                if (o instanceof Map) {
                    Map<String, Object> childRoot = getAgentRoot((Map<String, Object>) o);
                    if (childRoot == null) childRoot = (Map<String, Object>) o;
                    subs.add(parseAgent(childRoot));
                }
            }
            agent.setSubAgents(subs);
        }

        return agent;
    }

    private Map<String, Object> dumpAgent(Agent agent) {
        Map<String, Object> m = new HashMap<>();

        // 基础属性
        if (agent.getAgentClass() != null) {
            m.put("type", agent.getAgentClass());
        }
        m.put("name", agent.getName());
        m.put("description", agent.getDescription());
        m.put("input_key", agent.getInputKey());
        m.put("output_key", agent.getOutputKey());

        // LLM
        Map<String, Object> llm = new HashMap<>();
        if (agent.getModel() != null) llm.put("model", agent.getModel());
        if (agent.getChatOptions() != null && !agent.getChatOptions().isEmpty()) llm.put("options", agent.getChatOptions());
        if (!llm.isEmpty()) m.put("llm", llm);
        if (agent.getInstruction() != null) m.put("instruction", agent.getInstruction());
        if (agent.getMaxIterations() != null) m.put("max_iterations", agent.getMaxIterations());

        // 工具
        if (agent.getTools() != null) m.put("tools", agent.getTools());
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

        // 并行/流程配置回写（只对 parallel 做结构化映射）
        Map<String, Object> flowCfg = agent.getFlowConfig();
        if (flowCfg != null && "parallel".equalsIgnoreCase(String.valueOf(flowCfg.getOrDefault("type", agent.getAgentClass())))) {
            Map<String, Object> merge = new HashMap<>();
            Object ms = flowCfg.get("merge_strategy");
            if (ms instanceof String) merge.put("strategy", ms);
            Object sep = flowCfg.get("separator");
            if (sep instanceof String) merge.put("separator", sep);
            if (!merge.isEmpty()) m.put("merge", merge);
            Object mc = flowCfg.get("max_concurrency");
            if (mc instanceof Number) m.put("max_concurrency", ((Number) mc).intValue());
        }

        // 递归 sub_agents
        if (agent.getSubAgents() != null && !agent.getSubAgents().isEmpty()) {
            List<Map<String, Object>> subs = agent.getSubAgents().stream()
                    .map(this::dumpAgent)
                    .map(x -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("agent", x);
                        return item;
                    })
                    .collect(Collectors.toList());
            m.put("sub_agents", subs);
        }

        return m;
    }
}
