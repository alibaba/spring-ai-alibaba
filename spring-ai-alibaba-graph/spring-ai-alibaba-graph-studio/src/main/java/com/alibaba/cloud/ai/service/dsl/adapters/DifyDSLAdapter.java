package com.alibaba.cloud.ai.service.dsl.adapters;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.AppMetadata;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.chatbot.ChatBot;

import com.alibaba.cloud.ai.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.model.workflow.*;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * DifyDSLAdapter converts Dify DSL to {@link App} and vice versa.
 */
@Component
@Slf4j
public class DifyDSLAdapter extends AbstractDSLAdapter {

	private static final String[] DIFY_CHATBOT_MODES = { "chat", "completion", "agent-chat" };

	private static final String[] DIFY_WORKFLOW_MODES = { "workflow", "advanced-chat" };

	private final List<NodeDataConverter<? extends NodeData>> nodeDataConverters;

	private final Serializer serializer;

	public DifyDSLAdapter(List<NodeDataConverter<? extends NodeData>> nodeDataConverters, @Qualifier("yaml") Serializer serializer) {
		this.nodeDataConverters = nodeDataConverters;
		this.serializer = serializer;
	}

	private NodeDataConverter<? extends NodeData> getNodeDataConverter(NodeType nodeType) {
		return nodeDataConverters.stream()
			.filter(converter -> converter.supportNodeType(nodeType))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("invalid dify node type " + nodeType));
	}

	@Override
	public void validateDSLData(Map<String, Object> dslData) {
		if (dslData == null || !dslData.containsKey("app")) {
			throw new IllegalArgumentException("invalid dify dsl");
		}
	}

	@Override
	public Serializer getSerializer() {
		return serializer;
	}

	@Override
	public AppMetadata mapToMetadata(Map<String, Object> data) {
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
	public Map<String, Object> metadataToMap(AppMetadata metadata) {
		Map<String, Object> data = new HashMap<>();
		String difyMode = metadata.getMode().equals(AppMetadata.WORKFLOW_MODE) ? "workflow" : "agent-chat";
		data.put("app", Map.of("name", metadata.getName(), "description", metadata.getDescription(), "mode", difyMode));
		data.put("kind", "app");
		return data;
	}

	@Override
	public Workflow mapToWorkflow(Map<String, Object> data) {
		Map<String, Object> workflowData = (Map<String, Object>) data.get("workflow");
		Workflow workflow = new Workflow();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// map key is snake_case style
		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		if (workflowData.containsKey("conversation_variables")) {
			List<Map<String, Object>> variables = (List<Map<String, Object>>) workflowData
				.get("conversation_variables");
			List<Variable> workflowVars = variables.stream()
				.map(variable -> objectMapper.convertValue(variable, Variable.class))
				.collect(Collectors.toList());
			workflow.setWorkflowVars(workflowVars);
		}
		if (workflowData.containsKey("environment_variables")) {
			List<Map<String, Object>> variables = (List<Map<String, Object>>) workflowData.get("environment_variables");
			List<Variable> envVars = variables.stream()
				.map(variable -> objectMapper.convertValue(variable, Variable.class))
				.collect(Collectors.toList());
			workflow.setEnvVars(envVars);
		}
		workflow.setGraph(constructGraph((Map<String, Object>) workflowData.get("graph")));
		return workflow;
	}

	private Graph constructGraph(Map<String, Object> data) {
		Graph graph = new Graph();
		List<Node> nodes = new ArrayList<>();
		List<Edge> edges = new ArrayList<>();
		// convert nodes
		if (data.containsKey("nodes")) {
			List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) data.get("nodes");
			nodes = constructNodes(nodeMaps);
		}
		// convert edges
		if (data.containsKey("edges")) {
			List<Map<String, Object>> edgeMaps = (List<Map<String, Object>>) data.get("edges");
			edges = constructEdges(edgeMaps);
		}
		graph.setNodes(nodes);
		graph.setEdges(edges);
		return graph;
	}

	private List<Node> constructNodes(List<Map<String, Object>> nodeMaps) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Node> nodes = new ArrayList<>();
		for (Map<String, Object> nodeMap : nodeMaps) {
			Map<String, Object> nodeDataMap = (Map<String, Object>) nodeMap.get("data");
			String difyNodeType = (String) nodeDataMap.get("type");
			// determine the type of dify node is supported yet
			NodeType nodeType = NodeType.fromDifyValue(difyNodeType)
				.orElseThrow(() -> new NotImplementedException("unsupported node type " + difyNodeType));
			// convert node map to workflow node using jackson
			nodeMap.remove("data");
			Node n = objectMapper.convertValue(nodeMap, Node.class);
			// set title and desc
			n.setTitle((String) nodeDataMap.get("title")).setDesc((String) nodeDataMap.get("desc"));
			// convert node data using specific WorkflowNodeDataConverter
			NodeDataConverter<?> nodeDataConverter = getNodeDataConverter(nodeType);
			n.setData(nodeDataConverter.parseMapData(nodeDataMap, DSLDialectType.DIFY));
			n.setType(nodeType.value());
			nodes.add(n);
		}
		return nodes;
	}

	private List<Edge> constructEdges(List<Map<String, Object>> edgeMaps) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return edgeMaps.stream().map(edgeMap -> objectMapper.convertValue(edgeMap, Edge.class)).toList();
	}

	@Override
	public Map<String, Object> workflowToMap(Workflow workflow) {
		Map<String, Object> data = new HashMap<>();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<Map<String, Object>> workflowVars = objectMapper.convertValue(workflow.getWorkflowVars(), List.class);
		List<Map<String, Object>> envVars = objectMapper.convertValue(workflow.getEnvVars(), List.class);
		Graph graph = workflow.getGraph();
		Map<String, Object> graphMap = deconstructGraph(graph);
		data.put("workflow",
				Map.of("conversation_variables", workflowVars, "environment_variables", envVars, "graph", graphMap));
		return data;
	}

	private Map<String, Object> deconstructGraph(Graph graph) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// deconstruct edge
		List<Map<String, Object>> edgeMaps = deconstructEdge(graph.getEdges());
		// deconstruct node
		List<Map<String, Object>> nodeMaps = deconstructNode(graph.getNodes());
		return Map.of("edges", edgeMaps, "nodes", nodeMaps);
	}

	private List<Map<String, Object>> deconstructEdge(List<Edge> edges) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return edges.stream().map(edge -> {
			Map<String, Object> edgeMap = objectMapper.convertValue(edge, new TypeReference<>() {
			});
			edgeMap.put("type", "custom");
			return edgeMap;
		}).toList();

	}

	private List<Map<String, Object>> deconstructNode(List<Node> nodes) {
		ObjectMapper objectMapper = new ObjectMapper();
		List<Map<String, Object>> nodeMaps = new ArrayList<>();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		for (Node node : nodes) {
			Map<String, Object> n = objectMapper.convertValue(node, new TypeReference<>() {
			});
			NodeType nodeType = NodeType.fromValue(node.getType())
				.orElseThrow(() -> new NotImplementedException("Unsupported NodeType: " + node.getType()));
			NodeDataConverter<? extends NodeData> nodeDataConverter = getNodeDataConverter(nodeType);
			Map<String, Object> nodeData = dumpMapData(nodeDataConverter, node.getData());
			nodeData.put("type", nodeType.difyValue());
			nodeData.put("title", node.getTitle());
			nodeData.put("desc", node.getDesc());
			n.put("data", nodeData);
			n.put("type", "custom");
			nodeMaps.add(n);
		}
		return nodeMaps;
	}

	private <T extends NodeData> Map<String, Object> dumpMapData(NodeDataConverter<T> converter, NodeData data) {
		return converter.dumpMapData((T) data, DSLDialectType.DIFY);
	}

    @Override
    @SneakyThrows
    public ChatBot mapToChatBot (Map<String, Object> data) {
        if (data == null || !data.containsKey("model_config")) {
            throw new IllegalArgumentException("Invalid input data");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ChatBot chatBot = new ChatBot();

        Map<String, Object> modelConfig = (Map<String, Object>) data.get("model_config");

        if (modelConfig == null) {
            throw new IllegalArgumentException("Invalid model_config in input data");
        }

        ChatBot.AgentMode agentMode = safeConvert(modelConfig, "agent_mode", ChatBot.AgentMode.class, objectMapper);
        ChatBot.Model model = safeConvert(modelConfig, "model", ChatBot.Model.class, objectMapper);
        String openingStatement = safeWriteValue(modelConfig, "opening_statement", objectMapper);
        String prePrompt = safeWriteValue(modelConfig, "pre_prompt", objectMapper);
        String promptType = safeWriteValue(modelConfig, "prompt_type", objectMapper);
        ChatBot.CompletionPromptConfig completionPromptConfig = safeConvert(modelConfig, "completion_prompt_config", ChatBot.CompletionPromptConfig.class, objectMapper);

        // 构建datasetList
        Map<String, Object> datasetConfigsMap = (Map<String, Object>) modelConfig.get("dataset_configs");
        boolean rerankingEnable = Boolean.parseBoolean(safeWriteValue(datasetConfigsMap, "reranking_enable", objectMapper));
        String retrievalModel = safeWriteValue(datasetConfigsMap, "retrieval_model", objectMapper);
        int topK = Integer.parseInt(safeWriteValue(datasetConfigsMap, "top_k", objectMapper));
        ChatBot.Weights weights = safeConvert(datasetConfigsMap, "weights", ChatBot.Weights.class, objectMapper);
        JsonNode datasetsNode = objectMapper.valueToTree(datasetConfigsMap.get("datasets"));
        List<ChatBot.DataSet> datasetList = new ArrayList<>();
        if (datasetsNode.isObject()) {
            JsonNode datasetsArray = (datasetsNode).get("datasets");
            if (datasetsArray.isArray()) {
                for (JsonNode datasetNode : datasetsArray) {
                    JsonNode innerDatasetNode = datasetNode.get("dataset");
                    if (innerDatasetNode != null) {
                        ChatBot.DataSet dataset = new ChatBot.DataSet();
                        dataset.setEnabled(innerDatasetNode.get("enabled").asBoolean());
                        dataset.setId(innerDatasetNode.get("id").asText());
                        datasetList.add(dataset);
                    }
                }
            }
        }
        chatBot.setDatasetConfigs(new ChatBot.DataSetConfig(datasetList, rerankingEnable, retrievalModel, topK, weights));

        // 构建fileUpload
        Map<String, Object> fileUploadMap = (Map<String, Object>) modelConfig.get("file_upload");
        ChatBot.FileUpLoad fileUpLoad = objectMapper.convertValue(fileUploadMap, ChatBot.FileUpLoad.class);

        chatBot.setFileUpLoad(fileUpLoad);
        chatBot.setAgentMode(agentMode);
        chatBot.setModel(model);
        chatBot.setPrePrompt(prePrompt);
        chatBot.setPromptType(promptType);
        chatBot.setOpeningStatement(openingStatement);
        chatBot.setCompletionPromptConfig(completionPromptConfig);

        List<Map<String, Object>> userInputList = (List<Map<String, Object>>) ((Map<String, Object>) data.get("model_config")).get("user_input_form");

        if (userInputList.isEmpty()) {
            return chatBot;
        }

        List<ChatBot.Paragraph> paragraphList = new ArrayList<>();
        List<ChatBot.Select> selectList = new ArrayList<>();
        List<ChatBot.Number> numberList = new ArrayList<>();
        List<ChatBot.TextInput> textInputList = new ArrayList<>();

        for (Map<String, Object> item : userInputList) {

            if (item.containsKey("text_input")) {
                ChatBot.TextInput textInput = objectMapper.convertValue(item.get("text-input"), ChatBot.TextInput.class);
                textInputList.add(textInput);
            }
            else if (item.containsKey("select")) {
                ChatBot.Select select = objectMapper.convertValue(item.get("select"), ChatBot.Select.class);
                selectList.add(select);
            }
            else if (item.containsKey("paragraph")) {
                ChatBot.Paragraph paragraph = objectMapper.convertValue(item.get("paragraph"), ChatBot.Paragraph.class);
                paragraphList.add(paragraph);
            }
            else if (item.containsKey("number")) {
                ChatBot.Number number = objectMapper.convertValue(item.get("number"), ChatBot.Number.class);
                numberList.add(number);
            }
        }

        ChatBot.UserInputForm userInputForm = new ChatBot.UserInputForm();

        userInputForm.setParagraph(paragraphList);
        userInputForm.setSelect(selectList);
        userInputForm.setTextInput(textInputList);
        userInputForm.setNumber(numberList);

        chatBot.setUserInputForm(userInputForm);

        return chatBot;
    }

    @Override
    public Map<String, Object> chatbotToMap (ChatBot chatBot) {
        Map<String, Object> data = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);

        List<Map<String, Object>> userInputList = new ArrayList<>();
        ChatBot.UserInputForm userInputForm = chatBot.getUserInputForm();

        Map<String, Object> datasetConfigMap = new HashMap<>();

        // file_upload
        ChatBot.FileUpLoad fileUpLoad = null;
        if (chatBot.getFileUpLoad() != null) {
            fileUpLoad = objectMapper.convertValue(chatBot.getFileUpLoad(), ChatBot.FileUpLoad.class);
        }

        // dataset_configs
        if (chatBot.getDatasetConfigs() != null) {

            datasetConfigMap.put("datasets", chatBot.getDatasetConfigs().getDataSet() != null ? chatBot.getDatasetConfigs().getDataSet() : "");
            datasetConfigMap.put("reranking_enable", chatBot.getDatasetConfigs().getRerankingEnable());
            datasetConfigMap.put("retrieval_model", chatBot.getDatasetConfigs().getRerankingMode());
            datasetConfigMap.put("top_k", chatBot.getDatasetConfigs().getTopK());
            datasetConfigMap.put("weights", chatBot.getDatasetConfigs().getWeights() != null ? chatBot.getDatasetConfigs().getWeights() : "");
        }

        // user_input_form
        if (userInputForm != null) {

            processElements(userInputForm::getParagraph, "paragraph", userInputList, objectMapper);
            processElements(userInputForm::getSelect, "select", userInputList, objectMapper);
            processElements(userInputForm::getNumber, "number", userInputList, objectMapper);
            processElements(userInputForm::getTextInput, "text-input", userInputList, objectMapper);

        }

        Map<String, Object> modelConfig = Map.of("agent_mode", chatBot.getAgentMode() != null ? chatBot.getAgentMode() : "",
                "model", chatBot.getModel() != null ? chatBot.getModel() : "", "opening_statement",
                chatBot.getOpeningStatement() != null ? chatBot.getOpeningStatement() : "", "pre_prompt",
                chatBot.getPrePrompt() != null ? chatBot.getPrePrompt() : "", "prompt_type",
                chatBot.getPromptType() != null ? chatBot.getPromptType() : "", "completion_prompt_config",
                chatBot.getCompletionPromptConfig() != null ? chatBot.getCompletionPromptConfig() : "",
                "file_upload", fileUpLoad != null ? fileUpLoad : "",
                "dataset_configs", datasetConfigMap,
                "user_input_form", userInputList);

        data.put("model_config", modelConfig);
        return data;
    }

    private <T> void processElements (Supplier<List<T>> elementSupplier, String key, List<Map<String, Object>> userInputList, ObjectMapper objectMapper) {
        List<T> elements = elementSupplier.get();
        objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        if (elements != null && !elements.isEmpty()) {
            for (T element : elements) {
                try {
                    Map<String, Object> elementMap = objectMapper.convertValue(element, Map.class);
                    userInputList.add(Collections.singletonMap(key, elementMap));
                }
                catch (Exception e) {
                    log.error("Error converting element to map: {}", e.getMessage());
                }
            }
        }
    }

	@Override
	public Boolean supportDialect(DSLDialectType dialectType) {
		return DSLDialectType.DIFY.equals(dialectType);
	}

}
