/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.core.server;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.ServerRequest;

import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AErrorResponse;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonRpcA2aRequestHandlerTest {

	private JSONRPCHandler jsonRpcHandler;

	private JsonRpcA2aRequestHandler requestHandler;

	private ServerRequest.Headers headers;

	@BeforeEach
	void setUp() {
		this.jsonRpcHandler = mock(JSONRPCHandler.class);
		this.requestHandler = new JsonRpcA2aRequestHandler(this.jsonRpcHandler);
		this.headers = mock(ServerRequest.Headers.class);
		when(this.headers.asHttpHeaders()).thenReturn(new HttpHeaders());
	}

	@Test
	void nonStreamingHandlerErrorPreservesRequestId() {
		when(this.jsonRpcHandler.onGetTask(any(), any())).thenThrow(new TaskNotFoundError());

		Object result = this.requestHandler.onHandler("""
				{
				  "jsonrpc": "2.0",
				  "id": "get-task-request",
				  "method": "GetTask",
				  "params": { "id": "missing-task" }
				}
				""", this.headers);

		A2AErrorResponse response = assertInstanceOf(A2AErrorResponse.class, result);
		assertEquals("get-task-request", response.getId());
	}

	@Test
	void streamingHandlerErrorPreservesRequestId() {
		when(this.jsonRpcHandler.onMessageSendStream(any(), any())).thenThrow(new TaskNotFoundError());

		Object result = this.requestHandler.onHandler("""
				{
				  "jsonrpc": "2.0",
				  "id": "stream-request",
				  "method": "SendStreamingMessage",
				  "params": {
				    "message": {
				      "messageId": "message-id",
				      "role": "ROLE_USER",
				      "parts": [{ "text": "hello" }]
				    }
				  }
				}
				""", this.headers);

		A2AErrorResponse response = assertInstanceOf(A2AErrorResponse.class, result);
		assertEquals("stream-request", response.getId());
	}

}
