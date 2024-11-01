package com.zhych.aimemory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(value = {"com.alibaba.cloud.ai.memory.*", "com.zhych.aimemory.*"})
@MapperScan(value = "com.alibaba.cloud.ai.memory.mapper")
@SpringBootApplication
public class AimemoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AimemoryApplication.class, args);
    }

}
