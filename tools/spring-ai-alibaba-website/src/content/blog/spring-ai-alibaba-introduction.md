---
title: 阿里云开源 Spring AI Alibaba，加码 Java AI 生态建设
keywords: [Spring AI, LLM, 大模型, 通义千问, Langchain, RAG]
description: 阿里云开源 Spring AI Alibaba，加码 Java AI 生态建设
author: 刘军
date: "2024-12-09"
category: article
---

<font style="color:rgb(53, 56, 65);">本文作者：刘军，Spring AI Alibaba 发起人，Apache Member。</font>

<font style="color:rgb(53, 56, 65);"></font>

<font style="color:rgb(53, 56, 65);">编者按：6年前，2018年10月，阿里巴巴开源 Spring Cloud Alibaba，旨在帮助 Java 开发者通过 Spring Cloud 编程模型轻松开发微服务应用。6年后，大模型和 AI 正在深刻改变我们工作和生活的方方面面，不再是移动屏幕端，而是整个物理世界。恰逢其时，阿里云开源 Spring AI Alibaba，旨在帮助 Java 开发者快速构建 AI 应用，共同构建物理新世界，欢迎您加入社区，一起参与这件激动人心的事情。</font>

<font style="color:rgb(53, 56, 65);"></font>

<font style="color:rgb(53, 56, 65);">近期，阿里云重磅发布了首款面向 Java 开发者的开源 AI 应用开发框架：Spring AI Alibaba（项目 Github 仓库地址 alibaba/spring-ai-alibaba），Spring AI Alibaba 项目基于 Spring AI 构建，是阿里云通义系列模型及服务在 Java AI 应用开发领域的最佳实践，提供高层次的 AI API 抽象与云原生基础设施集成方案，帮助开发者快速构建 AI 应用。本文将详细介绍 Spring AI Alibaba 的核心特性，并通过「智能机票助手」的示例直观的展示 Spring AI Alibaba 开发 AI 应用的便利性。示例源代码已上传至 GitHub 仓库和官网(</font>[https://sca.aliyun.com/ai/](https://sca.aliyun.com/ai/)<font style="color:rgb(53, 56, 65);">)。</font>

<font style="color:rgb(53, 56, 65);"></font>

## 项目简介
![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-1.png)

Spring AI Alibaba 项目的产生背景是生成式 AI 与大模型在过去一年的快速发展，大家应该有直观的感受，周边所有人都在聊模型服务，但是训练大模型本身是少部分企业和算法工程师的职责，我们作为使用方、开发者，更关注的应该是如何为我们的应用接入生成式AI能力。

对应用来说，最直观的 AI 模型接入方式就是使用 Open API，包括阿里云通义系列模型、OpenAI 等都有提供 Open API 访问方式，这种方式最灵活、但可想而知对于开发者成本会非常高，我们要理解API规范，还要学习与AI模型交互的各种模式。如果我们是使用 Spring 开发的 AI 应用，那么我们可以使用 RestTemplate这样的工具，它可以减少我们调用 API 的成本，但对于一些通用的 AI 应用开发范式，RestTemplate 并不能给我们带来什么帮助。因此，对于Java开发者来说，我们需要一款AI应用开发框架来简化AI应用开发。



在这样的背景下，Spring 官方开源了 Spring AI 框架，用来简化 Spring 开发者开发智能体应用的过程。随后阿里巴巴开源了 Spring AI Alibaba，它基于 Spring AI，同时与阿里云百炼大模型服务、通义系列大模型做了深度集成与最佳实践。基于 Spring AI Alibaba，Java 开发者可以非常方便的开发 AI 智能体应用。

阿里巴巴和 Spring 官方一直保持着非常成功的合作，在微服务时代共同合作打造了 SpringCloud Alibaba 微服务框架与整体解决方案，该框架已经是国内使用最广泛的开源微服务框架之一，整体生态 star 数超过 10w。

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-2.png)


关于 Spring AI Alibaba，我们期望保持同样的深度合作模式，其中 Spring 社区主要负责绿色部分，即智能体应用开发原子能力与API的抽象，Spring AI Alibaba 社区负责与阿里云通义模型、云原生基础设施的深度集成，同时包括与智能体业务落地更密切的一些核心能力如流程编排、开发工具集、应用评测、可观测、配置管理、流量管控等的抽象和实现。背靠 Spring 与阿里巴巴两大开源社区支撑，相信 Spring AI Alibaba 项目将会长期保持一个持续且健康的发展态势。

## 核心特性
以下是 Spring AI Alibaba 框架的核心特性，可以帮我们加速和简化 Java 智能体应用的开发。

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-3.png)


