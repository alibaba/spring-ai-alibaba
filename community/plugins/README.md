# Function Calling 插件开发规范

此文档记录了 Function Calling 插件的开发规范，请贡献者遵循以下规范编写插件实现。

---

## **插件开发规范**

### **1. 命名规范**
- **插件命名**：
	- 插件名称：`spring-ai-alibaba-starter-plugin-${pluginName}`
	- 示例：`spring-ai-alibaba-starter-plugin-baidusearch`

- **包名前缀**：
	- 前缀：`com.alibaba.cloud.ai.plugin.${pluginName}`
	- 示例：`com.alibaba.cloud.ai.plugin.baidusearch`

- **AutoConfiguration 配置类命名**：
	- 类名：`${pluginName}AutoConfiguration`
	- 示例：`BaidusearchAutoConfiguration`

- **Function Bean 名称**：
	- 名称：`${pluginName}Service`
	- 通常由声明 `@Bean` 注解的方法名确定。
	- 示例：`baiduSearchService`

---

### **2. 插件功能描述**
- 使用 `@Description("xxx")` 注解提供插件功能的清晰描述。
	- 示例：
	  ```java
      @Description("百度搜索插件，用于查询百度上的新闻事件等信息")
      public class BaidusearchService {}
      ```

---

### **3. 配置参数定义**
- 如果插件需要自定义配置参数，请使用 `@ConfigurationProperties` 注解定义。
	- 前缀：`spring.ai.alibaba.plugin.${pluginName}`
	- 示例：
	  ```java
      @ConfigurationProperties(prefix = "spring.ai.alibaba.plugin.baidusearch")
      public class BaidusearchProperties {
          private String apiKey;
          private String endpoint;
  
          // Getters and Setters
      }
      ```

---

### **4. 模块注册**
- 在项目的 **根目录 `pom.xml`** 文件中添加模块：
  ```xml
  <modules>
      <module>community/plugin/spring-ai-alibaba-starter-plugin-baidusearch</module>
  </modules>
  ```

---

### **5. 依赖管理**
- 插件的 `pom.xml` 文件中仅保留必要的依赖。
- 插件版本必须与 Spring AI Alibaba 的版本保持一致。

---

### **6. 必须使用 @AutoConfiguration**
- 所有插件必须使用 `@AutoConfiguration` 注解，确保通过 Spring Boot 的自动配置机制加载插件。
	- 示例：
	  ```java
      @AutoConfiguration
      @ConditionalOnClass(BaidusearchService.class)
      @EnableConfigurationProperties(BaidusearchProperties.class)
      public class BaidusearchAutoConfiguration {
  
          @Bean
          @Description("百度搜索插件，用于查询百度上的新闻事件等信息")
          public BaidusearchService baiduSearchService(BaidusearchProperties properties) {
              return new BaidusearchService(properties);
          }
      }
      ```

---
通过遵循此规范，贡献者可以确保插件具有良好的健壮性、可维护性，并能够与 Spring AI Alibaba 框架无缝集成。

