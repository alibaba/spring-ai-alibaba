package com.alibaba.cloud.ai.Configure;

import com.alibaba.cloud.ai.properties.LarkSuiteProperties;
import com.alibaba.cloud.ai.service.LarkSuiteService;
import org.springframework.context.annotation.Description;


/**
 * @author 北极星
 */
@EnableConfigurationProperties({LarkSuiteProperties.class})
public class PluginToolAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Description("it calls the document business's to invoke a method to create a larksuite document")
    @ConditionalOnProperty(prefix = "spring.ai.alibaba.plugin.larksuite", name = "enabled", havingValue = "true")
    public LarkSuiteService larkSuiteBuild() {
        return new LarkSuiteService();
    }
}
