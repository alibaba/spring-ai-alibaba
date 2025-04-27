# Spring AI Alibaba OpenManus

The Spring AI implementation of <a href="https://github.com/mannaandpoem/OpenManus/" target="_blank">OpenManus</a>.

https://github.com/user-attachments/assets/fc8153bc-8353-4d5c-8789-755d2fd7c4f3

## How to Run

### Prerequisites

1. Make sure you have JDK 17 or later installed.
2. Install npx globally using npm:
   ```shell
   npm install -g npx
   ```
3. Set <a href="https://help.aliyun.com/zh/model-studio/getting-started/first-api-call-to-qwen" target="_blank">dashscope api key</a>.

 ```shell
 export AI_DASHSCOPE_API_KEY=xxxx
 ```

4. [Optional] Set <a href="https://serpapi.com/users/sign_in" target="_blank">SerpApi key</a>, register to get free tokens for each month.

 ```shell
 export SERP_API_KEY=xxxx
 ```

5. [Optional] Set <a href="https://lbsyun.baidu.com/apiconsole/key" target="_blank">Baidu Map key</a>. Modify the "ak" in the mcp-servers-config.json file.


 ```shell
 "BAIDU_MAP_API_KEY": "your_baidu_AK"
 ```

### Run with IDE

Import the this module as an independent project into your favorite IDE.

Open `OpenManusSpringBootApplication` in the editor and click `run`.

### Run with Maven

```shell
mvn spring-boot:run
```

## Architecture

![aaa](https://github.com/user-attachments/assets/4ad14a72-667b-456e-85c1-b05eef8fd414)
