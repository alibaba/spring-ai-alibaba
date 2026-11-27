# ReAct Agent Example

This example showcases basic ReactAgent usage in Spring AI Alibaba.

## Quick Start

### Prerequisites

* JDK 17 or later.
* A DashScope API key. [Get a Bailian API key](https://bailian.console.aliyun.com/?apiKey=1&tab=api#/api)

```shell
export AI_DASHSCOPE_API_KEY=your-api-key
```

### Run the ChatBot

1. Download the code.

```shell
git clone https://github.com/alibaba/spring-ai-alibaba.git
cd spring-ai-alibaba
```

2. Start the ChatBot.

```shell
# Run from repository root.
./mvnw -f examples/chatbot/pom.xml spring-boot:run
```

3. Chat with ChatBot.
Open the browser and visit [http://localhost:8080/chatui/index.html](http://localhost:8080/chatui/index.html) to chat with the ChatBot.

<p align="center">
    <img src="../../docs/imgs/chatbot-chat-ui.gif" alt="chatbot-ui" style="max-width: 740px; height: 508px" />
</p>

## More Examples
Check [spring-ai-alibaba-examples](https://github.com/spring-ai-alibaba/examples/tree/main/spring-ai-alibaba-agent-example) for more sophisticated examples.
