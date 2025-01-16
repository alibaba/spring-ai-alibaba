package com.alibaba.cloud.ai.graph.practice.intelligent_outbound_call.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.studio.StreamingServer.USER_INPUT;

public class CustomerNode implements NodeAction {

	@Override
	public Map<String, Object> apply(NodeState state) {
		USER_INPUT.clear();
		synchronized (USER_INPUT) {
			try {
				USER_INPUT.wait();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

		}
		USER_INPUT.remove(NodeState.OUTPUT);
		return new HashMap<>(USER_INPUT);
	}

}
