package com.alibaba.cloud.ai.utils;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

@Component
public class OpenApiGenerator implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        String apiDocsUrl = "http://localhost:8080/v3/api-docs.yaml"; // OpenAPI 文档 URL
        String outputDir = getOutputDir(); // 获取动态路径
        String outputFileName = "openapi.yaml"; // 输出文件名

        generateOpenApiFile(apiDocsUrl, outputDir, outputFileName);
    }

    private String getOutputDir() {
        // 获取当前工作目录，即项目根目录
        String baseDir = System.getProperty("user.dir");

        // 拼接成目标目录路径
        return Paths.get(baseDir, "spring-ai-alibaba-graph-studio", "src", "main", "resources").toString();
    }

    private void generateOpenApiFile(String apiDocsUrl, String outputDir, String outputFileName) throws IOException {
        // 获取 OpenAPI 文档内容
        RestTemplate restTemplate = new RestTemplate();
        String openApiYaml = restTemplate.getForObject(apiDocsUrl, String.class);

        // 创建输出目录
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        // 创建输出文件
        File outputFile = new File(outputDirectory, outputFileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            if (openApiYaml != null) {
                fos.write(openApiYaml.getBytes());
            }
            fos.flush();
        }
    }
}
