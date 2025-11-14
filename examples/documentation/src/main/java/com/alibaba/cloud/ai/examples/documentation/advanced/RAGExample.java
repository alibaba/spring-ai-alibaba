package com.alibaba.cloud.ai.graph.agent.documentation;

/**
 * RAG (Retrieval-Augmented Generation) Advanced Example - 完整代码示例
 * 展示检索增强生成的三种架构模式
 *
 * 来源：advanced/rag.md
 */
public class RAGExample {

    public static void main(String[] args) {
        System.out.println("=== RAG (Retrieval-Augmented Generation) Examples ===");
        System.out.println();
        System.out.println("RAG架构对比：");
        System.out.println();
        System.out.println("1. 两步RAG (2-Step RAG)");
        System.out.println("   控制性: ✅ 高 | 灵活性: ❌ 低 | 延迟: ⚡ 快");
        System.out.println("   场景: FAQ、文档机器人");
        System.out.println();
        System.out.println("2. Agentic RAG");
        System.out.println("   控制性: ❌ 低 | 灵活性: ✅ 高 | 延迟: ⏳ 可变");
        System.out.println("   场景: 研究助手、多工具访问");
        System.out.println();
        System.out.println("3. 混合RAG (Hybrid RAG)");
        System.out.println("   控制性: ⚖️ 中 | 灵活性: ⚖️ 中 | 延迟: ⏳ 可变");
        System.out.println("   场景: 领域特定问答+质量验证");
        System.out.println();
        System.out.println("详细代码示例请参考：advanced/rag.md");
    }
}

