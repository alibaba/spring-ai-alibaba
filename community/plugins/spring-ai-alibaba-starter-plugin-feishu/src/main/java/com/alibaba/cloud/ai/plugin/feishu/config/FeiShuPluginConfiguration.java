package com.alibaba.cloud.ai.plugin.feishu.config;

import com.lark.oapi.Client;
import com.lark.oapi.core.enums.BaseUrlEnum;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.util.Assert;

/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
@Configuration
@EnableConfigurationProperties(FeiShuProperties.class)
public class FeiShuPluginConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Build FeiShu Client in Spring AI Alibaba") // description
	@ConditionalOnProperty(prefix = FeiShuProperties.FEISHU_PROPERTIES_PREFIX, name = "enabled", havingValue = "true")
	public Client buildDefaultFeiShuClient(FeiShuProperties feiShuProperties) {
		Assert.notNull(feiShuProperties.getAppId(), "FeiShu AppId must not be empty");
		Assert.notNull(feiShuProperties.getAppSecret(), "FeiShu AppSecret must not be empty");
		return Client.newBuilder(feiShuProperties.getAppId(), feiShuProperties.getAppSecret()) // 默认配置为自建应用
			.openBaseUrl(BaseUrlEnum.FeiShu) // 设置域名，默认为飞书
			.logReqAtDebug(true) // 在 debug 模式下会打印 http 请求和响应的 headers、body 等信息。
			.build();
	}
	// 商店应用自行扩展

}
