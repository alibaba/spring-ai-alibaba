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

package com.alibaba.cloud.ai.examples.documentation.framework.tutorials.mcp;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示Controller
 *
 * @author NGshiyu
 */
@RestController
@RequestMapping("/mcpToolsExample")
public class RemoteMcpToolsExampleController {

    @Autowired
    RemoteMcpToolsExample remoteMcpToolsExample;

    @PostMapping("/mcpWithReactSpring")
    public void callRemoteMcpToolsWithSpringBootExample() throws GraphRunnerException {
        remoteMcpToolsExample.remoteMcpToolsReactWithSpringBootExample();
    }

    @PostMapping("/mcpWithReact")
    public void remoteMcpToolsReactWithoutSpringBootExample() throws GraphRunnerException {
        remoteMcpToolsExample.remoteMcpToolsReactWithoutSpringBootExample();
    }

    @PostMapping("/mcpWithSpringChat")
    public void remoteMcpToolsWithChatCliAndSpringBootExample() {
        remoteMcpToolsExample.remoteMcpToolsWithChatCliAndSpringBootExample();
    }
}
