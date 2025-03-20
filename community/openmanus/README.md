# Spring AI Alibaba OpenManus
The Spring AI implementation of <a href="https://github.com/mannaandpoem/OpenManus/" target="_blank">OpenManus</a>.

## How to Run

### Prerequisites
1. Make sure you have JDK 17 or later installed.
2. Set <a href="https://help.aliyun.com/zh/model-studio/getting-started/first-api-call-to-qwen" target="_blank">dashscope api key</a>.

	```shell
	export AI_DASHSCOPE_API_KEY=xxxx
	```
3. [Optional] Set <a href="https://serpapi.com/users/sign_in" target="_blank">SerpApi key</a>, register to get free tokens for each month.

	```shell
	export SERP_API_KEY=xxxx
	```

### Run with IDE
Import the this module as an independent project into your favorite IDE.

Open `OpenManusSpringBootApplication` in the editor and click `run`.

### Run with Maven

```shell
mvn spring-boot:run
```