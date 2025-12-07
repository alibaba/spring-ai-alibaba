---
title: Graph 执行取消
description: 使用 Spring AI Alibaba Graph 框架取消工作流执行，支持优雅停止和立即中断
keywords: [Spring AI Alibaba, Graph, 取消执行, AsyncGenerator, 流式处理, 工作流取消]
---

# Spring AI Alibaba Graph - 图执行取消

Spring AI Alibaba Graph 提供了强大的图执行取消机制，这对于长时间运行的工作流程特别有用。此功能基于 `java-async-generator` 库的取消能力构建。

## 取消图流

当您使用 `stream` 方法执行图时，会得到一个 `AsyncGenerator` 实例。此生成器可以被取消，允许您优雅地或立即停止图的执行。

要取消流，需要在生成器上调用 `cancel(boolean mayInterruptIfRunning)` 方法。

在下面的示例中，我们从与主线程不同的线程发起取消请求来测试取消功能。

### 使用 `forEachAsync` 消费流

当您使用 `forEachAsync` 消费流时，图执行在单独的线程中运行。

- **`cancel(true)` (立即取消):** 这将中断执行线程，导致 `forEachAsync` 返回的 `CompletableFuture` 以 `InterruptedException` 异常完成。

- **`cancel(false)` (优雅取消):** 这将让当前正在执行的节点完成，然后在启动下一个节点之前停止执行。

以下是一个演示立即取消的示例：

<Code
  language="java"
  title="立即取消示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CancellationExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

// 编译并获取图实例
CompiledGraph compiledGraph = stateGraph.compile();

// 创建运行配置
RunnableConfig runnableConfig = RunnableConfig.builder()
        .threadId("test-thread")
        .build();

// 准备输入数据
Map<String, Object> inputData = new HashMap<>();
// ... 添加输入数据

// 执行图并获取流
Flux<NodeOutput> stream = compiledGraph.stream(inputData, runnableConfig);

// 从新线程在 500 毫秒后请求取消
CompletableFuture.runAsync(() -> {
    try {
        Thread.sleep(500);
        // Flux 使用 dispose() 来取消
        System.out.println("请求取消执行");
    }
    catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
});

// 异步处理每个输出
var disposable = stream.subscribe(
        output -> System.out.println("当前迭代节点: " + output),
        error -> System.out.println("流错误: " + error.getMessage()),
        () -> System.out.println("流完成")
);

// 等待流完成或取消
try {
    stream.blockLast();
}
catch (Exception e) {
    System.err.println("执行异常: " + e.getMessage());
}

// 验证是否已取消（Flux 使用 isDisposed 检查）
System.out.println("是否已取消: " + disposable.isDisposed());`}
</Code>

### 使用迭代器消费流

当您使用 `for-each` 循环遍历流时，执行在当前线程上运行。

- **`cancel(true)` 或 `cancel(false)`:** 两者都会导致迭代器的 `hasNext()` 方法返回 `false`，从而有效地停止循环。

以下是一个示例：

<Code
  language="java"
  title="使用迭代器消费流示例" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CancellationExample.java"
>
{`import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

// 编译并获取图实例
CompiledGraph compiledGraph = stateGraph.compile();

// 创建运行配置
RunnableConfig runnableConfig = RunnableConfig.builder()
        .threadId("test-thread")
        .build();

// 准备输入数据
Map<String, Object> inputData = new HashMap<>();
// ... 添加输入数据

// 执行图并获取流
Flux<NodeOutput> stream = compiledGraph.stream(inputData, runnableConfig);

// 从新线程在 500 毫秒后请求取消
var disposable = stream.subscribe(
        output -> {
            System.out.println("当前迭代节点: " + output);
        },
        error -> {
            System.out.println("流错误: " + error.getMessage());
        },
        () -> {
            System.out.println("流完成");
        }
);

CompletableFuture.runAsync(() -> {
    try {
        Thread.sleep(500);
        disposable.dispose(); // 取消流
        System.out.println("已请求取消执行");
    }
    catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
});

// 等待流完成或取消
try {
    stream.blockLast();
}
catch (Exception e) {
    System.out.println("流被中断: " + e.getMessage());
}

// 验证取消状态
System.out.println("是否已取消: " + disposable.isDisposed());`}
</Code>

## 检查取消状态

您可以通过在生成器上调用 `isCancelled()` 方法来检查流是否已被取消。

<Code
  language="java"
  title="检查取消状态" sourceUrl="https://github.com/alibaba/spring-ai-alibaba/tree/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/CancellationExample.java"
>
{`import reactor.core.Disposable;

public static void checkCancellationStatus(Disposable disposable) {
    if (disposable.isDisposed()) {
        System.out.println("流已被取消");
    }
    else {
        System.out.println("流仍在运行");
    }
}`}
</Code>

## 取消与子图

取消功能同样适用于嵌套图（子图）。如果您取消父图的执行，取消操作会传播到当前正在执行的任何子图。

## 进一步阅读

取消机制由 `java-async-generator` 库提供。有关底层取消机制的更深入说明，请参阅 `java-async-generator` 仓库中的 [CANCELLATION.md](https://github.com/bsorrentino/java-async-generator/blob/main/CANCELLATION.md) 文档。
