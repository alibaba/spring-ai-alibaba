package com.alibaba.cloud.ai.observation.handler;

import com.alibaba.cloud.ai.entity.ObservationDetailEntity;
import com.alibaba.cloud.ai.entity.ObservationEntity;
import com.alibaba.cloud.ai.service.impl.ObservationDetailServiceImpl;
import com.alibaba.cloud.ai.service.impl.ObservationServiceImpl;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;

import java.time.Instant;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/26
 */
public class ChatClientObservationContextHandler implements ContextHandler<ChatClientObservationContext>{

    private final ObservationDetailServiceImpl modelObservationDetailService;

    private final ObservationServiceImpl observationService;

    public ChatClientObservationContextHandler(ObservationServiceImpl observationService, ObservationDetailServiceImpl modelObservationDetailService) {
        this.modelObservationDetailService = modelObservationDetailService;
        this.observationService = observationService;
    }

    @Override
    public void handle(ChatClientObservationContext context, long duration) {
        long timestampInMillis = Instant.now().toEpochMilli();

        // 保存主表 ObservationEntity
        ObservationEntity ObservationEntity = buildObservationEntity(context, duration, timestampInMillis);
        observationService.insert(ObservationEntity);

        // 保存详情表 ObservationDetailEntity
        ObservationDetailEntity modelObservationDetailEntity = buildObservationDetailEntity(context, ObservationEntity.getId(), timestampInMillis);
        modelObservationDetailService.insert(modelObservationDetailEntity);
    }

    private ObservationEntity buildObservationEntity(ChatClientObservationContext modelContext, long duration, long timestampInMillis) {
        ObservationEntity entity = new ObservationEntity();
        entity.setName(modelContext.getName());
        entity.setAddTime(timestampInMillis);
        entity.setDuration(duration);

        setIfNotNull(() -> modelContext.getRequest().getUserText(), entity::setUserText);
        setIfNotNull(() -> modelContext.getRequest().getChatOptions().getModel(), entity::setModel);
        // 设置非空字段
        setIfNotNull(() -> {
            Throwable error = modelContext.getError();
            return error != null ? error.toString() : null;
        }, entity::setError);

        return entity;
    }

    private ObservationDetailEntity buildObservationDetailEntity(ChatClientObservationContext modelContext, String modelObservationId, long timestampInMillis) {
        ObservationDetailEntity detailEntity = new ObservationDetailEntity();
        detailEntity.setModelObservationId(modelObservationId);
        detailEntity.setAddTime(timestampInMillis);

        // 设置非空字段
        setIfNotNull(() -> toJsonString(modelContext.getHighCardinalityKeyValues()), detailEntity::setHighCardinalityKeyValues);
        setIfNotNull(() -> toJsonString(modelContext.getLowCardinalityKeyValues()), detailEntity::setLowCardinalityKeyValues);
        setIfNotNull(() -> toJsonString(modelContext.getOperationMetadata()), detailEntity::setOperationMetadata);
        setIfNotNull(() -> toJsonString(modelContext.getRequest()), detailEntity::setRequest);
        setIfNotNull(modelContext::getContextualName, detailEntity::setContextualName);

        return detailEntity;
    }
}
