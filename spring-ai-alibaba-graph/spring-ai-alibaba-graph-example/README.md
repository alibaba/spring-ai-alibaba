## 如何运行

首先，您需要配置模型 API-KEY：

```shell
export AI_DASHSCOPE_API_KEY=xxx
```

然后，就可以在 IDE 中直接运行 `GraphApplication` 类启动示例应用。

或者，您可以运行以下 maven 命令启用示例应用（注意要在根目录先 install 源码）：
```shell
mvn clean install
cd spring-ai-alibaba-graph/spring-ai-alibaba-graph-example
mvn spring-boot:run
```

## 工作流示例（客户评价处理）

以下是工作流架构图：

![](./imgs/workflow-customer-service.png)

示例实现了一个客户评价处理系统，系统接收用户评论，根据评论内容，自动进行问题分类，总共有两级问题分类：
1. 第一级分类节点，将评论分为 positive 和 negative 两种。如果是 positive 评论则进行系统记录后结束流程；如果是 negative 评论则进行第二级分类。
2. 第二级分类节点，根据 negative 评论的具体内容识别用户的具体问题，如 "after-sale service"、"product quality"、"transportation" 等，根据具体问题分流到具体的问题处理节点。
3. 最后问题处理节点进行处理并记录后，流程结束。

浏览器访问如下示例链接，查看运行效果：
* http://localhost:18080/customer/chat?query=我收到的产品有快递破损，需要退换货？
* http://localhost:18080/customer/chat?query=我的产品不能正常工作了，要怎么去做维修？
* http://localhost:18080/customer/chat?query=商品收到了，非常好，下次还会买。

## React Agent 示例

以下是 React Agent 架构图：

![](./imgs/react-agent.png)

在本示例中，我们仅为 Agent 绑定了一个天气查询服务，React Agent 的结束条件也很简单（采用默认行为，模型判断无 tool_call 则结束）。

浏览器访问如下示例链接，查看运行效果：
* http://localhost:18080/react/chat?query=分别帮我查询杭州、上海和南京的天气

## Multi-agent OpenManus 示例

以下是 OpenManus 实现的架构图：

![](./imgs/multi-agent-openmanus.png)

在 OpenManus 示例中，我们实现了一个 multi-agent 系统。其中，有三个核心 agent 互相协作完成用户任务：
1. Planning Agent，负责任务规划
2. Supervisor Agent，负责监督 Executor Agent 完成规划的任务
3. Executor Agent，负责执行每一步任务

浏览器访问如下示例链接，查看运行效果：
* http://localhost:18080/manus/chat?query=帮我查询阿里巴巴近一周的股票信息

## 更多示例
更多示例请关注官网文档更新。
