# Spring AI Alibaba

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![CI Status](https://github.com/alibaba/spring-ai-alibaba/workflows/%F0%9F%9B%A0%EF%B8%8F%20Build%20and%20Test/badge.svg)](https://github.com/alibaba/spring-ai-alibaba/actions?query=workflow%3A%22%F0%9F%9B%A0%EF%B8%8F+Build+and+Test%22)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/alibaba/spring-ai-alibaba)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.alibaba.cloud.ai/spring-ai-alibaba/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.alibaba.cloud.ai/spring-ai-alibaba)
<img alt="gitleaks badge" src="https://img.shields.io/badge/protected%20by-gitleaks-blue">

[Spring AI Alibaba](https://java2ai.com) 是一款以 Spring AI 为基础，深度集成百炼平台，支持 ChatBot、工作流、多智能体应用开发模式的 AI 框架。

[English](./README.md)

## 核心特性

<p align="center">
    <img src="./docs/imgs/spring-ai-alibaba-architecture.png" alt="architecture" style="max-width: 740px; height: 508px" /> 
</p>


Spring AI Alibaba 提供以下核心能力，帮助开发者快速构建自己的 Agent、Workflow 或 Multi-agent 应用：

1. **Graph 多智能体框架。** 基于 Spring AI Alibaba Graph 开发者可快速构建工作流、多智能体应用，无需关心流程编排、上下文记忆管理等底层实现。支持 Dify DSL 自动生成 Graph 代码，支持 Graph 可视化调试。
2. **通过 AI 生态集成，解决企业智能体落地过程中关心的痛点问题。** Spring AI Alibaba 支持与百炼平台深度集成，提供模型接入、RAG知识库解决方案；支持 ARMS、Langfuse 等 AI 可观测产品无缝接入；支持企业级的 MCP 集成，包括 Nacos MCP Registry 分布式注册与发现、自动 Router 路由等。
3. **探索具备自主规划能力的通用智能体产品与平台。** 社区发布了基于 Spring AI Alibaba 框架实现的 JManus 智能体，除了对标 Manus 等通用智能体的产品能力外。社区在积极探索自主规划在智能体开发方向的应用，为开发者提供从低代码、高代码到零代码构建智能体的更灵活选择，加速智能体在企业垂直业务方向的快速落地。

## 快速开始

在项目中加入 `spring-ai-alibaba-starter-dashscope` 依赖，快速开始智能体应用开发

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.alibaba.cloud.ai</groupId>
      <artifactId>spring-ai-alibaba-bom</artifactId>
      <version>1.0.0.2</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
  </dependency>
</dependencies>
```

请查看官网 [快速开始](https://java2ai.com/docs/1.0.0/get-started/) 了解详细示例讲解。更多 starter 包括 spring-ai-alibaba-graph-core、spring-ai-alibaba-starter-nl2sql、spring-ai-alibaba-starter-nacos-mcp-client 等用法，请参考官方网文档资料。

> 注意：
> 1. 运行项目需要 JDK 17 及以上版本。
> 2. 如果出现 spring-ai 相关依赖下载问题，请参考官网文档配置 spring-milestones Maven 仓库。

### 体验官方 Playground 示例

Spring AI Alibaba 官方社区开发了一个**包含完整 `前端UI+后端实现` 的智能体 Playground 示例**，示例使用 Spring AI Alibaba 开发，可以体验聊天机器人、多轮对话、图片生成、多模态、工具调用、MCP集成、RAG知识库等所有框架核心能力。

整体运行后的界面效果如下所示：

<p align="center">
    <img src="./docs/imgs/playground.png" alt="PlayGround" style="max-width: 949px; height: 537px; border-radius: 15px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);" /> 
</p>



您可以在[本地部署 Playground 示例](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-playground)并通过浏览器访问体验，或者拷贝源码并按照自己的业务需求调整，以便能够快速基于 Spring AI Alibaba 搭建一套自己的 AI 应用。


学习更多 Spring AI Alibaba 框架用法，请参考 Spring AI Alibaba 社区的官方示例源码仓库：

[https://github.com/springaialibaba/spring-ai-alibaba-examples](https://github.com/springaialibaba/spring-ai-alibaba-examples)

### 体验官方Workflow示例

要快速体验 Spring AI Alibaba Graph，可以基于官方提供的 WorkflowAutoconfiguration 示例搭建一个完整的工作流应用。下面将以“客户评价处理系统”为例，介绍从项目依赖到运行测试的主要步骤：

1. **添加依赖与配置模型**：在 Spring Boot 项目的 Maven `pom.xml` 中引入 Spring AI Alibaba 的 BOM 以及所需的 Starter 依赖。如引入阿里百炼大模型 DashScope 的 Starter（或选择 OpenAI Starter，具体取决于所用模型平台）。例如：

   ```xml
   <dependencyManagement>
       <dependencies>
           <dependency>
               <groupId>com.alibaba.cloud.ai</groupId>
               <artifactId>spring-ai-alibaba-bom</artifactId>
               <version>1.0.0.2</version>
               <type>pom</type>
               <scope>import</scope>
           </dependency>
       </dependencies>
   </dependencyManagement>
   <dependencies>
       <!-- 引入 DashScope 模型适配的 Starter -->
       <dependency>
           <groupId>com.alibaba.cloud.ai</groupId>
           <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
       </dependency>
     	<!-- 引入 Graph 核心依赖 -->
     	<dependency>
           <groupId>com.alibaba.cloud.ai</groupId>
           <artifactId>spring-ai-alibaba-graph-core</artifactId>
           <version>1.0.0.2</version>
    		</dependency>
   </dependencies>
   
   ```

   添加依赖后，在项目的 `application.properties` 中配置模型 API 密钥。例如使用 OpenAI 模型时设置 `spring.ai.openai.api-key=<您的API密钥>`，或使用阿里百炼模型时配置 DashScope 服务的访问密钥（如 `spring.ai.dashscope.api-key=<您的API密钥>` 。这些配置确保 Spring AI 能自动创建所需的 **ChatModel** Bean，用于与对应的模型服务通信。

2. **定义工作流 StateGraph**：创建一个 Spring Boot 配置类（例如 `WorkflowAutoconfiguration`），在其中定义一个 **StateGraph** Bean 来描述工作流逻辑。在该配置中，需要完成以下几个步骤：

   - **初始化 ChatClient**：从容器中获取注入的 ChatModel（由上一步配置产生），构建一个 ChatClient 实例并附加必要的 Advisor（如日志记录器），用于后续 LLM 调用。例如：

     ```java
     ChatClient chatClient = ChatClient.builder(chatModel)
                                       .defaultAdvisors(new SimpleLoggerAdvisor())
                                       .build();
     ```

     这里 `ChatClient` 是 Spring AI Alibaba 提供的与大模型对话的客户端，可看作对底层 API 的封装。

   - **设置全局状态 OverAllState**：定义一个 OverAllStateFactory，用于在每次执行工作流时创建初始的全局状态对象。通过注册若干 **Key** 及其更新策略来管理上下文数据：

     ```java
     OverAllStateFactory stateFactory = () -> {
         OverAllState state = new OverAllState();
         state.registerKeyAndStrategy("input", new ReplaceStrategy());
         state.registerKeyAndStrategy("classifier_output", new ReplaceStrategy());
         state.registerKeyAndStrategy("solution", new ReplaceStrategy());
         return state;
     };
     ```

     上述代码注册了三个状态键：`input`（输入文本）、`classifier_output`（分类结果）和 `solution`（最终处理结论），均采用 **ReplaceStrategy**（每次写入替换旧值）。这些键将贯穿整个工作流，用于在节点之间传递数据。

   - **定义节点 (Node)**：创建工作流中的核心节点，包括两个文本分类节点和一个记录节点。在本示例中，框架提供了预定义的 **QuestionClassifierNode** 类用于文本分类任务。我们利用其构建器指定分类的细分类别及提示语，引入 ChatClient 来调用大模型服务，实现智能分类：

     ```java
     // 评价正负分类节点
     QuestionClassifierNode feedbackClassifier = QuestionClassifierNode.builder()
             .chatClient(chatClient)
             .inputTextKey("input")
             .categories(List.of("positive feedback", "negative feedback"))
             .classificationInstructions(
                     List.of("Try to understand the user's feeling when he/she is giving the feedback."))
             .build();
     // 负面评价具体问题分类节点
     QuestionClassifierNode specificQuestionClassifier = QuestionClassifierNode.builder()
             .chatClient(chatClient)
             .inputTextKey("input")
             .categories(List.of("after-sale service", "transportation", "product quality", "others"))
             .classificationInstructions(List.of(
                     "What kind of service or help the customer is trying to get from us? " + 
                     "Classify the question based on your understanding."))
             .build();
     ```

     上面定义了两个节点：`feedbackClassifier` 将判断反馈是**正面**还是**负面**；`specificQuestionClassifier` 则对负面反馈进一步归类（如售后服务、运输、产品质量或其他）。两者都使用 ChatClient 连调用大模型完成分类，并会把结果写入全局状态的 `"classifier_output"` 键中（框架内部约定）。此外，也可以按需定义自定义节点。

     例如自定义的 `RecordingNode` 节点用于记录和处理最终结果：

     ```java
     // 记录结果的节点
     RecordingNode recorderNode = new RecordingNode();
     ```

     `RecordingNode` 实现了 NodeAction 接口，会在流程末尾根据分类结果生成相应的解决方案，并将结果写回OverAllState。

   - **添加节点到 StateGraph**：使用 **StateGraph** 的 API，将上述节点加入图中，并设置节点间的跳转关系：

     ```java
     StateGraph graph = new StateGraph("Consumer Service Workflow Demo", stateFactory)
             .addNode("feedback_classifier", node_async(feedbackClassifier))
             .addNode("specific_question_classifier", node_async(specificQuestionClassifier))
             .addNode("recorder", node_async(recorderNode))
             // 定义边（流程顺序）
             .addEdge(START, "feedback_classifier")  // 起始节点
             .addConditionalEdges("feedback_classifier",
                     edge_async(new CustomerServiceController.FeedbackQuestionDispatcher()),
                     Map.of("positive", "recorder", "negative", "specific_question_classifier"))
             .addConditionalEdges("specific_question_classifier",
                     edge_async(new CustomerServiceController.SpecificQuestionDispatcher()),
                     Map.of("after-sale", "recorder", "transportation", "recorder", 
                            "quality", "recorder", "others", "recorder"))
             .addEdge("recorder", END);  // 结束节点
     ```

     上述配置完成了工作流图的搭建：首先将节点注册到图，并使用 `node_async(...)` 将每个 NodeAction 包装为异步节点执行（提高吞吐或防止阻塞，具体实现框架已封装）。然后定义了节点间的边（Edges）和条件跳转逻辑：

     - `START -> feedback_classifier`：特殊的 START 状态直接进入初始 **反馈分类** 节点；
     - `feedback_classifier -> recorder` 或 `-> specific_question_classifier`：通过 **条件边**根据分类结果选择下一步。这里使用 `FeedbackQuestionDispatcher` 实现 **EdgeAction** 来读取分类输出并返回 `"positive"` 或 `"negative"` 字符串，分别映射到后续节点；
     - `specific_question_classifier -> recorder`：同样通过条件边，无论负面反馈被细分为何种类别（售后、运输、质量或其它），都汇流到 **记录** 节点进行统一处理；
     - `recorder -> END`：最后记录节点执行完毕，进入终止状态 END，结束整个流程。

   完成上述定义后，将配置类中构建的 `StateGraph` Bean 注入 Spring 容器即可。框架会在运行时根据此定义自动编译图并等待被调用执行。

3. **运行应用**：在配置好依赖和工作流后，启动 Spring Boot 应用（例如使用 `mvn spring-boot:run` 或在 IDE 中运行主应用类）。应用启动时会读取配置的 API 密钥，初始化 ChatModel/ChatClient，并注册定义好的 StateGraph。Spring AI Alibaba Graph 提供了简单的调用接口来触发工作流执行，例如可以通过 REST 控制器映射 HTTP 请求到图的执行。示例应用就包含了一个控制器 `CustomerServiceController` 将工作流暴露为 HTTP 接口，下一节将详细说明。



上述工作流应用的业务场景是对用户产品评价进行分类和处理。各个组件的协作如下：

- **评价分类节点（feedback_classifier）**：这是一个 `QuestionClassifierNode`，用于判断用户反馈是正面还是负面。它利用 LLM 对输入文本（存储在 `"input"` 键）进行语义理解，并输出类别结果（如 *positive feedback* 或 *negative feedback*）。分类结果会写入全局状态的 `"classifier_output"` 键，供后续边的判断逻辑使用。

- **负面评价细分节点（specific_question_classifier）**：同样是 `QuestionClassifierNode`，在检测到反馈为负面时被执行。它会根据负面反馈的内容，将问题归类为 *售后服务*、*运输物流*、*产品质量* 或 *其他* 四种类型之一。这个节点复用了输入文本 `"input"`，并将更具体的分类结果写入 `"classifier_output"`（会覆盖之前的值，因为该键设置了 ReplaceStrategy 策略）。

- **边的调度逻辑（EdgeAction）**：两个分类节点之间的转接逻辑由 `FeedbackQuestionDispatcher` 和 `SpecificQuestionDispatcher` 来完成。它们实现了 **EdgeAction** 接口，作用是在节点执行完后读取全局状态，决定下一步该走哪条边：

  - `FeedbackQuestionDispatcher`（用于 feedback_classifier 节点之后）会检查 `classifier_output` 字符串，包含“positive”则返回 `"positive"`，否则一律返回 `"negative"。因此，StateGraph 将 `"positive"` 映射到 `recorder` 节点，`"negative"` 映射到 `specific_question_classifier` 节点。
  - `SpecificQuestionDispatcher`（用于 specific_question_classifier 节点之后）则解析更细的类别结果。它预先定义了若干关键词映射（如包含“after-sale”则返回 `"after-sale"` 等）。遍历发现分类结果字符串中含有某个关键词就返回对应值，否则返回 `"others"。StateGraph 据此将所有可能值（after-sale、transportation、quality、others）都指向同一个后续节点 `recorder`。

  通过以上 EdgeAction，工作流实现了**动态路径选择**：正面反馈走简化路径，负面反馈则进入细分流程，充分体现了 Spring AI Alibaba Graph 在**路由分支**场景下的优势。

- **记录节点（recorder）**：`RecordingNode` 是按需自定义的 NodeAction，实现对最终结果的记录和决策。它的 `apply` 方法读取全局状态中的 `"classifier_output"` 字段值，判断其中是否包含“positive”。如果是正面反馈，则仅记录日志无需进一步动作（在示例中将 `"solution"` 字段设为固定文本“Praise, no action taken.”表示无需处理，真实业务场景中可扩展逻辑，例如通过HttpNode将结果发送到品牌宣传部门)；否则将负面反馈的细分类结果作为解决方案（即把 `"classifier_output"` 的内容原样填入 `"solution"`）。同时，RecordingNode 也通过日志打印了收到的反馈类型，方便在控制台查看分类结果。这一节点相当于整个工作流的收尾，决定了对于不同类型的用户评价给出怎样的处理结论。

综上，各组件协同完成了一个两级分类流程：**首先判断评价正负，其次细分负面问题，最后输出处理方案**。这种解耦的设计使开发者可以轻松地调整每个环节，例如替换分类模型、更改分类粒度，或在负面反馈流程中增加其他处理步骤（发送告警、存储数据库等），而无需影响整体架构。

完整的PlantUML 工作流图如下：

```
powered by spring-ai-alibaba
end footer
circle start<<input>> as __START__
circle stop as __END__
usecase "feedback_classifier"<<Node>>
usecase "specific_question_classifier"<<Node>>
usecase "recorder"<<Node>>
hexagon "check state" as condition1<<Condition>>
hexagon "check state" as condition2<<Condition>>
"__START__" -down-> "feedback_classifier"
"feedback_classifier" .down.> "condition1"
"condition1" .down.> "specific_question_classifier": "negative"
'"feedback_classifier" .down.> "specific_question_classifier": "negative"
"condition1" .down.> "recorder": "positive"
'"feedback_classifier" .down.> "recorder": "positive"
"specific_question_classifier" .down.> "condition2"
"condition2" .down.> "recorder": "others"
'"specific_question_classifier" .down.> "recorder": "others"
"condition2" .down.> "recorder": "transportation"
'"specific_question_classifier" .down.> "recorder": "transportation"
"condition2" .down.> "recorder": "quality"
'"specific_question_classifier" .down.> "recorder": "quality"
"condition2" .down.> "recorder": "after-sale"
'"specific_question_classifier" .down.> "recorder": "after-sale"
"recorder" -down-> "__END__"
@enduml
```



```Mermaid
flowchart TD
    START((Start))
    feedback_classifier["反馈分类<br/>(feedback_classifier)"]
    specific_question_classifier["负面细分分类<br/>(specific_question_classifier)"]
    recorder["记录处理结果<br/>(recorder)"]
    END((End))

    START --> feedback_classifier

    feedback_classifier -->|positive| recorder
    feedback_classifier -->|negative| specific_question_classifier

    specific_question_classifier -->|after-sale service| recorder
    specific_question_classifier -->|transportation| recorder
    specific_question_classifier -->|product quality| recorder
    specific_question_classifier -->|others| recorder

    recorder --> END

```

完成上述配置后，就可以在本地运行这个工作流应用，并通过 HTTP 接口进行测试：

- **启动应用**：确保已在配置文件中设置模型所需的密钥，然后启动 Spring Boot 应用。应用启动日志中应能看到 ChatClient 初始化和 StateGraph 编译成功的信息。如果使用的是 OpenAI 模型，在首次调用时可能下载模型的 API 描述；使用阿里云模型则需要确保网络能访问 DashScope 服务。

- **调用工作流接口**：示例应用通过 `CustomerServiceController` 将工作流暴露为 REST 接口。在浏览器或命令行中调用以下 GET 请求即可触发流程：

  ```java
  # 调用正面评价案例
  curl "http://localhost:8080/customer/chat?query=This product is excellent, I love it!"
  ```

学习更多 Spring AI Alibaba 框架用法，请参考 Spring AI Alibaba 社区的官方示例源码仓库：

[https://github.com/springaialibaba/spring-ai-alibaba-examples](https://github.com/springaialibaba/spring-ai-alibaba-examples)

## Spring AI Alibaba Graph 多智能体框架
Spring AI Alibaba Graph 使开发者能够实现工作流和多智能体应用编排。其核心设计理念参考自 Langgraph，Spring AI Alibaba 社区在此基础上增加了大量预置 Node、简化了 State 定义过程，让开发者能够更好的与低代码平台集成、编写主流多智能体应用。

Spring AI Alibaba Graph 核心能力：

+ 支持工作流，内置工作流节点，与主流低代码平台对齐；
+ 支持 Multi-agent，内置 ReAct Agent、Supervisor 等模式；
+ 支持 Streaming；
+ Human-in-the-loop，通过人类确认节点，支持修改状态、恢复执行；
+ 支持记忆与持久存储；
+ 支持流程快照；
+ 支持嵌套分支、并行分支；
+ PlantUML、Mermaid 可视化导出。

## 企业级 AI 生态集成
在 Agent 生产落地过程中，用户需要解决智能体效果评估、MCP 工具集成、Prompt 管理、Token 上下文、可视化 Tracing 等各种问题。Spring AI Alibaba 通过与 Nacos3、Higress AI 网关、阿里云 ARMS、阿里云向量检索数据库、百炼智能体平台等深度集成，提供全面的 AI 智能体企业级生产解决方案，加速智能体从 Demo 走向生产落地。

<p align="center">   
    <img src="https://img.alicdn.com/imgextra/i2/O1CN01sON0wZ21yKROGt2SJ_!!6000000007053-2-tps-5440-2928.png" alt="spring-ai-alibaba-architecture" style="max-width: 700px; height: 400px"/> 
</p>



1. **企业级 MCP 部署与代理方案**：支持基于 Nacos MCP Registry 的分布式部署与负载均衡调用，通过 Spring AI Alibaba MCP Gateway、Higress 代理可实现零改造将 HTTP/Dubbo 服务发布为 MCP；

2. **AI 网关集成提升模型调用稳定性与灵活性**：使用 Higress 作为大模型代理，`spring-ai-starter-model-openai` 可通过 OpenAI 标准接口接入 Higress 代理服务。

3. **降低企业数据整合成本，提升 AI 数据应用效果**；

    - **百炼 RAG 知识库**：私有数据上传百炼平台清洗、切片、向量化管理，通过 Spring AI Alibaba 开发智能体应用并实现 RAG 检索；
    - **百炼析言 ChatBI，从自然语言到 SQL 自动生成**： Spring AI Alibaba Nl2sql 基于百炼析言 ChatBI 技术，可根据自然语言描述生成数据查询 SQL。

4. **可观测与效果评估，加速智能体从 Demo 走向生产落地**： SDK 默认埋点，支持 OpenTelemetry 格式的 tracing 上报，支持接入 Langfuse、阿里云 ARMS 等平台。
## 通用智能体平台

### JManus 智能体平台
Manus 的横空出世，让通用智能体自动规划、执行规划的能力给了人们无限想象空间，它非常擅长解决开放性问题，在日常生活、工作等场景都能有广泛的应用。在我们最开始发布 JManus 之时，给它的定位是一款完全以 Java 语言为核心、彻底开源的 Manus 复刻实现，基于 Spring AI Alibaba 实现的通用 AI Agent 产品，包含一个设计良好的前端 UI 交互界面。

随着对于通用智能体等方向的深度探索，开发者们逐渐开始认识到：基于当前以及未来相当长时间内的模型能力，完全依赖通用智能体的自动规划模式很难解决一些确定性极强的企业场景问题。企业级业务场景的典型特点是确定性，我们需要定制化的工具、子 agent，需要稳定而又确定性强的规划与流程。为此，我们调整了 JManus 通用智能体的终端产品定位。我们期望 JManus 能够成为一个智能体开发平台，让用户能以最直观、低成本的方式构建属于自己的垂直领域的智能体实现。

<p align="center">
    <img src="./docs/imgs/jmanus.png" alt="jmanus" style="max-width: 749px; height: 467px; border-radius: 15px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);" /> 
</p>




### DeepResearch 智能体
Spring AI Alibaba DeepResearch 是一款基于 Spring AI Alibaba Graph 开发的 Deep Research 智能体, 包括完整的前端 Web UI（开发中） 和后端实现，DeepResearch 支持一系列精心设计的工具，如 Web Search（网络查询）、Crawling（爬虫）、Python 脚本引擎等。借助大模型与工具能力，帮助用户完成各类深度调研报告。

<p align="center">
    <img src="./docs/imgs/deepresearch.png" alt="Deep Research" style="max-width: 770px; height: 850px">
</p>

## 贡献指南

Spring AI Alibaba 社区正在快速发展中，我们欢迎任何对 AI 感兴趣的开发者参与建设 Spring AI Alibaba。如果您有兴趣参与建设，请参考 [贡献指南](./CONTRIBUTING.md)。

## 联系我们

* 钉钉群：请通过群号 `124010006813` 搜索入群
* 微信交流群：请扫描以下二维码进群

  <p align="center">
      <img src="./docs/imgs/wechat-account.png" alt="Deep Research" style="max-width: 400px; height: 400px">
  </p>

## 致谢

本项目的一些灵感和代码受到以下项目的启发或基于其重写，非常感谢创建和维护这些开源项目的开发者们。

* [Spring AI](https://github.com/spring-projects/spring-ai)，一款面向 Spring 开发者的 AI 智能体应用开发框架，提供 Spring
  友好的 API 和抽象。基于 Apache License V2 开源协议。
* [Langgraph](https://github.com/langchain-ai/langgraph)，一个用于使用LLM构建有状态、多参与者应用程序的库，用于创建代理和多代理工作流。基于
  MIT 开源协议。
* [Langgraph4J](https://github.com/bsorrentino/langgraph4j)，[LangGraph]项目的 Java 移植版本。基于 MIT 开源协议。
