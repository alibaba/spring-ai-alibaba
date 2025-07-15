package com.alibaba.cloud.ai.example.manus.event;

import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;

/**
 * @author dahua
 * @time 2025/7/15
 * @desc jmanus模型变化事件类
 */
public class ModelChangeEvent implements JmanusEvent {

    private DynamicModelEntity dynamicModelEntity;
    private long createTime;

    public ModelChangeEvent(DynamicModelEntity dynamicModelEntity) {
        this.dynamicModelEntity = dynamicModelEntity;
        this.createTime = System.currentTimeMillis();
    }

    public DynamicModelEntity getDynamicModelEntity() {
        return dynamicModelEntity;
    }

    public long getCreateTime() {
        return createTime;
    }
}
