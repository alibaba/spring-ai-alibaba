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

package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.common.McpTransportType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.modelcontextprotocol.client.transport.ServerParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McpClientConfig {
    private McpTransportType transportType;

    @JsonProperty("command")
    private String command;

    @JsonProperty("args")
    private List<String> args = new ArrayList<>();

    @JsonProperty("env")
    private Map<String, String> env;

    public McpClientConfig(String command, List<String> args, Map<String, String> env) {
        this.command = command;
        this.args = args;
        if (env != null && !env.isEmpty()) {
            this.env.putAll(env);
        }
    }
    public McpClientConfig(String command, List<String> args) {
        this(command, args, null);
    }
    public McpClientConfig(){

    }


    public McpTransportType getTransportType() {
        return transportType;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getArgs() {
        return args;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setTransportType(McpTransportType transportType) {
        this.transportType = transportType;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }
}
