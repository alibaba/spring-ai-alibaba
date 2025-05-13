---
title: Java 版 Manus 实现来了，Spring AI Alibaba 发布开源 OpenManus 实现
keywords: [Spring AI Alibaba, Manus, OpenManus, openmanus4j, java]
description: "此次官方发布的 Spring AI Alibaba OpenManus 实现，首款 OpenManus Java 版本完整实现，包含完整的多智能体任务规划、思考与执行流程，可以让开发者体验 Java 版本的多智能体效果。它能够根据用户的问题进行分析，操作浏览器，执行代码等来完成复杂任务等。"
author: "刘军"
date: "2025-03-22"
category: article
---

此次官方发布的 [Spring AI Alibaba OpenManus](https://github.com/alibaba/spring-ai-alibaba/tree/main/community/openmanus) 实现，包含完整的多智能体任务规划、思考与执行流程，可以让开发者体验 Java 版本的多智能体效果。它<font style="color:rgb(0, 0, 0);">能够根据用户的问题进行分析，操作浏览器，执行代码等来完成复杂任务等。</font>

**项目源码及体验地址：**[**spring-ai-alibaba-openmanus**](https://github.com/alibaba/spring-ai-alibaba/tree/main/community/openmanus)

## 效果展示
话不多说，先看运行效果，以下是我们通过几个实际问答记录展示的 Spring AI Alibaba OpenManus 实际使用效果。

1. **<font style="color:rgb(0, 0, 0);">打开百度浏览器，在搜索框输入：阿里巴巴最最近一周股价，根据搜索到的信息绘制最近一周的股价趋势图并保存到本地目录。</font>**

![spring ai alibaba openmanus](/img/blog/manus/case1.png)


2. **我计划在接下来的五一劳动节假期到韩国旅行，行程是从杭州出发到韩国首尔，总预算为10000元。我想体验韩国的风土人情、文化、普通老百姓的生活，总行程计划为5天。请提供详细的行程并制作成一个简单的HTML旅行手册，其中包含地图、景点描述、基本的韩语短语和旅行提示，以供我在整个旅程中参考。**

![spring ai alibaba openmanus](/img/blog/manus/case2.png)



3. **在本机的/tmp/docs目录下有一些中文文档 ，请依次将这些文档翻译为中文并保存到一个独立文件，将新生成的文件都存放到/tmp/endocs目录下**

![spring ai alibaba openmanus](/img/blog/manus/case3.png)

## 总体架构与原理
<font style="color:rgb(0, 0, 0);">Spring AI Alibaba Openmanus 与 Python 版本 OpenManus 设计理念相似，其总体架构如下图所示。</font>

![spring ai alibaba openmanus architecture](/img/blog/manus/arch.png)

<font style="color:rgb(0, 0, 0);">分析上图架构，我们可以把它看作是一款多 Agent 智能自动协作实现，其中：</font>

+ Planning Agent 负责任务的分解与规划，将用户问题拆解成几个可顺序执行的 step。planning agent 调用 planning tool 动态生成一个串行的 Manus Agent 子工作流。
+ 多个 Manus Agent 组成一个链式、可顺序依次执行的子工作流。子工作流中的每个 agent 对应上述规划的一个 step，每个 agent 都是一个 ReAct 架构设计，即通过多轮 Tool 调用完成具体子任务。
+ Summary Agent 用来做最后的任务总结



## 实现总结与展望
### Spring AI Alibaba OpenManus 实现中的问题
当前的 OpenManus 实现主要有如下问题：

+ 仓库中 80% 代码都在解决流程编排问题，入串联 manus agent 子流程、做消息记忆、转发工具调用、全局状态修改等，这部分工作可以交给高度抽象的 agent 框架实现，以简化开发复杂度。
+ 工具的覆盖度与执行效果一般，如浏览器使用、脚本执行工具等。
+ 规划及工作流程中无法人为介入进行 review、动态修改、回退等动作。
+ 当前 OpenManus 实现的效果调试相对比较困难。

### Spring AI Alibaba 未来规划与解决方案
Spring AI Alibaba 是面向 Java 开发者的开源 AI 应用开发框架，它与 Spring 生态完美适配，可以基于 Spring AI Alibaba 构建全新的 AI 应用，也可以使用它为传统 Spring Boot 应用做智能化升级。



![spring ai alibaba architecture](/img/blog/manus/design.png)


从上图我们可以看出，除了框架原子抽象之外，Spring AI Alibaba 重点规划了 multi-agent 框架，配套生态如可视化评估平台、调试 Studio 等。

接下来，我们将会发布 Spring AI Alibaba Graph 多 agent 框架，以及基于 Spring AI Alibaba Graph 的强化版 OpenManus 实现，预期代码量将比当前减少 70% 以上，整体易读性与效果大幅提升，让开发者可以此为基础构建面向任意场景的智能体应用。

目前 Spring AI Alibaba 已经支持 MCP 工具接入，解析来我们将为 OpenManus 接入更成熟的 MCP server 实现，以提升整体工作表现。





