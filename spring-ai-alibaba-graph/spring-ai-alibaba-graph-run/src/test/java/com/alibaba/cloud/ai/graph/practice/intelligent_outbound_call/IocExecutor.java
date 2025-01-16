package com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call;

import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.data.IocDataFactory;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.data.IocEdge;
import com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.graph.StateGraphWithEdge;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.agent.JSONStateSerializer;
import com.aliyuncs.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

	public static class GraphBuilder {
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
					graph.addNodeWithEdge(iocEdge.getId(), iocEdge.getAffirmativeNode(), iocEdge.getNegativeNode(), iocEdge.getRefusalNode(), iocEdge.getDefaultNode(), node_async(IocDataFactory.getNode(iocEdge.getId())));
				}
			}
			return graph;
		}
	}

}