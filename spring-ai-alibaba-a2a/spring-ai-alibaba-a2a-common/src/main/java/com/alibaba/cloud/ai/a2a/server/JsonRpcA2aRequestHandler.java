/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.a2a.server;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.NonStreamingJSONRPCRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.util.Utils;
import org.reactivestreams.FlowAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.web.servlet.function.ServerRequest;

/**
 * The request handler for A2A protocol request by JSON RPC 2.0.
 *
 * @author xiweng.yy
 */
public class JsonRpcA2aRequestHandler implements A2aRequestHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonRpcA2aRequestHandler.class);

	private final JSONRPCHandler jsonRpcHandler;

	public JsonRpcA2aRequestHandler(JSONRPCHandler jsonRpcHandler) {
		this.jsonRpcHandler = jsonRpcHandler;
	}

	@Override
	public AgentCard getAgentCard() {
		return jsonRpcHandler.getAgentCard();
	}

	@Override
	public Object onHandler(String body, ServerRequest.Headers headers) {
		boolean streaming = isStreamingRequest(body);
		Object result = null;
		try {
			result = streaming ? handleStreamRequest(body) : handleNonStreamRequest(body);
		}
		catch (JsonProcessingException e) {
			result = new JSONRPCErrorResponse(null, new JSONParseError());
		}
		return result;
	}

	private static boolean isStreamingRequest(String requestBody) {
		try {
			JsonNode node = Utils.OBJECT_MAPPER.readTree(requestBody);
			JsonNode method = node != null ? node.get("method") : null;
			return method != null && (SendStreamingMessageRequest.METHOD.equals(method.asText())
					|| TaskResubscriptionRequest.METHOD.equals(method.asText()));
		}
		catch (Exception e) {
			return false;
		}
	}

	private Flux<?> handleStreamRequest(String body) throws JsonProcessingException {
		StreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, StreamingJSONRPCRequest.class);
		Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
		if (request instanceof SendStreamingMessageRequest req) {
			SendStreamingMessageRequest.Builder newReqBuilder = new SendStreamingMessageRequest.Builder()
				.id(req.getId())
				.jsonrpc(req.getJsonrpc())
				.method(req.getMethod())
				.params(injectStreamMetadata(req.getParams(), true));
			publisher = jsonRpcHandler.onMessageSendStream(newReqBuilder.build());
			LOGGER.info("get Stream publisher {}", publisher);
		}
		else if (request instanceof TaskResubscriptionRequest req) {
			publisher = jsonRpcHandler.onResubscribeToTask(req);
		}
		else {
			return Flux.just(generateErrorResponse(request, new UnsupportedOperationError()));
		}
		return Flux.from(FlowAdapters.toPublisher(publisher))
			.map((Function<JSONRPCResponse<?>, JSONRPCResponse<?>>) jsonrpcResponse -> jsonrpcResponse)
			.delaySubscription(Duration.ofMillis(10));
	}

	private JSONRPCResponse<?> handleNonStreamRequest(String body) throws JsonProcessingException {
		NonStreamingJSONRPCRequest<?> request = Utils.OBJECT_MAPPER.readValue(body, NonStreamingJSONRPCRequest.class);
		if (request instanceof GetTaskRequest req) {
			return jsonRpcHandler.onGetTask(req);
		}
		else if (request instanceof SendMessageRequest req) {
			SendMessageRequest.Builder newReqBuilder = new SendMessageRequest.Builder().id(req.getId())
				.jsonrpc(req.getJsonrpc())
				.method(req.getMethod())
				.params(injectStreamMetadata(req.getParams(), false));
			return jsonRpcHandler.onMessageSend(newReqBuilder.build());
		}
		else if (request instanceof CancelTaskRequest req) {
			return jsonRpcHandler.onCancelTask(req);
		}
		else if (request instanceof GetTaskPushNotificationConfigRequest req) {
			return jsonRpcHandler.getPushNotificationConfig(req);
		}
		else if (request instanceof SetTaskPushNotificationConfigRequest req) {
			return jsonRpcHandler.setPushNotificationConfig(req);
		}
		else if (request instanceof ListTaskPushNotificationConfigRequest req) {
			return jsonRpcHandler.listPushNotificationConfig(req);
		}
		else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
			return jsonRpcHandler.deletePushNotificationConfig(req);
		}
		else {
			return generateErrorResponse(request, new UnsupportedOperationError());
		}
	}

	private static JSONRPCErrorResponse generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
		return new JSONRPCErrorResponse(request.getId(), error);
	}

	private MessageSendParams injectStreamMetadata(MessageSendParams original, boolean isStreaming) {
		if (null == original.metadata()) {
			MessageSendParams.Builder newBuilder = new MessageSendParams.Builder();
			newBuilder.configuration(original.configuration());
			newBuilder.metadata(Map.of(GraphAgentExecutor.STREAMING_METADATA_KEY, isStreaming));
			newBuilder.message(original.message());
			return newBuilder.build();
		}
		else {
			original.metadata().put(GraphAgentExecutor.STREAMING_METADATA_KEY, isStreaming);
			return original;
		}
	}

}
