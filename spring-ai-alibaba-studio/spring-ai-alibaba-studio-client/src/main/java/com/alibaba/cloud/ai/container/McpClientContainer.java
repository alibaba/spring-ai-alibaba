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

package com.alibaba.cloud.ai.container;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//记得关闭所有的资源，因为这里mcpClient连接会有其他的资源
@Component
public class McpClientContainer implements DisposableBean {

    private final Map<String , McpSyncClient> mcpClients = new ConcurrentHashMap<>();

    public void add(String clientName , McpSyncClient mcpClient) {
        mcpClients.put(clientName, mcpClient);
    }

    public void remove(String clientName){
        mcpClients.remove(clientName);
    }

    public McpSyncClient get(String clientName){
        if(!mcpClients.containsKey(clientName)){
            throw new RuntimeException("Unknown client: " + clientName);
        }
        return mcpClients.get(clientName);

    }

    @Override
    public void destroy() throws Exception {
        mcpClients.values().forEach(McpSyncClient::closeGracefully);
    }
}
