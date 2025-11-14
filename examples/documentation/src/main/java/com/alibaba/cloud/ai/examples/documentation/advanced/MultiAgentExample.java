package com.alibaba.cloud.ai.graph.agent.documentation;

/**
 * Multi-Agent Advanced Example - 完整代码示例
 * 展示多智能体协作模式
 *
 * 来源：advanced/multi-agent.md
 */
public class MultiAgentExample {

    public static void main(String[] args) {
        System.out.println("=== Multi-Agent Advanced Examples ===");
        System.out.println("Multi-agent协作模式：");
        System.out.println();
        System.out.println("1. Tool Calling（工具调用）");
        System.out.println("   - Supervisor Agent将其他Agent作为工具调用");
        System.out.println("   - 集中式控制流");
        System.out.println("   - 适用于：任务编排、结构化工作流");
        System.out.println();
        System.out.println("2. Handoffs（交接）");
        System.out.println("   - 顺序执行（SequentialAgent）");
        System.out.println("   - 并行执行（ParallelAgent）");
        System.out.println("   - 路由选择（LlmRoutingAgent）");
        System.out.println("   - 自定义流程（FlowAgent）");
        System.out.println();
        System.out.println("详细代码示例请参考：");
        System.out.println("- advanced/multi-agent.md");
        System.out.println("- AgentToolExample.java");
    }
}

