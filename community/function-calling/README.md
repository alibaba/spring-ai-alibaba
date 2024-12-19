此文件记录了 Function Calling 插件的开发规范，请贡献者遵循以下规范编写插件实现。

## 插件开发规范
1. 命名规范
  * 插件命名为：`spring-ai-alibaba-starter-function-caling-${name}`，例如：`spring-ai-alibaba-starter-function-calling-baidusearch`
  * 包名前缀命名为：`com.alibaba.cloud.ai.functioncalling.${name}`，例如：`com.alibaba.cloud.ai.functioncalling.baidusearch`
  * AutoConfiguration 配置类命名为：`${name}AutoConfiguration`，例如：`BaidusearchAutoConfiguration`
  * Function Impl name 命名为：`${name}Service`，通常是由声明 Bean 注解的方法名确定，如 `baiduSearchService`（建议，请根据插件实际情况确定）
  * function bean name 命名为：`${xx}Function`,通常使用 动词+修饰词的方式确定，如`getCurrentLocationUseBaiduMapFunction`(建议，请根据插件实际情况确定)
3. 使用 `@Description("xxx")` 注解描述插件的功能，应提供对插件功能清晰明确的描述，例如：`@Description("百度搜索插件，用于查询百度上的新闻事件等信息")`
4. 如插件自身有配置参数，请使用 `@ConfigurationProperties(prefix = "spring.ai.alibaba.functioncalling.${name}")` 注解，例如：
	```java
	@ConfigurationProperties(prefix = "spring.ai.alibaba.functioncalling.baidusearch")
	public class BaidusearchProperties {}
	```
5. 请在根目录 pom.xml 中添加 module 配置，如 `<module>community/functioncalling/spring-ai-alibaba-starter-functioncalling-baidusearch</module>`
6. 请在插件 pom.xml 文件中只保留必须的传递依赖，插件版本应与 Spring AI Alibaba 统一。
