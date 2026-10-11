/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.a2a;

import java.util.List;

import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Legacy_0_3_AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentCardWrapperTests {

	@Test
	void preferredInterface_selectsJsonRpcFromSupportedInterfacesIgnoringCase() {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.supportedInterfaces())
			.thenReturn(List.of(new AgentInterface(TransportProtocol.GRPC.asString(), "http://localhost:8080/grpc"),
					new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:8080/http-json"),
					new AgentInterface("jsonrpc", "http://localhost:8080/jsonrpc", null, "0.3.0")));
		AgentCardWrapper wrapper = new AgentCardWrapper(agentCard);

		assertEquals("http://localhost:8080/jsonrpc", wrapper.url());
		assertEquals("jsonrpc", wrapper.preferredTransport());
		assertEquals("0.3.0", wrapper.protocolVersion());
	}

	@Test
	void preferredInterface_fallsBackToLegacyJsonRpcAdditionalInterface() {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.supportedInterfaces()).thenReturn(List.of());
		when(agentCard.url()).thenReturn("http://localhost:8080/http-json");
		when(agentCard.preferredTransport()).thenReturn(TransportProtocol.HTTP_JSON.asString());
		when(agentCard.additionalInterfaces())
			.thenReturn(List.of(new Legacy_0_3_AgentInterface(TransportProtocol.GRPC.asString(),
					"http://localhost:8080/grpc"),
					new Legacy_0_3_AgentInterface("jsonrpc", "http://localhost:8080/jsonrpc")));
		AgentCardWrapper wrapper = new AgentCardWrapper(agentCard);

		assertEquals("http://localhost:8080/jsonrpc", wrapper.url());
		assertEquals("jsonrpc", wrapper.preferredTransport());
		assertEquals("0.3", wrapper.protocolVersion());
		assertEquals(List.of("0.3", "0.3"),
				wrapper.additionalInterfaces().stream().map(AgentInterface::protocolVersion).toList());
	}

	@Test
	void preferredInterface_defaultsLegacyPrimaryTransportToJsonRpc() {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.supportedInterfaces()).thenReturn(List.of());
		when(agentCard.url()).thenReturn("http://localhost:8080/jsonrpc");
		AgentCardWrapper wrapper = new AgentCardWrapper(agentCard);

		assertEquals("http://localhost:8080/jsonrpc", wrapper.url());
		assertEquals(TransportProtocol.JSONRPC.asString(), wrapper.preferredTransport());
		assertEquals("0.3", wrapper.protocolVersion());
	}

	@Test
	void preferredInterface_rejectsCardsWithoutJsonRpcInterface() {
		AgentCard agentCard = mock(AgentCard.class);
		when(agentCard.supportedInterfaces())
			.thenReturn(List.of(new AgentInterface(TransportProtocol.GRPC.asString(), "http://localhost:8080/grpc"),
					new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:8080/http-json")));
		AgentCardWrapper wrapper = new AgentCardWrapper(agentCard);

		IllegalStateException exception = assertThrows(IllegalStateException.class, wrapper::url);

		assertEquals("Agent card does not declare a JSONRPC interface", exception.getMessage());
	}

}
