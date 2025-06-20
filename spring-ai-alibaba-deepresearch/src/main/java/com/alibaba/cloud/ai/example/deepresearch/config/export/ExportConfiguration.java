package com.alibaba.cloud.ai.example.deepresearch.config.export;

import com.alibaba.cloud.ai.example.deepresearch.service.ExportService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 导出服务配置类
 *
 * @author sixiyida
 * @since 2025/6/20
 *
 */
@Configuration
@EnableConfigurationProperties(ExportProperties.class)
public class ExportConfiguration {

	@Bean
	public ExportService exportService(ExportProperties exportProperties) {
		return new ExportService(exportProperties.getPath());
	}

}