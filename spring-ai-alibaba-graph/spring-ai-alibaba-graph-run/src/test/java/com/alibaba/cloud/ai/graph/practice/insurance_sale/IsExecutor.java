package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.practice.insurance_sale.node.HumanNode;
import com.alibaba.cloud.ai.graph.practice.insurance_sale.node.WelcomeNode;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.NodeState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Slf4j
@Service
public class IsExecutor {

	public enum Serializers {

		JSON(new IsJSONStateSerializer());

		private final StateSerializer<NodeState> serializer;

		Serializers(StateSerializer<NodeState> serializer) {
			this.serializer = serializer;
		}

		public StateSerializer<NodeState> object() {
			return serializer;
		}

	}

	public class GraphBuilder {

		private StateSerializer<NodeState> stateSerializer;

		public GraphBuilder stateSerializer(StateSerializer<NodeState> stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		public StateGraph<NodeState> build() throws GraphStateException {
			if (stateSerializer == null) {
				stateSerializer = new IsAgentStateSerializer();
			}
			// MemorySaver saver = new MemorySaver();
			// SaverConfig saverConfig = SaverConfig.builder()
			// .type(SaverConstant.MEMORY)
			// .register(SaverConstant.MEMORY, saver)
			// .build();
			// CompileConfig compileConfig =
			// CompileConfig.builder().saverConfig(saverConfig).build();
			// var subGraph = new StateGraph<>(State.SCHEMA,
			// stateSerializer).addEdge(START, "welcome")
			// .addNode("welcome", node_async(new WelcomeNode())) // 调用llm
			// .addEdge("welcome", "human")// 下一个节点
			// .addNode("human", node_async(new HumanNode()))
			// .addEdge("human", "agent")// 下一个节点
			// .addNode("agent", node_async(IsExecutor.this::callAgent)) // 调用llm
			// .addNode("action", IsExecutor.this::executeTools) // 独立节点
			// .addConditionalEdges( // 条件边，在agent节点之后
			// "agent", edge_async(IsExecutor.this::shouldContinue), // 根据agent的结果，进行条件判断
			// Map.of("continue", "action", "end", END) // 不同分支，使action不再独立
			// )
			// .addEdge("action", "agent")
			// .compile(compileConfig);

			return new StateGraph<>(stateSerializer).addEdge(START, "welcome")
				.addNode("welcome", node_async(new WelcomeNode())) // 调用llm
				.addEdge("welcome", "human")// 下一个节点
				.addNode("human", node_async(new HumanNode()))
				// .addEdge("human", "subGraph")// 下一个节点
				// .addSubgraph("subGraph", subGraph)
				// .addEdge("subGraph", "agent")// 下一个节点
				.addConditionalEdges( // 条件边，在agent节点之后
						"human", edge_async(IsExecutor.this::questionEnough),
						Map.of("continue", "agent", "return", "prompt"))
				.addNode("prompt", node_async(new WelcomeNode("请填写年龄、性别、学历等完整信息")))
				.addEdge("prompt", "human")// 下一个节点
				// .addEdge("human", "agent")// 下一个节点
				.addNode("agent", node_async(IsExecutor.this::callAgent)) // 调用llm
				.addNode("human1", node_async(new HumanNode()))
				.addEdge("agent", "human1")// 下一个节点
				.addNode("next", node_async(new WelcomeNode("我们将会在稍后联系您，引导购买")))
				.addEdge("next", END)
				.addConditionalEdges( // 条件边，在agent节点之后
						"human1", edge_async(IsExecutor.this::purchaseIntention),
						Map.of("purchase", "next", "end", END));
			// .addNode("action", IsExecutor.this::executeTools) // 独立节点

		}

	}

	public final GraphBuilder graphBuilder() {
		return new GraphBuilder();
	}

	public record Outcome(Action action, Finish finish) {
	}

	public record Step(Action action, String observation) {
	}

	public record Action(AssistantMessage.ToolCall toolCall, String log) {
	}

	public record Finish(Map<String, Object> returnValues, String log) {
	}

	private final IsAgentService agentService;

	public IsExecutor(IsAgentService agentService) {
		this.agentService = agentService;
	}

	Map<String, Object> callAgent(NodeState state) {
		log.info("callAgent");

		var input = state.input()
			.filter(StringUtils::hasText)
			.orElseThrow(() -> new IllegalArgumentException("no input provided!"));

		var response = agentService.execute(input);

		var output = response.getResult().getOutput();

		if (output.hasToolCalls()) {
			var action = new Action(output.getToolCalls().get(0), "");
			return Map.of(NodeState.AGENT_OUTCOME, new Outcome(action, null));

		}
		else {
			var finish = new Finish(Map.of("returnValues", output.getContent()), output.getContent());

			return Map.of(NodeState.AGENT_OUTCOME, new Outcome(null, finish));
		}
	}

	String questionEnough(NodeState state) {

		Map<String, Object> returnValues = state.data();
		if (!returnValues.containsKey("input")) {
			return "return";
		}
		String input = (String) returnValues.get("input");
		// 判断用户输入内容是否包括全部所需要信息（年龄、性别、学历……）
		if (input.contains("年龄") && input.contains("性别") && input.contains("学历")) {
			return "continue";
		}
		else {
			return "return";
		}
	}

	String purchaseIntention(NodeState state) {

		var input = state.input()
			.filter(StringUtils::hasText)
			.orElseThrow(() -> new IllegalArgumentException("no input provided!"));

		var response = agentService.executeByPrompt("判断用户是否有购买保险意愿，只返回是或者否。比如用户输入我要买，返回是。" + input,
				"判断用户是否有购买保险意愿，只返回是或者否。比如用户输入我要买，返回是");

		var output = response.getResult().getOutput();
		log.info("agent:{}", output.getContent());
		// 判断用户输入内容是否包括全部所需要信息（年龄、性别、学历……）
		if (output.getContent().equals("是")) {
			return "purchase";
		}
		else {
			return "end";
		}
	}

}
