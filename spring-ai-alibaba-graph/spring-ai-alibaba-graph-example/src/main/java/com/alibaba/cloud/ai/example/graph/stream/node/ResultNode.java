package com.alibaba.cloud.ai.example.graph.stream.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResultNode implements NodeAction {

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		System.out.println("state data = " + state.data());
		return Map.of();
	}

}