第一点，Spring AI Alibaba是专门为Spring和Java开发者设计的智能体开发框架，使用它开发应用就如同开发一个普通的Spring Boot应用，理解成本非常低。

第二点，框架对AI智能体应用的通用开发范式做了很好的抽象，从原子能力层次如对话模型接入、提示词模板到函数调用，再到高层次抽象如智能体编排、对话记忆等。

第三点，框架默认与通义系列模型做了深度适配，除此之外，还提供了应用从部署到运维的最佳实践，包括网关、配置管理、部署、可观测等。



接下来，我们一起看一下 Spring AI Alibaba 框架中的一些具体概念与 API 定义

### 聊天模型（Chat Model）
首先，是 SpringAI 对模型基本交互的抽象与适配，其实智能体应用开发本质就是一个与大模型服务不停交互的过程，应用为模型提供语义化的输入，模型推理后反馈给我们输出。

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-4.png)


与模型交互的输入输出可以是文本，比如最早期的 ChatGPT，现在我们不止有文本型模型、还有支持图像、视频、语音等的模型，因此输入输出对应的可能是各种类型的。包括一些模型开始支持多模态输入，也就是图形、文本的混合输入。

Spring AI 对于这种类似聊天式的大模型交互模式提供了完整的抽象，包括文本、图像、语音等，对于应用开发者来说，可以直接调用 Spring AI 提供的 API，以方法入参做模型输入，返回值作模型输出，同时还支持同步、异步、流式等通信模式。如果开发者要切换底层的模型服务，也是完全透明无感的。

### 提示词（Prompt）
Prompt 提示词是与模型交互的一种输入数据组织方式，本质上是一种复合结构的输入，在 prompt 我们是可以包含多组不同角色（System、User、Aissistant等）的信息。如何管理好 Prompt 是简化 AI 应用开发的关键环节。

Spring AI 提供了 Prompt Template 提示词模板管理抽象，开发者可以预先定义好模板，并在运行时替换模板中的关键词。

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-5.png)


SpringAI 还支持从资源文件中直接加载提示词模板：

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-6.png)


### 格式化输出（Structured Output）
大模型返回的数据通常是非格式化的，而应用上下游需要传递格式化的、确定的数据结构。因此 SpringAI 提供了结构化输出转换的能力，它可以自动化的帮我们在 Prompt 中加入数据格式信息，辅助模型理解我们要求的结果数据格式，同时在拿到模型数据后完成到 JavaBean 的转换。

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-7.png)


以上图为例，Spring AI 框架帮我们简化了整个格式化输出转换的过程，包括格式信息输入与结果转换。

### 函数调用（Function Calling）
函数调用是AI应用与模型交互中一个非常典型的范式，它可以辅助模型更好的回答用户问题。我们在给模型输入的过程中，附带上可用的函数列表（包含函数名、函数描述等），模型在收到问题和函数列表后，根据对问题的推理在必要的时候发起对函数的调用。

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-8.png)


SpringAI 帮我们规范了函数定义、注册等过程，并在发起模型请求之前自动将函数注入到 Prompt 中，而当模型决策在合适的时候去调用某个函数时，Spring AI 完成函数调用动作，最终将函数执行结果与原始问题再一并发送给模型，模型根据新的输入决策下一步动作。这其中涉及与大模型的多次交互过程，一次函数调用就是一次完成的交互过程。

