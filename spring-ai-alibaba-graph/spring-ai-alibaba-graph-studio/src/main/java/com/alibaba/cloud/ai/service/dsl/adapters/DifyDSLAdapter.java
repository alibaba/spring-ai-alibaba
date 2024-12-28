package com.alibaba.cloud.ai.service.dsl.adapters;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.AppMetadata;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.chatbot.node.ChatBot;
import com.alibaba.cloud.ai.model.workflow.*;
import com.alibaba.cloud.ai.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.SneakyThrows;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DifyDSLAdapter converts Dify DSL to {@link App} and vice versa.
 */
@Component
public class DifyDSLAdapter extends AbstractDSLAdapter {

    private static final String DIFY_DIALECT = "dify";

    private static final String[] DIFY_CHATBOT_MODES = {"chat", "completion", "agent-chat"};

    private static final String[] DIFY_WORKFLOW_MODES = {"workflow", "advanced-chat"};

    private final List<NodeDataConverter> nodeDataConverters;

    private final Serializer serializer;

    public DifyDSLAdapter (List<NodeDataConverter> nodeDataConverters, @Qualifier("yaml") Serializer serializer) {
        this.nodeDataConverters = nodeDataConverters;
        this.serializer = serializer;
    }

    private NodeDataConverter getNodeDataConverter (String type) {
        return nodeDataConverters.stream().filter(converter -> converter.supportType(type)).findFirst().orElseThrow(() -> new IllegalArgumentException("invalid dify node type " + type));
    }

    @Override
    public void validateDSLData (Map<String, Object> dslData) {
        if (dslData == null || !dslData.containsKey("app")) {
            throw new IllegalArgumentException("invalid dify dsl");
        }
    }

    @Override
    public Serializer getSerializer () {
        return serializer;
    }

    @Override
    public AppMetadata mapToMetadata (Map<String, Object> data) {
        Map<String, Object> map = (Map<String, Object>) data.get("app");
        AppMetadata metadata = new AppMetadata();
        if (Arrays.asList(DIFY_CHATBOT_MODES).contains((String) map.get("mode"))) {
            metadata.setMode(AppMetadata.CHATBOT_MODE);
        }
        else if (Arrays.asList(DIFY_WORKFLOW_MODES).contains((String) map.get("mode"))) {
            metadata.setMode(AppMetadata.WORKFLOW_MODE);
        }
        else {
            throw new IllegalArgumentException("unknown dify app mode" + map.get("mode"));
        }
        metadata.setId(UUID.randomUUID().toString());
        metadata.setName((String) map.getOrDefault("name", metadata.getMode() + "-" + metadata.getId()));
        metadata.setDescription((String) map.getOrDefault("description", ""));
        return metadata;
    }

    @Override
    public Map<String, Object> metadataToMap (AppMetadata metadata) {
        Map<String, Object> data = new HashMap<>();
        String difyMode = metadata.getMode().equals(AppMetadata.WORKFLOW_MODE) ? "workflow" : "agent-chat";
        data.put("app", Map.of("name", metadata.getName(), "description", metadata.getDescription(), "mode", difyMode));
        data.put("kind", "app");
        return data;
    }

    @Override
    public Workflow mapToWorkflow (Map<String, Object> data) {
        Map<String, Object> workflowData = (Map<String, Object>) data.get("workflow");
        Workflow workflow = new Workflow();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // map key is snake_case style
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        if (workflowData.containsKey("conversation_variables")) {
            List<Map<String, Object>> variables = (List<Map<String, Object>>) workflowData.get("conversation_variables");
            List<Variable> workflowVars = variables.stream().map(variable -> objectMapper.convertValue(variable, Variable.class)).collect(Collectors.toList());
            workflow.setWorkflowVars(workflowVars);
        }
        if (workflowData.containsKey("environment_variables")) {
            List<Map<String, Object>> variables = (List<Map<String, Object>>) workflowData.get("environment_variables");
            List<Variable> envVars = variables.stream().map(variable -> objectMapper.convertValue(variable, Variable.class)).collect(Collectors.toList());
            workflow.setEnvVars(envVars);
        }
        workflow.setGraph(constructGraph((Map<String, Object>) workflowData.get("graph")));
        return workflow;
    }

