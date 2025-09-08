/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.strategy.impl;

import com.alibaba.cloud.ai.common.McpTransportType;
import com.alibaba.cloud.ai.container.McpClientContainer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SSETransportProcesser extends AbstractTransport{

    private final Logger log = LoggerFactory.getLogger(SSETransportProcesser.class);

    @Autowired
    private McpClientContainer mcpClientContainer;

    @Override
    protected McpTransportType getTransportType() {
        return McpTransportType.SSE;
    }
    @Override
    public McpSyncClient connect(ServerParameters serverParameters) {
        //去连接对应的mcpServer
        log.info("current command is {} , current args is {}" , serverParameters.getCommand(), serverParameters.getArgs());
        McpSyncClient mcpStdioClient = McpClient.sync(
                new HttpClientSseClientTransport()
        ).build();
        return mcpStdioClient;
    }

    @Override
    public String getClientName() {
        return  "SSE_" + counter.incrementAndGet();
    }
}
