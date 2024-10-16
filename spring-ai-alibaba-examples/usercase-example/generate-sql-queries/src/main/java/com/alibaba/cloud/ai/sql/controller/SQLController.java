package com.alibaba.cloud.ai.sql.controller;

import com.alibaba.cloud.ai.sql.entity.Request;
import com.alibaba.cloud.ai.sql.entity.Response;
import com.alibaba.cloud.ai.sql.service.SQLService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/ai/sql")
public class SQLController {

    @Resource
    private SQLService sqlService;

    @GetMapping
    public Response sql(Request request) throws IOException {

        Response response = sqlService.sql(request);

        if (Objects.isNull(response)) {
            throw new RuntimeException("SQL Example throw exception");
        }

        return response;
    }

}
