package com.alibaba.cloud.ai.toolcalling.toutiaonews;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.ai.alibaba.toolcalling.toutiaonews")
public class ToutiaoNewsProperties extends CommonToolCallProperties {

	public ToutiaoNewsProperties() {
		super("https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc");
	}

}
