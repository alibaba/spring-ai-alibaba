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
Nacos A2A Registration Module

This module provides utilities for registering Python A2A agents with Nacos 3.x.
It supports the URL registration mode which is recommended for non-Java services.

Usage:
    registry = NacosA2aRegistry(server_addr="127.0.0.1:8848")
    registry.register_agent_url(agent_card)
"""

import json
import logging
from typing import Optional
from dataclasses import dataclass, field, asdict

import requests

logger = logging.getLogger(__name__)


@dataclass
class AgentCapabilities:
    """A2A Agent capabilities."""
    streaming: bool = True
    pushNotifications: bool = False
    stateTransitionHistory: bool = False


@dataclass
class AgentSkill:
    """A2A Agent skill definition."""
    id: str
    name: str
    description: str
    inputModes: list[str] = field(default_factory=lambda: ["text/plain"])
    outputModes: list[str] = field(default_factory=lambda: ["text/plain"])
    tags: list[str] = field(default_factory=list)
    examples: list[str] = field(default_factory=list)


@dataclass
class AgentProvider:
    """A2A Agent provider information."""
    organization: str
    url: str = ""


@dataclass
class AgentCard:
    """
    A2A Agent Card - metadata for agent registration and discovery.

    This follows the A2A protocol specification (version 0.2.5).
    """
    name: str
    description: str
    url: str  # The message endpoint URL (e.g., http://host:port/a2a)
    version: str = "1.0.0"
    protocolVersion: str = "0.2.5"
    preferredTransport: str = "JSONRPC"
    capabilities: AgentCapabilities = field(default_factory=AgentCapabilities)
    defaultInputModes: list[str] = field(default_factory=lambda: ["text/plain"])
    defaultOutputModes: list[str] = field(default_factory=lambda: ["text/plain"])
    skills: list[AgentSkill] = field(default_factory=list)
    provider: Optional[AgentProvider] = None
    iconUrl: Optional[str] = None
    documentationUrl: Optional[str] = None
    supportsAuthenticatedExtendedCard: bool = False

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization."""
        data = {
            "protocolVersion": self.protocolVersion,
            "name": self.name,
            "description": self.description,
            "version": self.version,
            "url": self.url,
            "preferredTransport": self.preferredTransport,
            "capabilities": asdict(self.capabilities),
            "defaultInputModes": self.defaultInputModes,
            "defaultOutputModes": self.defaultOutputModes,
            "supportsAuthenticatedExtendedCard": self.supportsAuthenticatedExtendedCard,
        }

        if self.skills:
            data["skills"] = [asdict(s) for s in self.skills]

        if self.provider:
            data["provider"] = asdict(self.provider)

        if self.iconUrl:
            data["iconUrl"] = self.iconUrl

        if self.documentationUrl:
            data["documentationUrl"] = self.documentationUrl

        return data


class NacosA2aRegistry:
    """
    Nacos A2A Registry client for Python agents.

    This class provides methods to register Python A2A agents with Nacos 3.x
    using the Admin HTTP API.

    Attributes:
        server_addr: Nacos server address (host:port)
        namespace: Nacos namespace (default: "public")
        context_path: Nacos context path (default: "/nacos")
    """

    def __init__(
        self,
        server_addr: str = "127.0.0.1:8848",
        namespace: str = "public",
        username: str = "nacos",
        password: str = "nacos",
        context_path: str = "/nacos",
        timeout: int = 10,
    ):
        self.server_addr = server_addr
        self.namespace = namespace
        self.context_path = context_path.rstrip("/")
        self.timeout = timeout
        self._access_token: Optional[str] = None
        self._username = username
        self._password = password

    @property
    def base_url(self) -> str:
        return f"http://{self.server_addr}{self.context_path}"

    def _ensure_token(self) -> str:
        """Ensure we have a valid access token."""
        if self._access_token is None:
            self._access_token = self._login()
        return self._access_token

    def _login(self) -> str:
        """
        Authenticate with Nacos and get access token.

        Nacos 3.x login endpoint: POST {contextPath}/v3/auth/user/login
        """
        url = f"{self.base_url}/v3/auth/user/login"
        logger.info(f"Logging into Nacos at {url}")

        try:
            resp = requests.post(
                url,
                data={"username": self._username, "password": self._password},
                timeout=self.timeout,
            )
            resp.raise_for_status()
            token = resp.json().get("accessToken")
            if not token:
                raise ValueError("No accessToken in response")
            logger.info("Successfully authenticated with Nacos")
            return token
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to login to Nacos: {e}")
            raise

    def register_agent_url(
        self,
        agent_card: AgentCard,
        registration_type: str = "URL",
    ) -> dict:
        """
        Register an agent with Nacos using URL mode.

        This is the recommended approach for non-Java services.
        SAA will use the `url` field in AgentCard to make HTTP calls.

        Args:
            agent_card: The AgentCard to register
            registration_type: Registration type (URL or SERVICE)

        Returns:
            Nacos API response

        HTTP API:
            POST {contextPath}/v3/admin/ai/a2a
            Content-Type: application/x-www-form-urlencoded
        """
        token = self._ensure_token()
        url = f"{self.base_url}/v3/admin/ai/a2a"

        headers = {"accessToken": token}
        data = {
            "namespaceId": self.namespace,
            "agentName": agent_card.name,
            "registrationType": registration_type,
            "agentCard": json.dumps(agent_card.to_dict(), ensure_ascii=False),
        }

        logger.info(f"Registering agent '{agent_card.name}' to Nacos at {url}")
        logger.debug(f"AgentCard: {data['agentCard']}")

        try:
            resp = requests.post(url, headers=headers, data=data, timeout=self.timeout)
            resp.raise_for_status()
            result = resp.json()
            logger.info(f"Successfully registered agent '{agent_card.name}'")
            return result
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to register agent: {e}")
            raise

    def deregister_agent(self, agent_name: str) -> dict:
        """
        Deregister an agent from Nacos.

        Args:
            agent_name: The name of the agent to deregister

        Returns:
            Nacos API response
        """
        token = self._ensure_token()
        url = f"{self.base_url}/v3/admin/ai/a2a"

        headers = {"accessToken": token}
        params = {
            "namespaceId": self.namespace,
            "agentName": agent_name,
        }

        logger.info(f"Deregistering agent '{agent_name}' from Nacos")

        try:
            resp = requests.delete(
                url, headers=headers, params=params, timeout=self.timeout
            )
            resp.raise_for_status()
            result = resp.json()
            logger.info(f"Successfully deregistered agent '{agent_name}'")
            return result
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to deregister agent: {e}")
            raise

    def get_agent_card(self, agent_name: str) -> Optional[dict]:
        """
        Get an agent card from Nacos.

        Args:
            agent_name: The name of the agent

        Returns:
            AgentCard dict or None if not found
        """
        token = self._ensure_token()
        url = f"{self.base_url}/v3/admin/ai/a2a"

        headers = {"accessToken": token}
        params = {
            "namespaceId": self.namespace,
            "agentName": agent_name,
        }

        try:
            resp = requests.get(
                url, headers=headers, params=params, timeout=self.timeout
            )
            resp.raise_for_status()
            return resp.json()
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to get agent card: {e}")
            return None
