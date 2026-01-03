#
# Copyright 2024-2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
A2A Protocol Server Implementation

This module implements the A2A (Agent-to-Agent) protocol server endpoints
for Python agents, compatible with Spring AI Alibaba.

Endpoints:
    GET  /.well-known/agent.json  - Returns the AgentCard
    POST /a2a                      - Handles JSON-RPC 2.0 messages

Protocol: A2A 0.2.5 with JSON-RPC 2.0 transport
"""

import json
import uuid
import asyncio
import logging
from typing import AsyncGenerator, Callable, Awaitable, Optional
from dataclasses import dataclass

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse, StreamingResponse

logger = logging.getLogger(__name__)


# Type alias for agent handler function
AgentHandler = Callable[[str, dict], Awaitable[str]]


@dataclass
class A2aServerConfig:
    """Configuration for A2A server."""
    agent_card: dict
    agent_handler: AgentHandler
    streaming_enabled: bool = True


class A2aProtocolError(Exception):
    """A2A protocol error."""

    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message
        super().__init__(message)


def create_jsonrpc_response(request_id: str, result: dict) -> dict:
    """Create a JSON-RPC 2.0 response."""
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": result,
    }


def create_jsonrpc_error(request_id: str, code: int, message: str) -> dict:
    """Create a JSON-RPC 2.0 error response."""
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "error": {
            "code": code,
            "message": message,
        },
    }


def extract_text_from_message(message: dict) -> str:
    """Extract text content from A2A message parts."""
    parts = message.get("parts", [])
    texts = []
    for part in parts:
        if part.get("kind") == "text":
            texts.append(part.get("text", ""))
    return "".join(texts)


def extract_metadata(params: dict) -> dict:
    """Extract metadata from request params."""
    metadata = params.get("metadata", {})
    return {
        "thread_id": metadata.get("threadId"),
        "user_id": metadata.get("userId"),
    }


async def generate_streaming_response(
    request_id: str,
    text: str,
    metadata: dict,
    handler: AgentHandler,
) -> AsyncGenerator[str, None]:
    """
    Generate SSE streaming response for A2A message/stream.

    The response follows A2A protocol:
    1. status-update: working
    2. artifact-update: response content
    3. status-update: completed
    """
    # 1. Status: working
    yield f"data: {json.dumps(create_jsonrpc_response(request_id, {'kind': 'status-update', 'status': {'state': 'working'}}))}\n\n"

    try:
        # 2. Execute agent logic
        response_text = await handler(text, metadata)

        # 3. Artifact: return result
        artifact = {
            "kind": "artifact-update",
            "artifact": {
                "parts": [{"kind": "text", "text": response_text}],
            },
        }
        yield f"data: {json.dumps(create_jsonrpc_response(request_id, artifact))}\n\n"

        # 4. Status: completed
        yield f"data: {json.dumps(create_jsonrpc_response(request_id, {'kind': 'status-update', 'status': {'state': 'completed'}}))}\n\n"

    except Exception as e:
        logger.exception(f"Error in agent handler: {e}")
        # Status: failed
        yield f"data: {json.dumps(create_jsonrpc_response(request_id, {'kind': 'status-update', 'status': {'state': 'failed', 'message': {'parts': [{'kind': 'text', 'text': str(e)}]}}}))}\n\n"


async def handle_non_streaming(
    request_id: str,
    text: str,
    metadata: dict,
    handler: AgentHandler,
) -> dict:
    """Handle non-streaming A2A message/send."""
    try:
        response_text = await handler(text, metadata)

        return create_jsonrpc_response(
            request_id,
            {
                "kind": "task",
                "taskId": str(uuid.uuid4()),
                "status": {"state": "completed"},
                "artifacts": [{"parts": [{"kind": "text", "text": response_text}]}],
            },
        )
    except Exception as e:
        logger.exception(f"Error in agent handler: {e}")
        return create_jsonrpc_response(
            request_id,
            {
                "kind": "task",
                "taskId": str(uuid.uuid4()),
                "status": {
                    "state": "failed",
                    "message": {"parts": [{"kind": "text", "text": str(e)}]},
                },
            },
        )


def setup_a2a_routes(app: FastAPI, config: A2aServerConfig) -> None:
    """
    Setup A2A protocol routes on a FastAPI application.

    Args:
        app: FastAPI application instance
        config: A2A server configuration
    """

    @app.get("/.well-known/agent.json")
    async def get_agent_card():
        """Return the AgentCard for this agent."""
        return JSONResponse(content=config.agent_card)

    @app.post("/a2a")
    async def handle_a2a_message(request: Request):
        """
        Handle A2A JSON-RPC 2.0 messages.

        Supported methods:
        - message/send: Non-streaming message
        - message/stream: Streaming message (SSE)
        - task/get: Get task status (not implemented)
        - task/cancel: Cancel task (not implemented)
        """
        try:
            body = await request.json()
        except json.JSONDecodeError:
            return JSONResponse(
                content=create_jsonrpc_error(None, -32700, "Parse error"),
                status_code=400,
            )

        request_id = body.get("id", str(uuid.uuid4()))
        method = body.get("method")
        params = body.get("params", {})

        logger.info(f"Received A2A request: method={method}, id={request_id}")

        if method == "message/stream":
            if not config.streaming_enabled:
                return JSONResponse(
                    content=create_jsonrpc_error(
                        request_id, -32601, "Streaming not supported"
                    )
                )

            message = params.get("message", {})
            text = extract_text_from_message(message)
            metadata = extract_metadata(params)

            return StreamingResponse(
                generate_streaming_response(
                    request_id, text, metadata, config.agent_handler
                ),
                media_type="text/event-stream",
            )

        elif method == "message/send":
            message = params.get("message", {})
            text = extract_text_from_message(message)
            metadata = extract_metadata(params)

            result = await handle_non_streaming(
                request_id, text, metadata, config.agent_handler
            )
            return JSONResponse(content=result)

        elif method == "task/get":
            # Task status query - not implemented for stateless agents
            return JSONResponse(
                content=create_jsonrpc_error(
                    request_id, -32601, "task/get not implemented"
                )
            )

        elif method == "task/cancel":
            # Task cancellation - not implemented for stateless agents
            return JSONResponse(
                content=create_jsonrpc_error(
                    request_id, -32601, "task/cancel not implemented"
                )
            )

        else:
            return JSONResponse(
                content=create_jsonrpc_error(
                    request_id, -32601, f"Method not found: {method}"
                )
            )


def create_a2a_app(config: A2aServerConfig) -> FastAPI:
    """
    Create a FastAPI application with A2A protocol support.

    Args:
        config: A2A server configuration

    Returns:
        Configured FastAPI application
    """
    app = FastAPI(
        title=config.agent_card.get("name", "A2A Agent"),
        description=config.agent_card.get("description", "A2A Protocol Agent"),
        version=config.agent_card.get("version", "1.0.0"),
    )

    setup_a2a_routes(app, config)

    return app
