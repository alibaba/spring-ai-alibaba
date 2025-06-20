package com.alibaba.cloud.ai.example.deepresearch.config.export;

import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 导出功能相关的配置属性
 *
 * @author sixiyida
 * @since 2025/6/20
 */
@ConfigurationProperties(prefix = ExportProperties.EXPORT_PREFIX)
public class ExportProperties {

	public static final String EXPORT_PREFIX = DeepResearchProperties.PREFIX + ".export";

	private String path = "${user.home}/reports";

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}