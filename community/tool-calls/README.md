此文件记录了 Tool Calling 插件的开发规范，请贡献者遵循以下规范编写插件实现。

## 插件开发规范

1. 命名规范

* 插件命名为：`spring-ai-alibaba-starter-tool-calling-${name}`，例如：`spring-ai-alibaba-starter-tool-calling-baidusearch`
* 包名前缀命名为：`com.alibaba.cloud.ai.toolcalling.${name}`，例如：`com.alibaba.cloud.ai.toolcalling.baidusearch`
* AutoConfiguration 配置类命名为：`${name}AutoConfiguration`，例如：`BaiduSearchAutoConfiguration`
* Properties 类命名为：`${name}Properties`，例如：`BaiduSearchProperties`。
* ToolFunction Impl name 命名为：`${name}Service`，通常是由声明 Bean 注解的方法名确定，如 `baiduSearchService`（建议，请根据插件实际情况确定）
* ToolFunction bean name 命名为：`${name}`。
* Constants 类命名为：``。
* 单元测试类的名称为：``。

2. 使用 **org.springframework.context.annotation.Description** `@Description("xxx")` 注解描述插件的功能，应提供对插件功能清晰明确的描述，例如：`@Description("百度搜索插件，用于查询百度上的新闻事件等信息")`
3. 如果 Function Impl 实现较为复杂，需要使用一些自定义函数，方法命名规范为：`${name}Tool`，例如：`BaiduSearchTool`, 目录层级和实现类保持一致
4. 请在根目录 pom.xml 中添加 module 配置，如 `<module>community/tool-calls/spring-ai-alibaba-starter-tool-calling-baidusearch</module>`
5. 请在插件 pom.xml 文件中只保留必须的传递依赖，插件版本应与 Spring AI Alibaba 统一，日志依赖统一使用 `org.slf4j`，其余依赖尽可能少引用或不引用

6. Properties配置属性类应并继承来自`spring-ai-alibaba-starter-tool-calling-common`模块的`CommonToolCallProperties`，并且需要写上`@ConfigurationProperties(prefix = ${name}Properties.${NAME}_PREFIX)`。
即使这个配置类不需要额外的属性字段，也需要定义一个`CommonToolCallProperties`的子类。在Properties类中定义一个属性前缀常量，构造方法中初始化该服务的基础URL，以及部分重要属性用户没有给出时可以根据哪个环境变量读取。之后该类可以定义自己独有的属性。一个Properties类的实例如下：

```java
@ConfigurationProperties(prefix = BaiDuTranslatePrefix)
public class BaiduTranslateProperties extends CommonToolCallProperties {
   protected static final String BaiDuTranslatePrefix = TOOL_CALLING_CONFIG_PREFIX + ".baidu.translate";
   private static final String TRANSLATE_HOST_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate/";

   public BaiduTranslateProperties() {
      super(TRANSLATE_HOST_URL);
      this.setPropertiesFromEnv(null, "BAIDU_TRANSLATE_SECRET_KEY", "BAIDU_TRANSLATE_APP_ID", null);
   }
}
```

7. 在Function Impl中，JSON的序列化与反序列化统一使用`spring-ai-alibaba-starter-tool-calling-common`模块的`JsonParseTool`对象，common模块自动注入了一个默认的Bean，如果有特殊需求也可以自定义`objectMapper`，在`AutoConfiguration`中覆盖原有的`JsonParseTool`的Bean。
HTTP请求统一使用common模块的`RestClientTool`或者`WebClientTool`的对象，该类有`builder`方法，必要`CommonToolCallProperties`和`JsonParseTool`对象，根据需要也可以自定义其他对象。
8. Auto Configuration 类中，应该声明一个Function Impl的Bean，供用户使用，且 Bean 的名称应该在对应的 Constants 类给出，常量的名称应为`TOOL_NAME`。例如：

```java
@Configuration
@EnableConfigurationProperties(BaiduTranslateProperties.class)
@ConditionalOnProperty(prefix = BaiduTranslateConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
  matchIfMissing = true)
public class BaiduTranslateAutoConfiguration {
 @Bean(name = BaiduTranslateConstants.TOOL_NAME)
 @ConditionalOnMissingBean
 @Description("Baidu translation function for general text translation")
 public BaiduTranslateService baiduTranslate(BaiduTranslateProperties properties, JsonParseTool jsonParseTool) {

  return new BaiduTranslateService(properties, RestClientTool.builder(jsonParseTool, properties).build(),
    jsonParseTool);
 }
}
```

如果该插件有多个 ToolFunction Bean，Constants 类可以分别定义每个 Bean 名称的常量，但必须以 `_TOOL_NAME` 结尾，比如`GET_ADDRESS_TOOL_NAME`。

9. 对于多个模块可能共用的方法，应该写到common模块的`CommonToolCallUtils`类中。对于多个模块可能共用的常量，应该写到common模块的`CommonToolCallConstants`中。
10. 对于 Properties 类中需要用户隐私的信息（比如 API Key），需要在 Constants 类定义一个环境变量名词，并在 Properties 类的构造方法中读取系统环境变量的值，例如：

```java
// BaiduTranslateConstants
public static final String SECRET_KEY_ENV = "BAIDU_TRANSLATE_SECRET_KEY";

// BaiduTranslateProperties
public BaiduTranslateProperties() {
 this.secretKey = System.getenv(SECRET_KEY_ENV);
}
```

11. 每一个插件都需要编写单元测试类。对于需要用户隐私的信息（比如 API Key）的插件，可以在测试方法上标注 `@EnabledIfEnvironmentVariable(named = XXXConstants.API_KEY_ENV, matches = CommonToolCallConstants.NOT_BLANK_REGEX)` 注解，保证自己本地能够通过单元测试。
