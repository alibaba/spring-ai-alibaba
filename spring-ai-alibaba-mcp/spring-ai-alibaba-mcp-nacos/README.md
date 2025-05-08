# Spring AI Alibaba Mcp Nacos


```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-mcp-nacos</artifactId>
</dependency>

<!--    WebMvc mode and WebFlux mode, choose one-->
<!--WebMvc SSE-->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webmvc-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>

<!--WebFlux SSE-->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webflux-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

```java
@Service
public class WeatherService {
    
    @Tool(description = "Get weather information by city name")
    public String getWeather(String cityName) {
        return "Sunny";
    }

}


@SpringBootApplication
public class SpringAiMcpApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SpringAiMcpApplication.class, args);
    }
    
    @Bean
    public ToolCallbackProvider weatherTools(WeatherService weatherService) {
        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    }

}
```
application.yaml
```yaml
spring:
  ai:
    mcp:
      server:
        name: webmvc-mcp-server
        version: 1.0.0
        type: SYNC
    alibaba:
      mcp:
        nacos:
          enabled: true
          server-addr: <nacos-sever-addr>
          service-namespace: <nacos-namespace>  
          service-group: <nacos-group>
```
The tools information and server information will be published to ```nacos-default-mcp``` namespace in nacos,
and the service of mcp server will be registered to ```<nacos-group>``` group of ```<nacos-namespace>``` namespace which set in application.yaml.