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

import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.HEADERS_KEY;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;

import org.springframework.web.servlet.function.ServerRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;

import com.google.gson.JsonSyntaxException;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.jsonrpc.common.json.IdJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.InvalidParamsJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.MethodNotFoundJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AErrorResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2ARequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.NonStreamingJSONRPCRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendStreamingMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.MethodNotFoundError;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.reactivestreams.FlowAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

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
		A2ARequest<?> request = null;
		try {
			request = JSONRPCUtils.parseRequestBody(body, null);
			ServerCallContext context = createCallContext(headers);
			context.getState().put(METHOD_NAME_KEY, request.getMethod());
			return request instanceof NonStreamingJSONRPCRequest<?> nonStreamingRequest
					? handleNonStreamRequest(nonStreamingRequest, context) : handleStreamRequest(request, context);
		}
		catch (InvalidParamsJsonMappingException e) {
			return new A2AErrorResponse(e.getId(), new InvalidParamsError(null, e.getMessage(), null));
		}
		catch (MethodNotFoundJsonMappingException e) {
			return new A2AErrorResponse(e.getId(), new MethodNotFoundError(null, e.getMessage(), null));
		}
		catch (IdJsonMappingException e) {
			return new A2AErrorResponse(e.getId(), new InvalidRequestError(null, e.getMessage(), null));
		}
		catch (JsonMappingException e) {
			return new A2AErrorResponse(new InvalidRequestError(null, e.getMessage(), null));
		}
		catch (JsonSyntaxException | JsonProcessingException e) {
			return new A2AErrorResponse(new JSONParseError(e.getMessage()));
		}
		catch (A2AError e) {
			return request != null ? generateErrorResponse(request, e) : new A2AErrorResponse(e);
		}
		catch (Exception ex) {
			LOGGER.error("Unexpected error while handling an A2A JSON-RPC request", ex);
			InternalError error = new InternalError("Internal server error");
			return request != null ? generateErrorResponse(request, error) : new A2AErrorResponse(error);
		}
	}

	private Flux<?> handleStreamRequest(A2ARequest<?> request, ServerCallContext context) {
		Flow.Publisher<? extends A2AResponse<?>> publisher;
		if (request instanceof SendStreamingMessageRequest req) {
			SendStreamingMessageRequest newRequest = SendStreamingMessageRequest.builder()
				.id(req.getId())
				.jsonrpc(req.getJsonrpc())
				.params(injectStreamMetadata(req.getParams(), true))
				.build();
			publisher = jsonRpcHandler.onMessageSendStream(newRequest, context);
			LOGGER.debug("Created stream publisher {}", publisher);
		}
		else if (request instanceof SubscribeToTaskRequest req) {
			publisher = jsonRpcHandler.onSubscribeToTask(req, context);
		}
		else {
			return Flux.just(generateErrorResponse(request, new UnsupportedOperationError()));
		}
		return Flux.from(FlowAdapters.toPublisher(publisher));
	}

	private A2AResponse<?> handleNonStreamRequest(NonStreamingJSONRPCRequest<?> request, ServerCallContext context) {
		if (request instanceof GetTaskRequest req) {
			return jsonRpcHandler.onGetTask(req, context);
		}
		else if (request instanceof SendMessageRequest req) {
			SendMessageRequest newRequest = SendMessageRequest.builder()
				.id(req.getId())
				.jsonrpc(req.getJsonrpc())
				.params(injectStreamMetadata(req.getParams(), false))
				.build();
			return jsonRpcHandler.onMessageSend(newRequest, context);
		}
		else if (request instanceof CancelTaskRequest req) {
			return jsonRpcHandler.onCancelTask(req, context);
		}
		else if (request instanceof GetTaskPushNotificationConfigRequest req) {
			return jsonRpcHandler.getPushNotificationConfig(req, context);
		}
		else if (request instanceof CreateTaskPushNotificationConfigRequest req) {
			return jsonRpcHandler.setPushNotificationConfig(req, context);
		}
		else if (request instanceof ListTaskPushNotificationConfigsRequest req) {
			return jsonRpcHandler.listPushNotificationConfigs(req, context);
		}
		else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
			return jsonRpcHandler.deletePushNotificationConfig(req, context);
		}
		else if (request instanceof ListTasksRequest req) {
			return jsonRpcHandler.onListTasks(req, context);
		}
		else if (request instanceof GetExtendedAgentCardRequest req) {
			return jsonRpcHandler.onGetExtendedCardRequest(req, context);
		}
		else {
			return generateErrorResponse(request, new UnsupportedOperationError());
		}
	}

	private static A2AErrorResponse generateErrorResponse(A2ARequest<?> request, A2AError error) {
		return new A2AErrorResponse(request.getId(), error);
	}

	private MessageSendParams injectStreamMetadata(MessageSendParams original, boolean isStreaming) {
		Map<String, Object> metadata = new HashMap<>();
		if (original.metadata() != null) {
			metadata.putAll(original.metadata());
		}
		metadata.put(GraphAgentExecutor.STREAMING_METADATA_KEY, isStreaming);
		return MessageSendParams.builder().configuration(original.configuration())
			.message(original.message())
			.metadata(metadata)
			.tenant(original.tenant())
			.build();
	}

	private ServerCallContext createCallContext(ServerRequest.Headers headers) {
		Map<String, Object> state = new HashMap<>();
		state.put(TRANSPORT_KEY, TransportProtocol.JSONRPC);
		state.put(HEADERS_KEY, headers.asHttpHeaders().toSingleValueMap());

		Set<String> requestedExtensions = new HashSet<>();
		headers.asHttpHeaders().getOrEmpty(A2AHeaders.A2A_EXTENSIONS).stream()
			.flatMap(extensionsHeader -> Arrays.stream(extensionsHeader.split(",")))
				.map(String::trim)
				.filter(extension -> !extension.isEmpty())
				.forEach(requestedExtensions::add);
		return new ServerCallContext(UnauthenticatedUser.INSTANCE, state, requestedExtensions,
				headers.firstHeader(A2AHeaders.A2A_VERSION));
	}

}
