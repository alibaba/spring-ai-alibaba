package com.alibaba.cloud.ai.graph.practice.insurance_sale.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.practice.insurance_sale.IsExecutor;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.studio.StreamingServer.USER_INPUT;

public class HumanNode implements NodeAction<IsExecutor.State> {

	@Override
	public Map<String, Object> apply(IsExecutor.State state) {
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
