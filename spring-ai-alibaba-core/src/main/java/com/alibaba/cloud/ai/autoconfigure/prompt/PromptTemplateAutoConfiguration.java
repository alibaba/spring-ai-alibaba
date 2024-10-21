/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
package com.alibaba.cloud.ai.autoconfigure.prompt;

import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplateFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * .
 *
 * @date: 2024-09-20
 * @version: 1.0
 * @author: KrakenZJC
 **/

public class PromptTemplateAutoConfiguration {
<<<<<<< Updated upstream
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.nacos.prompt.template", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public ConfigurablePromptTemplateFactory configurablePromptTemplateFactory(){
=======
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.ai.nacos.prompt.template", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ConfigurablePromptTemplateFactory configurablePromptTemplateFactory() {
>>>>>>> Stashed changes
        return new ConfigurablePromptTemplateFactory();
    }
}