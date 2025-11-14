package com.alibaba.cloud.ai.graph.agent.documentation;

/**
 * Workflow Advanced Example - 完整代码示例
 * 展示如何使用StateGraph构建工作流和自定义Node
 *
 * 来源：advanced/workflow.md
 */
public class WorkflowExample {

    public static void main(String[] args) {
        System.out.println("=== Workflow (Graph) Advanced Examples ===");
        System.out.println();
        System.out.println("Graph核心概念：");
        System.out.println("1. 状态（State） - 在Node与Edge之间传递的数据结构");
        System.out.println("2. 节点（Node） - Graph中的执行逻辑单元");
        System.out.println("3. 边（Edge） - 定义Node间的控制流");
        System.out.println();
        System.out.println("Node开发最佳实践：");
        System.out.println("1. 单一职责 - 每个Node只负责一个明确任务");
        System.out.println("2. 状态不可变 - 返回新状态而非修改输入");
        System.out.println("3. 异常处理 - 在Node内部处理可预见异常");
        System.out.println("4. 日志记录 - 添加适当日志便于调试");
        System.out.println("5. 参数验证 - 处理前验证状态参数");
        System.out.println();
        System.out.println("详细代码示例请参考：");
        System.out.println("- advanced/workflow.md");
        System.out.println("- Graph API文档");
    }
}

