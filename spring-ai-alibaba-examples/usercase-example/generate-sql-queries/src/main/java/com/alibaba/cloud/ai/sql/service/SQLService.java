package com.alibaba.cloud.ai.sql.service;

import com.alibaba.cloud.ai.sql.entity.Request;
import com.alibaba.cloud.ai.sql.entity.Response;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;

@Service
public class SQLService {

    @Value("classpath:schema.sql")
    private Resource ddlResource;

    @Value("classpath:/prompt/sql-queries.st")
    private Resource sqlPromptTemplateResource;

    private final ChatClient aiClient;
    private final JdbcTemplate jdbcTemplate;

    public SQLService(ChatClient.Builder aiClientBuilder, JdbcTemplate jdbcTemplate) {
        this.aiClient = aiClientBuilder.build();
        this.jdbcTemplate = jdbcTemplate;
    }

    public Response sql(Request request) throws IOException {
        String schema = ddlResource.getContentAsString(Charset.defaultCharset());

        String query = aiClient.prompt()
                .advisors(new SimpleLoggerAdvisor())
                .user(userSpec -> userSpec
                        .text(sqlPromptTemplateResource)
                        .param("question", request.text())
                        .param("ddl", schema)
                )
                .call()
                .content();

        if (query.toLowerCase().startsWith("select")) {

            return new Response(query, jdbcTemplate.queryForList(query));
        }

        throw new RuntimeException(query);
    }

}
