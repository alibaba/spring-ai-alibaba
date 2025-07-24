package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.Map;

/**
 * A NodeAction that simply passes through the state data without any processing.
 *
 * @author hupei
 */
public class PassThroughNode implements NodeAction {

	@Override
	public Map<String, Object> apply(OverAllState state) {
		return state.data(); // 无任何处理
	}

}
