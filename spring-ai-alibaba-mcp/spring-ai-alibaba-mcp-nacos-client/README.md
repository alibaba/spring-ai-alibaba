The Spring AI Alibaba Mcp Nacos Client supports multi-node deployment of MCP Server


### The name of the MCP Server service registered in nacos
```yaml
spring:
  ai:
    alibaba:
      mcp:
        nacos:
          client:
            sse:
              connections:
                nacos-server1: mcp-server1
                nacos-server2: mcp-server2
```

### Connect the basic configuration of Nacos...
Detailed configuration items can be found in the spring-ai-alibaba-macp-nacos module
```yaml
spring:
  ai:       
    alibaba:
      mcp:
        nacos:
          enabled: true
          server-addr: <nacos-sever-addr>
          service-namespace: <nacos-namespace>  
          service-group: <nacos-group>
```

### Enable multiple nodes
```yaml
spring:
  ai:
    mcp:
      client:
        nacos-enabled: true
```





### The complete yaml configuration file is as follows
```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: mcp-client-webflux
        version: 1.0.0
        type: SYNC
        nacos-enabled: true
        
    alibaba:
      mcp:
        nacos:
          enabled: true
          server-addr: <nacos-sever-addr>
          service-namespace: <nacos-namespace>  
          service-group: <nacos-group>

        client:
          sse:
            connections:
              nacos-server1: mcp-server1
              nacos-server2: mcp-server2
```