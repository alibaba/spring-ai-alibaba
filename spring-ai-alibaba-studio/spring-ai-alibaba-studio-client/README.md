# spring-ai-alibaba-studio 后端

## 本地运行

spring-ai-alibaba-studio 后端是嵌入式server，需外部SpringBoot项目引入该依赖后运行。

具体可参考`spring-ai-alibaba-examples/cli-debug-example` 示例，配置好`spring.ai.dashscope.api-key=xxxx`
后运行`CliDebugExampleApplication.java`即可。

## 生成openapi文档

1. 运行项目
2. 运行`mvn verify`
3. openapi.yaml生成在`resources/openapi.yaml`
