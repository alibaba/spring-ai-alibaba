# AI powered expert system demo

Spring AI re-implementation of https://github.com/marcushellberg/java-ai-playground

This app shows how you can use Spring AI Alibaba to build an AI-powered system that:

- Has access to terms and conditions (retrieval augmented generation, RAG)
- Can access tools (Java methods) to perform actions (Function Calling)
- Uses an LLM to interact with the user

![alt text](diagram.jpg)

## Requirements

- Java 17+
- Dashscope API key in `AI_DASHSCOPE_API_KEY` environment variable

## Running

Run the app by running `Application.java` in your IDE or `mvn` in the command line.


Add to the POM the Spring AI Alibaba boot starter:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Add the DashScope configuration to the `application.properties`:

```
spring.ai.dashscope.api-key=${AI_DASHSCOPE_API_KEY}
spring.ai.dashscope.chat.options.model=qwen-max
```


## Build Jar

```shell
./mvnw clean install -Pproduction
```

```shell
java -jar ./target/playground-flight-booking-0.0.1-SNAPSHOT.jar
```


```
docker run -it --rm --name postgres -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres ankane/pgvector
```
