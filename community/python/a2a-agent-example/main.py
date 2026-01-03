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
Python A2A Agent Example

This example demonstrates how to create a Python A2A agent that can be:
1. Registered with Nacos 3.x for service discovery
2. Called by Spring AI Alibaba (SAA) services via A2A protocol

Usage:
    # Start the agent server
    python main.py

    # Or with uvicorn directly
    uvicorn main:app --host 0.0.0.0 --port 8000
"""

import os
import logging
import asyncio
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from openai import AsyncOpenAI

from a2a_server import A2aServerConfig, create_a2a_app
from nacos_a2a import (
    NacosA2aRegistry,
    AgentCard,
    AgentCapabilities,
    AgentSkill,
    AgentProvider,
)

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

# Configuration from environment
AGENT_NAME = os.getenv("AGENT_NAME", "python-translator-agent")
AGENT_HOST = os.getenv("AGENT_HOST", "127.0.0.1")
AGENT_PORT = int(os.getenv("AGENT_PORT", "8000"))

NACOS_SERVER_ADDR = os.getenv("NACOS_SERVER_ADDR", "127.0.0.1:8848")
NACOS_NAMESPACE = os.getenv("NACOS_NAMESPACE", "public")
NACOS_USERNAME = os.getenv("NACOS_USERNAME", "nacos")
NACOS_PASSWORD = os.getenv("NACOS_PASSWORD", "nacos")
NACOS_ENABLED = os.getenv("NACOS_ENABLED", "true").lower() == "true"

# OpenAI compatible API configuration (e.g., DashScope)
OPENAI_API_KEY = os.getenv("DASHSCOPE_API_KEY", os.getenv("OPENAI_API_KEY", ""))
OPENAI_BASE_URL = os.getenv(
    "OPENAI_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"
)
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "qwen-plus")

# Initialize OpenAI client
openai_client = AsyncOpenAI(
    api_key=OPENAI_API_KEY,
    base_url=OPENAI_BASE_URL,
)


async def translator_agent_handler(text: str, metadata: dict) -> str:
    """
    Agent handler: Translate text between Chinese and English.

    This is the core logic of the agent. It receives user input and
    returns the agent's response.

    Args:
        text: User input text
        metadata: Request metadata (thread_id, user_id, etc.)

    Returns:
        Agent response text
    """
    logger.info(f"Processing request: {text[:100]}...")
    logger.debug(f"Metadata: {metadata}")

    system_prompt = """你是一个专业的翻译助手。你的任务是：
1. 如果用户输入的是中文，将其翻译成英文
2. 如果用户输入的是英文，将其翻译成中文
3. 如果用户有特殊的翻译要求（如翻译成其他语言），按照用户要求执行

请直接输出翻译结果，不需要额外的解释。如果无法判断源语言或目标语言，请询问用户。"""

    try:
        response = await openai_client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": text},
            ],
            temperature=0.3,
        )
        result = response.choices[0].message.content
        logger.info(f"Generated response: {result[:100]}...")
        return result

    except Exception as e:
        logger.exception(f"Error calling LLM: {e}")
        return f"抱歉，翻译服务暂时不可用：{str(e)}"


# Build AgentCard
agent_card = AgentCard(
    name=AGENT_NAME,
    description="Python 翻译 Agent - 支持中英文互译，可被 Spring AI Alibaba 通过 A2A 协议调用",
    url=f"http://{AGENT_HOST}:{AGENT_PORT}/a2a",
    version="1.0.0",
    capabilities=AgentCapabilities(
        streaming=True,
        pushNotifications=False,
        stateTransitionHistory=False,
    ),
    skills=[
        AgentSkill(
            id="translate",
            name="翻译",
            description="支持中英文互译，自动检测源语言",
            examples=[
                "翻译：Hello, how are you?",
                "Translate: 今天天气真好",
                "把这段话翻译成英文：人工智能正在改变世界",
            ],
        ),
    ],
    provider=AgentProvider(
        organization="Spring AI Alibaba Community",
        url="https://github.com/alibaba/spring-ai-alibaba",
    ),
)

# Create A2A server configuration
a2a_config = A2aServerConfig(
    agent_card=agent_card.to_dict(),
    agent_handler=translator_agent_handler,
    streaming_enabled=True,
)


@asynccontextmanager
async def lifespan(app):
    """Application lifespan handler for startup/shutdown events."""
    # Startup
    logger.info(f"Starting {AGENT_NAME} on {AGENT_HOST}:{AGENT_PORT}")

    if NACOS_ENABLED:
        try:
            registry = NacosA2aRegistry(
                server_addr=NACOS_SERVER_ADDR,
                namespace=NACOS_NAMESPACE,
                username=NACOS_USERNAME,
                password=NACOS_PASSWORD,
            )
            registry.register_agent_url(agent_card)
            logger.info(f"Registered agent '{AGENT_NAME}' with Nacos at {NACOS_SERVER_ADDR}")
        except Exception as e:
            logger.warning(f"Failed to register with Nacos: {e}")
            logger.warning("Agent will still run, but won't be discoverable via Nacos")
    else:
        logger.info("Nacos registration disabled")

    yield

    # Shutdown
    logger.info("Shutting down agent")
    if NACOS_ENABLED:
        try:
            registry = NacosA2aRegistry(
                server_addr=NACOS_SERVER_ADDR,
                namespace=NACOS_NAMESPACE,
                username=NACOS_USERNAME,
                password=NACOS_PASSWORD,
            )
            registry.deregister_agent(AGENT_NAME)
            logger.info(f"Deregistered agent '{AGENT_NAME}' from Nacos")
        except Exception as e:
            logger.warning(f"Failed to deregister from Nacos: {e}")


# Create FastAPI application
app = create_a2a_app(a2a_config)
app.router.lifespan_context = lifespan


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "agent": AGENT_NAME}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=AGENT_PORT,
        reload=False,
        log_level="info",
    )
