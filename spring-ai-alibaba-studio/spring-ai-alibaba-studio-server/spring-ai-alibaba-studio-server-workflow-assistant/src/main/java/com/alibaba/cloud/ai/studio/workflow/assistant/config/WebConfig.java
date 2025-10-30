package com.alibaba.cloud.ai.studio.workflow.assistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for handling CORS (Cross-Origin Resource Sharing) requests.
 * This configuration allows the frontend to make requests to the backend from different origins.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure CORS mappings to allow cross-origin requests.
     *
     * @param registry the CorsRegistry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // For development: allow all localhost origins with any port
                .allowedOriginPatterns(
                        "http://localhost:*",       // Any localhost port
                        "http://127.0.0.1:*",      // Any 127.0.0.1 port
                        "https://localhost:*",      // HTTPS localhost
                        "https://127.0.0.1:*",// HTTPS 127.0.0.1
                        "http://192.168.32.54:*"
                )
                // Allow all HTTP methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                // Allow common headers
                .allowedHeaders("*")
                // Allow credentials (cookies, authorization headers)
                .allowCredentials(true)
                // Cache preflight response for 1 hour
                .maxAge(3600);
    }
}
