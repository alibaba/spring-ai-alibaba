package com.alibaba.cloud.ai.graph.practice.insurance_sale;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.practice.insurance_sale.node.HumanNode;
import com.alibaba.cloud.ai.graph.practice.insurance_sale.node.WelcomeNode;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Slf4j
@Service
public class IsExecutor {

	public enum Serializers {

		JSON(new IsJSONStateSerializer());

		private final StateSerializer<State> serializer;

		Serializers(StateSerializer<State> serializer) {
			this.serializer = serializer;
		}

		public StateSerializer<State> object() {
			return serializer;
		}

	}

	public class GraphBuilder {

		private StateSerializer<State> stateSerializer;

		public GraphBuilder stateSerializer(StateSerializer<State> stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		public StateGraph<State> build() throws GraphStateException {
			if (stateSerializer == null) {
				stateSerializer = new IsAgentStateSerializer();
			}
			MemorySaver saver = new MemorySaver();
			SaverConfig saverConfig = SaverConfig.builder()
					.type(SaverConstant.MEMORY)
					.register(SaverConstant.MEMORY, saver)
					.build();
			CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();
			var subGraph = new StateGraph<>(State.SCHEMA, stateSerializer).addEdge(START, "welcome")
					.addNode("welcome", node_async(new WelcomeNode())) // 调用llm
					.addEdge("welcome", "human")// 下一个节点
					.addNode("human", node_async(new HumanNode()))
					.addEdge("human", "agent")// 下一个节点
					.addNode("agent", node_async(IsExecutor.this::callAgent)) // 调用llm
					.addNode("action", IsExecutor.this::executeTools) // 独立节点
					.addConditionalEdges( // 条件边，在agent节点之后
							"agent", edge_async(IsExecutor.this::shouldContinue), // 根据agent的结果，进行条件判断
							Map.of("continue", "action", "end", END) // 不同分支，使action不再独立
					)
					.addEdge("action", "agent").compile(compileConfig);

			return new StateGraph<>(State.SCHEMA, stateSerializer).addEdge(START, "welcome")
				.addNode("welcome", node_async(new WelcomeNode())) // 调用llm
				.addEdge("welcome", "human")// 下一个节点
				.addNode("human", node_async(new HumanNode()))
				.addEdge("human", "subGraph")// 下一个节点
				.addSubgraph("subGraph", subGraph)
				.addEdge("subGraph", "agent")// 下一个节点
				.addNode("agent", node_async(IsExecutor.this::callAgent)) // 调用llm
				.addNode("action", IsExecutor.this::executeTools) // 独立节点
				.addConditionalEdges( // 条件边，在agent节点之后
						"agent", edge_async(IsExecutor.this::shouldContinue), // 根据agent的结果，进行条件判断
						Map.of("continue", "action", "end", END) // 不同分支，使action不再独立
				)
				.addEdge("action", "agent") // action后返回agent，非条件边
			;

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

	public static class State extends AgentState {

		public static final String INPUT = "input";

		public static final String AGENT_OUTCOME = "outcome";

		public static final String INTERMEDIATE_STEPS = "intermediate_steps";

		static Map<String, Channel<?>> SCHEMA = Map.of(INTERMEDIATE_STEPS, AppenderChannel.<Step>of(ArrayList::new));

		public State(Map<String, Object> initData) {
			super(initData);
		}

		public Optional<String> input() {
			return value(INPUT);
		}

		public Optional<Outcome> agentOutcome() {
			return value(AGENT_OUTCOME);
		}

		public List<Step> intermediateSteps() {
			return this.<List<Step>>value(INTERMEDIATE_STEPS).orElseGet(ArrayList::new);
		}

	}

	private final IsAgentService agentService;

	public IsExecutor(IsAgentService agentService) {
		this.agentService = agentService;
	}

	Map<String, Object> callAgent(State state) {
		log.info("callAgent");

		var input = state.input()
			.filter(StringUtils::hasText)
			.orElseThrow(() -> new IllegalArgumentException("no input provided!"));

		var intermediateSteps = state.intermediateSteps();

		var response = agentService.execute(input, intermediateSteps);

		var output = response.getResult().getOutput();

		if (output.hasToolCalls()) {

			var action = new Action(output.getToolCalls().get(0), "");

			return Map.of(State.AGENT_OUTCOME, new Outcome(action, null));

		}
		else {
			var finish = new Finish(Map.of("returnValues", output.getContent()), output.getContent());

			return Map.of(State.AGENT_OUTCOME, new Outcome(null, finish));
		}
	}

	CompletableFuture<Map<String, Object>> executeTools(State state) {
		log.trace("executeTools");

		var agentOutcome = state.agentOutcome()
			.orElseThrow(() -> new IllegalArgumentException("no agentOutcome provided!"));

		return agentService.toolService.executeFunction(agentOutcome.action().toolCall())
			.thenApply(result -> Map.of("intermediate_steps", new Step(agentOutcome.action(), result.responseData())));
	}

	String shouldContinue(State state) {
		return state.agentOutcome().map(Outcome::finish).map(finish -> "end").orElse("continue");
	}

}
