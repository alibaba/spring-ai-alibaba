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
package com.alibaba.cloud.ai.dashscope.image.observation;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.fastjson.JSON;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * Dashscope conventions to populate observations for Image model operations.
 *
 * @author Lumian
 * @since 1.0.0
 */
public class DashScopeImageModelObservationConvention extends DefaultImageModelObservationConvention {

    public static final String DEFAULT_NAME = "gen_ai.client.operation";

    private static final String ILLEGAL_STOP_CONTENT = "<illegal_stop_content>";

    @Override
    public String getName () {
        return DEFAULT_NAME;
    }
}
