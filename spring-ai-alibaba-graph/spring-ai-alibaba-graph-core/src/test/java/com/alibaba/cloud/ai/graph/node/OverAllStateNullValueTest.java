package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OverAllStateNullValueTest {

	@Test
	void testOverAllStateWithNullValues() {
		Map<String, Object> variables = new HashMap<>();
		variables.put("nullVar", null);
		variables.put("validVar", "valid");

		OverAllState state = new OverAllState(variables);

		Optional<Object> nullVarOpt = state.value("nullVar");
		Optional<Object> validVarOpt = state.value("validVar");
		Optional<Object> missingVarOpt = state.value("missingVar");

		System.out.println("nullVar present: " + nullVarOpt.isPresent());
		System.out.println("nullVar value: " + nullVarOpt.orElse("NOT_PRESENT"));

		System.out.println("validVar present: " + validVarOpt.isPresent());
		System.out.println("validVar value: " + validVarOpt.orElse("NOT_PRESENT"));

		System.out.println("missingVar present: " + missingVarOpt.isPresent());
		System.out.println("missingVar value: " + missingVarOpt.orElse("NOT_PRESENT"));
	}

}
