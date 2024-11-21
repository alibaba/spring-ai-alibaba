package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.Workflow;

/**
 * DSLAdapter is used to convert workflow to different dsl(dify, flowise, etc.) and vice versa.
 */
public interface DSLAdapter {

    String toDSL(Workflow workflow);

    Workflow fromDSL(String dsl);
}
