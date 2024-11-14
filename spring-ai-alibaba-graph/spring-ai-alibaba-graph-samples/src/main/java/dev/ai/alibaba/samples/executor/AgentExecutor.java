package dev.ai.alibaba.samples.executor;

import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import dev.ai.alibaba.samples.executor.std.AgentStateSerializer;
import dev.ai.alibaba.samples.executor.std.json.JSONStateSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

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
public class AgentExecutor {

	public enum Serializers {

		JSON(new JSONStateSerializer());

		private final StateSerializer<AgentExecutor.State> serializer;

		Serializers(StateSerializer<AgentExecutor.State> serializer) {
			this.serializer = serializer;
		}

		public StateSerializer<AgentExecutor.State> object() {
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
				stateSerializer = new AgentStateSerializer();
			}

			return new StateGraph<>(State.SCHEMA, stateSerializer).addEdge(START, "agent") // 下一个节点
				.addNode("agent", node_async(AgentExecutor.this::callAgent)) // 调用llm
				.addNode("action", AgentExecutor.this::executeTools) // 独立节点
				.addConditionalEdges( // 条件边，在agent节点之后
						"agent", edge_async(AgentExecutor.this::shouldContinue), // 根据agent的结果，进行条件判断
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

	private final AgentService agentService;

	public AgentExecutor(AgentService agentService) {
		this.agentService = agentService;
	}

	Map<String, Object> callAgent(State state) {
		log.info("callAgent");

		var input = state.input().orElseThrow(() -> new IllegalArgumentException("no input provided!"));

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
