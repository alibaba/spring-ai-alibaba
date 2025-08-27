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

package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.adapters;

import com.alibaba.cloud.ai.studio.admin.generator.model.AppMetadata;
import com.alibaba.cloud.ai.studio.admin.generator.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Workflow;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractDSLAdapter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.NodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.Serializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author vlsmb
 * @since 2025/8/27
 */
@Component
public class StudioDSLAdapter extends AbstractDSLAdapter {

    private final List<NodeDataConverter<? extends NodeData>> nodeDataConverters;

    private final Serializer serializer;

    public StudioDSLAdapter(List<NodeDataConverter<? extends NodeData>> nodeDataConverters,
                          @Qualifier("json") Serializer serializer) {
        this.nodeDataConverters = nodeDataConverters;
        this.serializer = serializer;
    }

    @Override
    public AppMetadata mapToMetadata(Map<String, Object> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> metadataToMap(AppMetadata metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Workflow mapToWorkflow(Map<String, Object> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> workflowToMap(Workflow workflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChatBot mapToChatBot(Map<String, Object> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> chatbotToMap(ChatBot chatBot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateDSLData(Map<String, Object> data) {

    }

    @Override
    public Serializer getSerializer() {
        return this.serializer;
    }

    @Override
    public Boolean supportDialect(DSLDialectType dialectType) {
        return DSLDialectType.STUDIO.equals(dialectType);
    }
}