这里是函数调用过程中具体的模型交互过程分析。

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-9.png)


以上是没有函数计算时应用和模型的交互过程，应用给模型的输入是求一个数的平方根，模型根据自己的理解算出了一个结果，但并不准确。

为了解决这个问题，我们定义了一个求平方根的函数，并通过 Spring AI 提供的注解将这个函数注册为一个可与模型交互的特殊函数，如下图代码段所示：


![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-10.png)


下图是有了函数调用之后应用与模型的交互，可以看到为了得到最终答案，应用总共有两轮与模型的交互过程。第一次是发起求平方根提问，与之前的不同是请求 prompt 中携带了 “tools” 信息（包含我们定义的函数），此时模型返回了 “ToolExecutionRequest” 的特殊结果，要求 Spring AI 通过调用函数来辅助完成求平方根的动作；随后是第二轮模型交互，Spring AI 把原始问题和函数执行结果发送给模型，模型最终生成回答。


![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-11.png)


### 检索增强（RAG）
RAG 是另外一个常用的智能体应用开发范式，它本质上和函数调用类似，也是应用程序辅助模型推理与回答问题的一种方式，只不过在交互流程上和函数调用略有区别。

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-12.png)


如上图所示，总体上 RAG 是分为离线和运行时两部分。离线部分是将一些领域特有数据进行向量化的过程，将向量化的数据存入向量数据库。图中后半部分体现的运行时流程，Spring AI 框架在组装 prompt 时，会额外检索向量数据库，最终生成一个比用户原始问题具有更多辅助上下文的 prompt，然后将这个具备上下文的 prompt 给到模型，模型根据用户问题、上下文以及自己的推理生成响应。

Spring AI 提供了从离线数据加载、分析到向量化存储的抽象，也提供了运行时检索、prompt 增强的抽象。

## 示例实践
### Hello World 示例
> 可参考 [官网文档快速开始](/docs/dev/get-started/) 详细了解如何使用 Spring AI Alibaba 快速开发生成式 AI 应用。
>

使用 Spring AI Alibaba 开发应用与使用普通 Spring Boot 没有什么区别，只需要增加 `spring-ai-alibaba-starter` 依赖，将 `ChatClient` Bean 注入就可以实现与模型聊天了。

1. 添加依赖

```xml
<dependency>
    <groupId>com.alibaba.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.0.0-M3.2</version>
</dependency>

```



> 注意：由于 spring-ai 相关依赖包还没有发布到中央仓库，如出现 spring-ai-core 等相关依赖解析问题，请在您项目的 pom.xml 依赖中加入如下仓库配置。
>
> <repositories>
>
>     <repository>
>
>     	<id>spring-milestones</id>
>
>     	<name>Spring Milestones</name>
>
>     	<url>[https://repo.spring.io/milestone</url>](https://repo.spring.io/milestone</url>)
>
>     	<snapshots>
>
>     		<enabled>false</enabled>
>
>     	</snapshots>
>
>     </repository>
>
> </repositories>
>


2. 配置 `application.yaml` 指定 API-KEY（可通过访问阿里云百炼模型服务平台获取，有免费额度可用）

```xml
spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
```



3. 注入智能体代理 `ChatClient`

```java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(String input) {
        return this.chatClient.prompt()
                .user(input)
                .call()
                .content();
    }
}
```

### 智能机票助手
接下来，我们通过一个更贴近实际使用场景的示例，来展示 Spring AIAlibaba 在构建智能体应用方面的强大能力。

示例目标是使用 Spring AI Alibaba 框架开发一个智能机票助手，它可以帮助消费者完成<font style="color:#5e5e5e;">机票预定、问题解答、机票改签、取消等动作，具体要求为：</font>

+ <font style="color:#5e5e5e;">基于 AI 大模型</font>与用户对话，理解用户自然语言表达的需求
+ <font style="color:#5e5e5e;">支持多轮连续对话，能在上下文中理解用户意图</font>
+ <font style="color:#5e5e5e;">理解机票操作相关的术语与规范并严格遵守，如航空法规、退改签规则等</font>
+ 在必要时可<font style="color:#5e5e5e;">调用工具辅助完成任务</font>

#### 完整架构图
<font style="color:#5e5e5e;">基于这样一个</font>智能机票助手目标，我们绘制了一个如下图所示的架构图：

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-13.png)


