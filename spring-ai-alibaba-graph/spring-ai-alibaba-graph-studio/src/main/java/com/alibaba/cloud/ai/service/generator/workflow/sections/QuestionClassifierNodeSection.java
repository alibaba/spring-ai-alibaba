package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.QuestionClassifierNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class QuestionClassifierNodeSection implements NodeSection {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.QUESTION_CLASSIFIER.equals(nodeType);
	}

	@Override
	public String render(Node node) {
		QuestionClassifierNodeData d = (QuestionClassifierNodeData) node.getData();
		String id = node.getId();
		String inputKey = d.getInputs().get(0).getName();

		// 把可能的换行替换成空格，确保整个字符串在 Java 源里是一行
		String instructions = d.getInstruction() == null ? ""
				: d.getInstruction().replace("\n", " ").replace("\"", "\\\"");

		// categories 同前
		String categories = d.getClasses()
			.stream()
			.map(c -> "\"" + c.getText() + "\"")
			.collect(Collectors.joining(", "));

		return String.format(
				"// —— QuestionClassifierNode [%s] ——%n"
						+ "QuestionClassifierNode %1$sNode = QuestionClassifierNode.builder()%n"
						+ "    .chatClient(chatClient)%n" + "    .inputTextKey(\"%s\")%n"
						+ "    .categories(List.of(%s))%n" + "    .classificationInstructions(List.of(\"%s\"))%n"
						+ "    .build();%n" + "stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%1$sNode));%n%n",
				id, inputKey, categories, instructions, id);
	}

}
