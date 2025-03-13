此文件记录了 Tool Calling 插件的开发规范，请贡献者遵循以下规范编写插件实现。

## 插件开发规范

1. 命名规范

* 插件命名为：`spring-ai-alibaba-starter-tool-calling-${name}`，例如：`spring-ai-alibaba-starter-tool-calling-baidusearch`
* 包名前缀命名为：`com.alibaba.cloud.ai.toolcalling.${name}`，例如：`com.alibaba.cloud.ai.toolcalling.baidusearch`
* AutoConfiguration 配置类命名为：`${name}AutoConfiguration`，例如：`BaidusearchAutoConfiguration`
* ToolFunction Impl name 命名为：`${name}Service`，通常是由声明 Bean 注解的方法名确定，如 `baiduSearchService`（建议，请根据插件实际情况确定）
* ToolFunction bean name 命名为：`${xx}Function`,通常使用 动词+修饰词的方式确定，如`getCurrentLocationUseBaiduMapFunction`(建议，请根据插件实际情况确定)

2. 使用 **org.springframework.context.annotation.Description** `@Description("xxx")` 注解描述插件的功能，应提供对插件功能清晰明确的描述，例如：`@Description("百度搜索插件，用于查询百度上的新闻事件等信息")`
3. 如果 Function Impl 实现较为复杂，需要使用一些自定义函数，方法命名规范为：`${name}Tool`，例如：`BaiduSearchTool`, 目录层级和实现类保持一致
4 . 如插件自身有配置参数，请使用 `@ConfigurationProperties(prefix = "spring.ai.alibaba.toolcalling.${name}")` 注解，例如：

    ```java
    @ConfigurationProperties(prefix = "spring.ai.alibaba.toolcalling.baidusearch")
    public class BaidusearchProperties {}
    ```

4. 请在根目录 pom.xml 中添加 module 配置，如 `<module>community/tool-calls/spring-ai-alibaba-starter-tool-calling-baidusearch</module>`
5. 请在插件 pom.xml 文件中只保留必须的传递依赖，插件版本应与 Spring AI Alibaba 统一,插件依赖规范为：序列化与反序列化依赖统一使用 `com.fasterxml.jackson.core:jackson`，日志依赖统一使用 `org.slf4j`，HTTP 客户端依赖统一使用 `org.springframework.boot::webClient`,其余依赖尽可能少引用或不引用
