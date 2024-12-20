package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * TongYi Image API properties.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

@ConfigurationProperties(DashScopeImageProperties.CONFIG_PREFIX)
public class DashScopeImageProperties extends DashScopeParentProperties {

	/**
	 * Spring AI Alibaba configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.image";

	/**
	 * Default DashScope Chat model.
	 */
	public static final String DEFAULT_IMAGES_MODEL_NAME = ImageSynthesis.Models.WANX_V1;

	/**
	 * Enable DashScope ai images client.
	 */
	private boolean enabled = true;

	@NestedConfigurationProperty
	private DashScopeImageOptions options = DashScopeImageOptions.builder()
		.withModel(DEFAULT_IMAGES_MODEL_NAME)
		.withN(1)
		.build();

	public DashScopeImageOptions getOptions() {

		return this.options;
	}

	public void setOptions(DashScopeImageOptions options) {

		this.options = options;
	}

	public boolean isEnabled() {

		return this.enabled;
	}

	public void setEnabled(boolean enabled) {

		this.enabled = enabled;
	}

}
