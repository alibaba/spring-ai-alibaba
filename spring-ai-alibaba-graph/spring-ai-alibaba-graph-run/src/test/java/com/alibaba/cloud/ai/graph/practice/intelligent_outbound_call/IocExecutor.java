package com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call;

import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.data.IocDataFactory;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.data.IocEdge;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.graph.StateGraphWithEdge;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.aliyuncs.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Slf4j
@Service
public class IocExecutor {
	private final IocAgentService agentService;

	public IocExecutor(IocAgentService agentService) {
		this.agentService = agentService;
	}

	public final GraphBuilder graphBuilder() {
		return new GraphBuilder();
	}

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
			var graph = new StateGraphWithEdge(stateSerializer);
			for (IocEdge iocEdge : IocDataFactory.getIocEdges()) {
				if (!StringUtils.isEmpty(iocEdge.getNextNode())) {
					graph.addNodeWithEdge(iocEdge.getId(), iocEdge.getNextNode(), node_async(IocDataFactory.getNode(iocEdge.getId())));
				} else {
					graph.addNodeWithConditionalEdge(iocEdge.getId(), iocEdge.getAffirmativeNode(), iocEdge.getNegativeNode(), iocEdge.getRefusalNode(), iocEdge.getDefaultNode(), node_async(IocDataFactory.getNode(iocEdge.getId())), edge_async(IocExecutor.this::generateCustomer));
				}
			}
			return graph;
		}
	}

	String generateCustomer(NodeState state) {
		var input = state.input()
				.filter(org.springframework.util.StringUtils::hasText)
				.orElseThrow(() -> new IllegalArgumentException("no input provided!"));
		if (affirmatives.contains(input)) {
			return "affirmative";
		} else if (negatives.contains(input)) {
			return "negative";
		} else if (refusals.contains(input)) {
			return "refusal";
		} else {
			return "default";
		}
	}

	private static final List<String> affirmatives = new ArrayList<>();
	private static final List<String> negatives = new ArrayList<>();
	private static final List<String> refusals = new ArrayList<>();

	static {
		affirmatives.add("是的");
		affirmatives.add("好的");
		affirmatives.add("对的");

		negatives.add("不是");
		negatives.add("不行");
		negatives.add("不对");

		refusals.add("不需要");
		refusals.add("没空");
		refusals.add("在忙");
	}

}