    private Graph constructGraph (Map<String, Object> data) {
        Graph graph = new Graph();
        List<Node> workflowNodes = new ArrayList<>();
        List<Edge> workflowEdges = new ArrayList<>();
        List<Map<String, Object>> branchNodes = new ArrayList<>();
        Map<String, Edge> branchEdges = new HashMap<>();
        // convert nodes
        if (data.containsKey("nodes")) {
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
            constructNodes(nodes, workflowNodes, branchNodes);
        }
        // convert edges
        if (data.containsKey("edges")) {
            List<Map<String, Object>> edges = (List<Map<String, Object>>) data.get("edges");
            constructEdges(edges, workflowEdges, branchEdges);
        }
        // convert if-else node to condition edge
        constructConditionEdge(branchNodes, branchEdges, workflowEdges);

        graph.setNodes(workflowNodes);
        graph.setEdges(workflowEdges);
        return graph;
    }

    private void constructNodes (List<Map<String, Object>> nodeMaps, List<Node> nodes, List<Map<String, Object>> branchNodes) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (Map<String, Object> nodeMap : nodeMaps) {
            Map<String, Object> nodeDataMap = (Map<String, Object>) nodeMap.get("data");
            String difyNodeType = (String) nodeDataMap.get("type");
            // collect if-else node
            if (difyNodeType.equals("if-else")) {
                branchNodes.add(nodeMap);
                continue;
            }
            // determine the type of dify node is supported yet
            NodeType nodeType = NodeType.difyValueOf(difyNodeType);
            if (nodeType == null) {
                throw new NotImplementedException("unsupported node type " + difyNodeType);
            }
            // convert node map to workflow node using jackson
            nodeMap.remove("data");
            Node n = objectMapper.convertValue(nodeMap, Node.class);
            // set title and desc
            n.setTitle((String) nodeDataMap.get("title")).setDesc((String) nodeDataMap.get("desc"));
            // convert node data using specific WorkflowNodeDataConverter
            NodeDataConverter nodeDataConverter = getNodeDataConverter(nodeType.value());
            n.setData(nodeDataConverter.parseDifyData(nodeDataMap));
            n.setType(nodeType.value());
            nodes.add(n);
        }
    }

    private void constructEdges (List<Map<String, Object>> edgeMaps, List<Edge> edges, Map<String, Edge> branchEdges) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (Map<String, Object> edgeMap : edgeMaps) {
            Edge edge = objectMapper.convertValue(edgeMap, Edge.class);
            if (edgeMap.get("sourceHandle").equals("source")) {
                edge.setType(EdgeType.DIRECT.value());
                edges.add(edge);
            }
            else {
                // collect if-else edges
                String sourceHandle = (String) edgeMap.get("sourceHandle");
                String source = (String) edgeMap.get("source");
                branchEdges.put(conditionKey(source, sourceHandle), edge);
            }
        }
    }

    private void constructConditionEdge (List<Map<String, Object>> branchNodes, Map<String, Edge> branchEdges, List<Edge> edges) {
        for (Map<String, Object> nodeMap : branchNodes) {
            Map<String, Object> nodeDataMap = (Map<String, Object>) nodeMap.get("data");
            String branchNodeId = (String) nodeMap.get("id");
            List<Case> cases = new ArrayList<>();
            Map<String, String> targetMap = new HashMap<>();
            List<Map<String, Object>> casesMap = (List<Map<String, java.lang.Object>>) nodeDataMap.get("cases");
            for (Map<String, Object> caseData : casesMap) {
                // convert cases
                List<Map<String, Object>> conditionMaps = (List<Map<String, Object>>) caseData.get("conditions");
                List<Case.Condition> conditions = conditionMaps.stream().map(conditionMap -> {
                    List<String> selectors = (List<String>) conditionMap.get("variable_selector");
                    return new Case.Condition().setValue((String) conditionMap.get("value")).setVarType((String) conditionMap.get("varType")).setComparisonOperator((String) conditionMap.get("comparison_operator")).setVariableSelector(new VariableSelector(selectors.get(0), selectors.get(1)));
                }).collect(Collectors.toList());
                Case c = new Case().setId((String) caseData.get("id")).setLogicalOperator((String) caseData.get("logical_operator")).setConditions(conditions);
                // collect case target
                Edge branchEdge = branchEdges.get(conditionKey(branchNodeId, c.getId()));
                targetMap.put(conditionKey(branchNodeId, c.getId()), branchEdge.getTarget());
                cases.add(c);
            }
            // else branch
            Edge elseEdge = branchEdges.get(conditionKey(branchNodeId, "false"));
            if (elseEdge != null) {
                targetMap.put(conditionKey(branchNodeId, "false"), elseEdge.getTarget());
            }
            // find branchNode's source
            String source = findSourceNode(edges, branchEdges.values().stream().toList(), branchNodeId);
            Edge conditionEdge = new Edge().setId(branchNodeId).setType(EdgeType.CONDITIONAL.value()).setSource(source).setCases(cases).setTargetMap(targetMap);
            edges.add(conditionEdge);
        }
    }

    private String conditionKey (String source, String caseId) {
        return source + "&" + caseId;
    }

    private String findSourceNode (List<Edge> directEdges, List<Edge> branchEdges, String nodeId) {
        for (Edge edge : directEdges) {
            if (edge.getTarget().equals(nodeId)) {
                return edge.getSource();
            }
        }
        for (Edge edge : branchEdges) {
            if (edge.getTarget().equals(nodeId)) {
                return edge.getSource();
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> workflowToMap (Workflow workflow) {
        Map<String, Object> data = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Map<String, Object>> workflowVars = objectMapper.convertValue(workflow.getWorkflowVars(), List.class);
        List<Map<String, Object>> envVars = objectMapper.convertValue(workflow.getEnvVars(), List.class);
        Graph graph = workflow.getGraph();
        Map<String, Object> graphMap = deconstructGraph(graph);
        data.put("workflow", Map.of("conversation_variables", workflowVars, "environment_variables", envVars, "graph", graphMap));
        return data;
    }

    private Map<String, Object> deconstructGraph (Graph graph) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Map<String, Object>> edgeMaps = new ArrayList<>();
        List<Map<String, Object>> nodeMaps = new ArrayList<>();
        // deconstruct edge
        deconstructEdge(graph.getEdges(), edgeMaps, nodeMaps);
        // deconstruct node
        deconstructNode(graph.getNodes(), nodeMaps);
        return Map.of("edges", edgeMaps, "nodes", nodeMaps);
    }

    private void deconstructEdge (List<Edge> edges, List<Map<String, Object>> edgeMaps, List<Map<String, Object>> nodeMaps) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (Edge edge : edges) {
            // collect direct edge
            if (edge.getType().equals(EdgeType.DIRECT.value())) {
                Map<String, Object> edgeMap = objectMapper.convertValue(edge, Map.class);
                edgeMap.put("sourceHandle", "source");
                edgeMap.put("targetHandle", "target");
                edgeMap.put("type", "custom");
                edgeMaps.add(edgeMap);
                continue;
            }
            // convert condition edge
            Map<String, String> targetMap = edge.getTargetMap();
            // number of entries in targetMap equals the number of edges needed to
            targetMap.forEach((k, v) -> {
                String[] splits = k.split("&");
                Map<String, Object> edgeMap = Map.of("source", splits[0], "sourceHandle", splits[1], "target", v, "targetHandle", "target", "type", "custom", "zIndex", 0, "selected", false);
                edgeMaps.add(edgeMap);
            });
            // convert to if-else node
            List<Map<String, Object>> caseMaps = new ArrayList<>();
            for (Case c : edge.getCases()) {
                List<Map<String, Object>> conditions = c.getConditions().stream().map(condition -> Map.of("comparison_operator", condition.getComparisonOperator(), "value", condition.getValue(), "varType", condition.getVarType(), "variable_selector", List.of(condition.getVariableSelector().getNamespace(), condition.getVariableSelector().getName()))).toList();
                caseMaps.add(Map.of("id", c.getId(), "case_id", c.getId(), "conditions", conditions, "logical_operator", c.getLogicalOperator()));
            }
            nodeMaps.add(Map.of("id", edge.getId(), "type", "custom", "width", 250, "height", 250, "data", Map.of("cases", caseMaps, "desc", "", "selected", false, "title", "if-else", "type", "if-else")));
        }

    }

    private void deconstructNode (List<Node> nodes, List<Map<String, Object>> nodeMaps) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (Node node : nodes) {
            Map<String, Object> n = objectMapper.convertValue(node, Map.class);
            NodeType nodeType = NodeType.valueOf(node.getType());
            NodeDataConverter nodeDataConverter = getNodeDataConverter(node.getType());
            Map<String, Object> nodeData = nodeDataConverter.dumpDifyData(node.getData());
            nodeData.put("type", nodeType.difyValue());
            nodeData.put("title", node.getTitle());
            nodeData.put("desc", node.getDesc());
            n.put("data", nodeData);
            n.put("type", "custom");
            nodeMaps.add(n);
        }
    }

    @Override
    @SneakyThrows
    public ChatBot mapToChatBot (Map<String, Object> data) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ChatBot chatBot = new ChatBot();

        Map<String, Object> chatbotData = (Map<String, Object>) data.get("model_config");

        ChatBot.AgentMode agentMode = objectMapper.convertValue(chatbotData.get("agent_mode"), ChatBot.AgentMode.class);
        ChatBot.Model model = objectMapper.convertValue(chatbotData.get("model"), ChatBot.Model.class);
        String openingStatement = objectMapper.writeValueAsString(chatbotData.get("opening_statement"));
        String prePrompt = objectMapper.writeValueAsString(chatbotData.get("pre_prompt"));
        String promptType = objectMapper.writeValueAsString(chatbotData.get("prompt_type"));
        ChatBot.CompletionPromptConfig completionPromptConfig = objectMapper.convertValue(chatbotData.get("completion_prompt_config"), ChatBot.CompletionPromptConfig.class);

        List<Map<String, Object>> userInputList = (List<Map<String, Object>>) ((Map<String, Object>) data.get("model_config")).get("user_input_form");

        List<ChatBot.Paragraph> paragraphList = new ArrayList<>();
        List<ChatBot.Select> selectList = new ArrayList<>();
        List<ChatBot.Number> numberList = new ArrayList<>();
        List<ChatBot.TextInput> textInputList = new ArrayList<>();

        for (Map<String, Object> item : userInputList) {

            if (item.containsKey("text_input")) {
                ChatBot.TextInput textInput = objectMapper.convertValue(item.get("text-input"), ChatBot.TextInput.class);
                textInputList.add(textInput);
            }
            if (item.containsKey("select")) {
                ChatBot.Select select = objectMapper.convertValue(item.get("select"), ChatBot.Select.class);
                selectList.add(select);
            }
            if (item.containsKey("paragraph")) {
                ChatBot.Paragraph paragraph = objectMapper.convertValue(item.get("paragraph"), ChatBot.Paragraph.class);
                paragraphList.add(paragraph);
            }
            if (item.containsKey("number")) {
                ChatBot.Number number = objectMapper.convertValue(item.get("number"), ChatBot.Number.class);
                numberList.add(number);
            }
        }

        ChatBot.UserInputForm userInputForm = new ChatBot.UserInputForm();

        userInputForm.setParagraph(paragraphList);
        userInputForm.setSelect(selectList);

        chatBot.setUserInputForm(userInputForm);
        chatBot.setAgentMode(agentMode);
        chatBot.setModel(model);
        chatBot.setPrePrompt(prePrompt);
        chatBot.setPromptType(promptType);
        chatBot.setOpeningStatement(openingStatement);
        chatBot.setCompletionPromptConfig(completionPromptConfig);
        return chatBot;
    }

    @Override
    public Map<String, Object> chatbotToMap (ChatBot chatBot) {
        Map<String, Object> data = new HashMap<>();
        data.put("chatbot", chatBot);
        return data;
    }

    @Override
    public Boolean supportDialect (String dialect) {
        return DIFY_DIALECT.equals(dialect);
    }

}
