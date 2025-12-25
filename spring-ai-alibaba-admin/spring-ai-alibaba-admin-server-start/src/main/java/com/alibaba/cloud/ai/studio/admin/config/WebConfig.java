package com.alibaba.cloud.ai.studio.admin.config;

import com.alibaba.cloud.ai.studio.admin.builder.resolver.ApiModelAttributeMethodArgumentResolver;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(@NotNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ApiModelAttributeMethodArgumentResolver());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源映射
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // 配置前端资源映射 - 优先处理静态文件
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // 跳过 API 路径
                        if (resourcePath.startsWith("console/v1/") || resourcePath.startsWith("api/v1/")
                                || resourcePath.startsWith("api/") || resourcePath.startsWith("initializr/")) {
                            return null;
                        }
                        
                        // 尝试获取请求的资源
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果请求的是文件且存在，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // 对于所有其他路径（前端路由），返回 index.html
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 配置根路径重定向到index.html
        registry.addViewController("/admin")
                .setViewName("forward:/index.html");
    }
}

