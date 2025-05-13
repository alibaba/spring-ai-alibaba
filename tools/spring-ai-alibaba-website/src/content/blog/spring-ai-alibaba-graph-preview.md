---
title: 仅用十几行代码实现 OpenManus，Spring AI Alibaba Graph 快速预览
keywords: [Langgraph, OpenManus, Spring Ai, Spring AI Alibaba, 工作流, 多智能体, multi-agent]
description: Spring AI Alibaba Graph 可帮助开发者轻松开发工作流（workflow）与多智能体（multi-agent）系统，本文通过三个示例展示其在客户评价处理、天气查询及 OpenManus 实现中的应用，大幅简化流程编排复杂度。
author: 刘军
date: "2025-04-21"
category: article
---

Spring AI Alibaba Graph 的核心内容开发已基本就绪，将在近期发布正式版本，基于 Spring AI Alibaba Graph 开发者可以轻松开发工作流、不同模式的智能体&多智能体等系统，在 Spring AI ChatClient 基础上给开发者带来灵活的选择与更丰富的功能。

跟着这篇文章，我们将以三个示例形式为大家展示如何使用 Spring AI Alibaba 开发工作流、智能体应用，几行代码即可实现智能体编排：

1. 示例一：一个客户评价处理系统（基于工作流编排实现）
2. 示例二：基于 ReAct Agent 模式的天气预报查询系统
3. 示例三：基于 Supervisor 多智能体的 OpenManus 实现


Spring AI Alibaba Graph 内核与示例完整源码请参见：[https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph](https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph)

## 示例一：基于工作流编排的客户评价处理系统
以下是系统架构图：

![Spring AI Alibaba Graph Workflow](/img/blog/graph-preview/workflow-comment-review.png)

本示例实现了一个客户评价处理系统，系统首先接收用户评论，根据评论内容自动进行问题分类，总共有两级问题分类：

1. 第一级分类节点，将评论分为 positive 和 negative 两种。如果是 positive 评论则进行系统记录后结束流程；如果是 negative 评论则进行第二级分类。
2. 第二级分类节点，根据 negative 评论的具体内容识别用户的具体问题，如 "after-sale service"、"product quality"、"transportation" 等，根据具体问题分流到具体的问题处理节点。
3. 最后问题处理节点进行处理并记录后，流程结束。



核心代码展示：

```java
AgentStateFactory<OverAllState> stateFactory = (inputs) -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("input", new ReplaceStrategy());
			state.registerKeyAndStrategy("classifier_output", new ReplaceStrategy());
			state.registerKeyAndStrategy("solution", new ReplaceStrategy());
			state.input(inputs);
			return state;
		};

StateGraph stateGraph = new StateGraph("Consumer Service Workflow Demo", stateFactory)
			.addNode("feedback_classifier", node_async(feedbackClassifier))
			.addNode("specific_question_classifier", node_async(specificQuestionClassifier))
			.addNode("recorder", node_async(new RecordingNode()))

			.addEdge(START, "feedback_classifier")
			.addConditionalEdges("feedback_classifier",
					edge_async(new CustomerServiceController.FeedbackQuestionDispatcher()),
					Map.of("positive", "recorder", "negative", "specific_question_classifier"))
			.addConditionalEdges("specific_question_classifier",
					edge_async(new CustomerServiceController.SpecificQuestionDispatcher()),
					Map.of("after-sale", "recorder", "transportation", "recorder", "quality", "recorder", "others",
							"recorder"))
			.addEdge("recorder", END);
```



可下载本示例源码并运行，打开浏览器访问如下示例链接，查看运行效果：

