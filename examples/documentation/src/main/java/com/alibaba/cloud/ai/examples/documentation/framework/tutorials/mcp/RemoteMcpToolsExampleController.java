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
