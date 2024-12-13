package com.alibaba.cloud.ai.graph.practice.insurance_sale.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.studio.StreamingServer.USER_INPUT;

public class HumanNode implements NodeAction<NodeState> {

	@Override
	public Map<String, Object> apply(NodeState state) {
		synchronized (USER_INPUT) {
			try {
				USER_INPUT.wait();
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

		}
		return USER_INPUT;
	}

}