+ [http://localhost:18080/customer/chat?query=我收到的产品有快递破损，需要退换货？](http://localhost:18080/customer/chat?query=我收到的产品有快递破损，需要退换货？)
+ [http://localhost:18080/customer/chat?query=我的产品不能正常工作了，要怎么去做维修？](http://localhost:18080/customer/chat?query=我的产品不能正常工作了，要怎么去做维修？)
+ [http://localhost:18080/customer/chat?query=商品收到了，非常好，下次还会买。](http://localhost:18080/customer/chat?query=商品收到了，非常好，下次还会买。)

## 示例二：基于 ReAct Agent 的天气预报查询系统
以下是 React Agent 架构图：

![Spring AI Alibaba Graph react](/img/blog/graph-preview/react.png)


在本示例中，我们仅为 Agent 绑定了一个天气查询服务，接收到用户的天气查询服务后，流程会在 AgentNode 和 ToolNode 之间循环执行，直到完成用户指令。示例中判断指令完成的条件（即 ReAct 结束条件）也很简单，模型 AssistantMessage 无 tool_call 指令则结束（采用默认行为）。



核心代码展示：

```java
ReactAgent reactAgent = ReactAgent.builder()
    .name("React Agent Demo")
    .prompt("请完成接下来用户输入给你的任务。")
    .chatClient(chatClient)
    .resolver(resolver)
    .maxIterations(10)
    .build();

reactAgent.invoke(Map.of("messages", new UserMessage(query)))
```



可下载本示例源码并运行，打开浏览器访问如下示例链接，查看运行效果：

+ [http://localhost:18080/react/chat?query=分别帮我查询杭州、上海和南京的天气](http://localhost:18080/react/chat?query=分别帮我查询杭州、上海和南京的天气)

## 示例三：基于 Supervisor 多智能体的 OpenManus 实现
Spring AI Alibaba 曾发布了业界首个 OpenManus 的 Java 版本实现方案，原版本实现源码与博客解读文章请参见：

1. 博客解读 [https://java2ai.com/blog/spring-ai-alibaba-openmanus](https://java2ai.com/blog/spring-ai-alibaba-openmanus)
2. 示例源码：[https://github.com/alibaba/spring-ai-alibaba](https://github.com/alibaba/spring-ai-alibaba)/community/openmanus



<font style="color:rgb(53, 56, 65);">原 OpenManus 的实现并没有使用 Spring AI Alibaba Graph，因此我们花费了大量时间在编写流程控制逻辑。在之前版本的 OpenManus 实现解读中，我们总结了以下相关实现问题：</font>

+ <font style="color:rgb(53, 56, 65);">仓库中 80% 代码都在解决流程编排问题，入串联 manus agent 子流程、做消息记忆、转发工具调用、全局状态修改等，这部分工作可以交给高度抽象的 agent 框架实现，以简化开发复杂度。</font>
+ <font style="color:rgb(53, 56, 65);">工具的覆盖度与执行效果一般，如浏览器使用、脚本执行工具等。</font>
+ <font style="color:rgb(53, 56, 65);">规划及工作流程中无法人为介入进行 review、动态修改、回退等动作。</font>
+ <font style="color:rgb(53, 56, 65);">当前 OpenManus 实现的效果调试相对比较困难。</font>



如今在 Spring AI Alibaba Graph 的帮助下，我们这次可以低于之前 80% 的代码量实现同样的 OpenManus 系统，以下是 OpenManus 实现的架构图：

![Spring AI Alibaba Graph OpenManus](/img/blog/graph-preview/openmanus.png)

在 OpenManus 示例中，我们实现了一个 multi-agent 系统。其中，有三个核心 agent 互相协作完成用户任务：

1. Planning Agent，负责任务规划，任务规划的输出是包含多个步骤描述的详细规划。
2. Supervisor Agent（Controller Agent），负责监督 Executor Agent 完成 Planning Agent 规划的任务，按顺序将每一步子任务转发给 Executor Agent。
3. Executor Agent，负责执行每一步子任务，该 Agent 包含多个可用工具如 Browser_use、FileSaver、PythonExecutor 等。

请注意，上图中的 Planning Agent、Executor Agent 都分别是一个嵌套的 ReAct Agent 实现，这两个 Agent 可以理解为是一个嵌套的 Graph，并且与父 Graph 具有隔离的上下文状态。他们展开后的架构如下：

![Spring AI Alibaba Graph React](/img/blog/graph-preview/react.png)

可下载本示例源码并运行，打开浏览器访问如下示例链接，查看运行效果：

+ [http://localhost:18080/manus/chat?query=帮我查询阿里巴巴近一周的股票信息](http://localhost:18080/manus/chat?query=帮我查询阿里巴巴近一周的股票信息)

## 运行示例
首先，运行以下命令下载源码：

```shell
git clone https://github.com/alibaba/spring-ai-alibaba.git
cd spring-ai-alibaba-graph/spring-ai-alibaba-graph-example
```

为了访问大模型，需要导出模型 API-KEY 到环境变量：

```shell
export AI_DASHSCOPE_API_KEY=xxx
```

然后，就可以在 IDE 中直接运行 `GraphApplication` 类启动示例应用。

或者，您可以运行以下 maven 命令启用示例应用（注意，由于 Spring AI Alibaba Graph 尚未正式发布，需要在根目录先 install 源码）：

```shell
mvn clean install
cd spring-ai-alibaba-graph/spring-ai-alibaba-graph-example
mvn spring-boot:run
```

## 设计理念与未来规划
当前版本的 Spring AI Alibaba Graph 在设计理念上大幅参考了 Langgraph，包括全局 State 管理、Graph 定义等，基本上可以说是 Langgraph 的 Java 实现版本，特别感谢 Langchain 社区开源贡献的 Langgraph 智能体框架，让我们可以站在巨人肩膀上继续前进。Agent 技术一直在快速发展之中，我们将在此基础上探索更多前沿 Agent 实现方案。


Spring AI Alibaba Graph 尚未正式发布，欢迎开发者通过 Github 源码体验并参与贡献：[https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph](https://github.com/alibaba/spring-ai-alibaba/tree/main/spring-ai-alibaba-graph)

