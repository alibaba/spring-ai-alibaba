# Spring AI Alibaba Mcp Nacos


```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-mcp-dynamic-server</artifactId>
</dependency>

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


@SpringBootApplication
public class SpringAiMcpApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SpringAiMcpApplication.class, args);
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
    alibaba:
      mcp:
        nacos:
          enabled: true
          server-addr: <nacos-sever-addr>
          service-namespace: <nacos-namespace>  
          service-group: <nacos-group>
```

The server can add tools or remove tools dynamically

read instance info from nacos service list and tools info from nacos config