#### **<font style="color:rgb(0, 0, 0);">使用 ChatClient 完成编码</font>**
<font style="color:rgb(0, 0, 0);">Spring AI Alibaba 不止提供了以上原子能力抽象，还提供了高阶 “智能体” API 抽象 `ChatClient`，让我们可以非常方便的使用流式 Fluent API 把多个组件组装起来，成为一个智能体 Agent。</font>

<font style="color:rgb(0, 0, 0);">对于智能机票助手与 AI 模型交互的所有能力（Prompt、RAG、Chat Memory、Function Calling 等），我们可以直接使用</font><font style="color:rgb(0, 0, 0);background-color:#f7f7f7;">ChatClient</font><font style="color:rgb(0, 0, 0);"> 进行声明，最终实例化一个可以智能体代理对象。示例代码如下：</font>

```java
this.chatClient = modelBuilder
				.defaultSystem("""
						您是“Funnair”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
					   您正在通过在线聊天系统与客户互动。
					   在提供有关预订或取消预订的信息之前，您必须始终
					   从用户处获取以下信息：预订号、客户姓名。
					   在询问用户之前，请检查消息历史记录以获取此信息。
					   在更改预订之前，您必须确保条款允许这样做。
					   如果更改需要收费，您必须在继续之前征得用户同意。
					   使用提供的功能获取预订详细信息、更改预订和取消预订。
					   如果需要，可以调用相应函数调用完成辅助动作。
					   请讲中文。
					   今天的日期是 {current_date}.
					""")
				.defaultAdvisors(
						new PromptChatMemoryAdvisor(chatMemory), // Chat Memory
						new VectorStoreChatMemoryAdvisor(vectorStore)),
						new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()), // RAG
						new LoggingAdvisor())
				.defaultFunctions("getBookingDetails", "changeBooking", "cancelBooking") // FUNCTION CALLING

				.build();
```

<font style="color:#5e5e5e;">这样，</font>`<font style="color:#5e5e5e;">ChatClient</font>`<font style="color:#5e5e5e;">就为我们屏蔽了所有与大模型交互的细节，只需要把 </font>`<font style="color:#5e5e5e;">ChatClient</font>`<font style="color:#5e5e5e;">注入常规的 Spring Bean 就可以为我们的机票应用加入智能化能力了。</font>

最终，我们开发的示例运行效果如下所示：



![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-14.png)

<font style="color:#5e5e5e;"></font>

> 本示例项目的源码请参见文章最后 Github 仓库地址。
>

## 项目规划
Spring AI Alibaba 的目标是提供 AI 开源框架以及与阿里巴巴整体开源生态的深度适配，以帮助 Java 开发者快速构建 AI Native 应用架构。

+ Prompt Template 管理
+ 事件驱动的 AI 应用程序
+ 更多 Vector Database 支持
+ 函数计算等部署模式
+ 可观测性建设
+ AI代理节点开发能力，如绿网、限流、多模型切换等
+ 开发者工具集

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-15.png)


## 联系社区
+ <font style="color:rgba(0, 0, 0, 0.65);">项目官网：https://sca.aliyun.com/ai</font>
+ <font style="color:rgba(0, 0, 0, 0.65);">Github源码与示例：https://github.com/alibaba/spring-ai-alibaba</font>
+ 钉钉群：请通过群号 `64485010179` 搜索入群
+ 微信群：请扫描下图加入

![spring-ai-alibaba-introduction](/img/blog/intro/spring-ai-alibaba-introduction-16.png)


