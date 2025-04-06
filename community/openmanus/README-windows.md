# Windows 环境下启动 Spring AI Alibaba OpenManus

## 1. 启动时需要 jdk17 或者更高版本；

```shell
$ java -version

java version "17.0.1" 2021-10-19 LTS
Java(TM) SE Runtime Environment (build 17.0.1+12-LTS-39)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.1+12-LTS-39, mixed mode, sharing)
```

## 2. MCP 相关配置

系统中集成 MCP，在 windows 中需要更新对应的 command：

全局安装 npx 工具：

```shell
npm install -g npx
```

更新 `community/openmanus/src/main/resources/mcp-servers-config.json` 为

```json
{
    "mcpServers": {
        "baidu-map": {
            "command": "npx.cmd",
            "args": [
                "-y",
                "@baidumap/mcp-server-baidu-map"
            ],
            "env": {
                "BAIDU_MAP_API_KEY": "your_baidu_AK"
            }
        }
    }
}
```

`BAIDU_MAP_API_KEY`：需要在[百度地图](https://lbsyun.baidu.com/apiconsole/key)上申请。

## 3. SERP_API_KEY

系统中搜索相关的服务 key，申请地址：https://serpapi.com/users/sign_in，
PS：收不到邮箱验证码，查看垃圾邮件。

## 4. 启动

启动类位置：`community/openmanus/src/main/java/com/alibaba/cloud/ai/example/manus/OpenManusSpringBootApplication.java` 点击启动即可。

> 如果 openManus 有生成文件操作，生成的文件路径应该在项目中的 `extensions` 路径下。

## 报错处理

1. Failed to create ChromeDriver instance

```text
org.openqa.selenium.SessionNotCreatedException: Could not start a new session. Response code 500. Message: session not created: This version of ChromeDriver only supports Chrome version 118
Current browser version is 135.0.7049.41 with binary path C:\Program Files\Google\Chrome\Application\chrome.exe 
```

下载合适的浏览器驱动版本，当前系统浏览器不受支持。下载地址：`https://googlechromelabs.github.io/chrome-for-testing/`；
在浏览器地址栏收入 `chrome://version/` 查看当前系统浏览器版本。
