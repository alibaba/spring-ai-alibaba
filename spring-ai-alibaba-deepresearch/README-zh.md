本项目利用spring-ai-alibaba-graph实现深度研究

### 配置

需要配置以下环境变量，或者直接修改`application.yml`:

- DashScope API: `${AI_DASHSCOPE_API_KEY}`
- TavilySearch API: `${TAVILY_API_KEY}`

此外需要安装`docker`，需要在配置文件的`spring.ai.alibaba.deepreserch.python-coder.docker-host`字段中设置DockerHost，默认为`unix:///var/run/docker.sock`。
本项目需要使用`python:3-slim`镜像创建临时容器，也可以自己定制包含一些常用的第三方库的镜像，第三方库需要安装在镜像的`/app/dependency`文件夹里，在配置文件中设置`spring.ai.alibaba.deepreserch.python-coder.image-name`的值指定镜像名称。

### Docker
- 在deepresearch项目工程目录下执行构建命令
```shell
###构建alibaba-deepresearch:v1.0版本镜像
docker build -t alibaba-deepresearch:v1.0 . 
```
- 构建完成后，执行docker run命令启动镜像，设置环境变量
```shell
###需替换your keys
docker run -d \
  --name alibaba-deepresearch \
  -e AI_DASHSCOPE_API_KEY="your_key_here" \
  -e TAVILY_API_KEY="your_key_here" \
  -e JINA_API_KEY="your_key_here" \
  -p 8080:8080 \
  alibaba-deepresearch:v1.0
```


### 工具
- tavily API文档：https://docs.tavily.com/documentation/api-reference/endpoint/search
- Jina API文档：https://jina.ai/reader
- 高德地图MCP：https://lbs.amap.com/api/mcp-server/gettingstarted#t1

macos:

mcp-servers.json

```json
{
  "mcpServers": {
    "amap-maps": {
      "command": "npx",
      "args": ["-y", "@amap/amap-maps-mcp-server"],
      "env": {
        "AMAP_MAPS_API_KEY": "AMAP_MAPS_API_KEY"
      }
    }
  }
}
```

windows:

mcp-servers-windows.json

记得删掉`-windows`，命名和macos的一致，只需要保留一个文件即可

```json
{
  "mcpServers": {
    "amap-maps": {
      "command": "cmd",
      "args": ["/c","npx","-y","@amap/amap-maps-mcp-server"],
      "env": {
        "AMAP_MAPS_API_KEY": "AMAP_MAPS_API_KEY"
      }
    }
  }
}
```

### 完整系统流程图

![image-20250605212205834](../docs/imgs/202506052122988.png)

### 整体架构图：

![image-20250605212328282](../docs/imgs/202506052123329.png)



### Contributors

GitHub:
- [yingzi](https://github.com/GTyingzi)
- [zhouyou](https://github.com/zhouyou9505)
- [NOBODY](https://github.com/SCMRCORE)
- [xiaohai-78](https://github.com/xiaohai-78)
- [VLSMB](https://github.com/VLSMB)
- [disaster1-tesk](https://github.com/disaster1-tesk)
- [Allen Hu](https://github.com/big-mouth-cn)
- [Makoto](https://github.com/zxuexingzhijie)
- [sixiyida](https://github.com/sixiyida)
