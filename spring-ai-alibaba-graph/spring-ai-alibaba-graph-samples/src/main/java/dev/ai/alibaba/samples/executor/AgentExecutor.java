package dev.ai.alibaba.samples.executor;

import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import com.alibaba.cloud.ai.graph.state.NodeState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Slf4j
@Service
public class AgentExecutor {

	public class GraphBuilder {

		private StateSerializer stateSerializer;

		public GraphBuilder stateSerializer(StateSerializer stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		public StateGraph build() throws GraphStateException {
			if (stateSerializer == null) {
				stateSerializer = new JSONStateSerializer();
			}

			return new StateGraph(stateSerializer).addEdge(START, "agent") // 下一个节点
				.addNode("agent", node_async(new NodeAction() {
					@Override
					public Map<String, Object> apply(OverAllState t) throws Exception {
						return null;
					}
				})) // 调用llm
				.addConditionalEdges( // 条件边，在agent节点之后
						"agent", edge_async(new EdgeAction() {
							@Override
							public String apply(OverAllState t) throws Exception {
								return null;
							}
						}), // 根据agent的结果，进行条件判断
						Map.of("continue", END, "end", END) // 不同分支，使action不再独立
				);

		}

	}

	public final GraphBuilder graphBuilder() {
		return new GraphBuilder();
	}

	public record Outcome(Action action, Finish finish) {
	}

	public record Action(AssistantMessage.ToolCall toolCall, String log) {
	}

	public record Finish(Map<String, Object> returnValues, String log) {
	}

	private final AgentService agentService;

	public AgentExecutor(AgentService agentService) {
		this.agentService = agentService;
	}

	Map<String, Object> callAgent(NodeState state) {
		log.info("callAgent");

		var input = state.input().orElseThrow(() -> new IllegalArgumentException("no input provided!"));

		var response = agentService.execute(input);

		var output = response.getResult().getOutput();

		if (output.hasToolCalls()) {

			var action = new Action(output.getToolCalls().get(0), "");

			return Map.of(NodeState.OUTPUT, new Outcome(action, null));

		}
		else {
			var finish = new Finish(Map.of("returnValues", output.getContent()), output.getContent());

			return Map.of(NodeState.OUTPUT, new Outcome(null, finish));
		}
	}

	String shouldContinue(NodeState state) {

		return "end";
	}

}
