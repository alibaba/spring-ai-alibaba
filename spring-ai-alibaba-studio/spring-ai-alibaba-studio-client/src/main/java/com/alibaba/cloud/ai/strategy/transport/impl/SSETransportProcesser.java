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

package com.alibaba.cloud.ai.strategy.transport.impl;

import com.alibaba.cloud.ai.common.McpTransportType;
import com.alibaba.cloud.ai.container.McpClientContainer;
import com.alibaba.cloud.ai.domain.McpConnectRequest;
import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SSETransportProcesser extends AbstractTransport {

    private final Logger log = LoggerFactory.getLogger(SSETransportProcesser.class);

    @Autowired
    private McpClientContainer mcpClientContainer;

    @Override
    protected McpTransportType getTransportType() {
        return McpTransportType.SSE;
    }
    @Override
    public McpSyncClient connect(McpConnectRequest mcpConnectRequest) {
        //去连接对应的mcpServer
        return null;
    }

    @Override
    public String getClientName() {
        return  "SSE_" + counter.incrementAndGet();
    }
}
