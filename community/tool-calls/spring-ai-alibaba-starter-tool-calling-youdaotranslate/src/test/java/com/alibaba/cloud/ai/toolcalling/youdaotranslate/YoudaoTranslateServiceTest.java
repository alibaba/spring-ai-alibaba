/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.toolcalling.youdaotranslate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link YoudaoTranslateService}.
 *
 * @author zhangshenghang
 */
class YoudaoTranslateServiceTest {

    @Test
    void apply() {
        String appKey = System.getenv("YOUDAO_APP_KEY");
        if (appKey == null || appKey.isEmpty()) {
            appKey = System.getenv("YOUDAO_API_KEY");
        }
        
        String appSecret = System.getenv("YOUDAO_APP_SECRET");
        if (appSecret == null || appSecret.isEmpty()) {
            appSecret = System.getenv("YOUDAO_API_SECRET");
        }
        
        YoudaoTranslateProperties youdaoTranslateProperties = new YoudaoTranslateProperties();
        youdaoTranslateProperties.setAppKey(appKey);
        youdaoTranslateProperties.setAppSecret(appSecret);
        YoudaoTranslateService youdaoTranslateService = new YoudaoTranslateService(youdaoTranslateProperties);
        YoudaoTranslateService.Request request = new YoudaoTranslateService.Request("你好","zh-CHS","en");
        YoudaoTranslateService.Response apply = youdaoTranslateService.apply(request);
        Assertions.assertNotNull(apply);
        Assertions.assertTrue(apply.translatedTexts().get(0).toLowerCase().contains("hello"));
    }
}