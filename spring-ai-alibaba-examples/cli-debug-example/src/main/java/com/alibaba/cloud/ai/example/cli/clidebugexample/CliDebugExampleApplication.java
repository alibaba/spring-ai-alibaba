package com.alibaba.cloud.ai.example.cli.clidebugexample;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.alibaba.cloud.ai"})
@SpringBootApplication
@MapperScan("com.alibaba.cloud.ai.mapper")
public class CliDebugExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(CliDebugExampleApplication.class, args);
    }

}
