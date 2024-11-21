package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.Workflow;

import java.util.List;

/**
 * WorkflowDelegate defines the workflow crud operations.
 */
public interface WorkflowDelegate {

    Workflow create();

    Workflow get(String id);

    List<Workflow> list();

    Boolean post(Workflow workflow);

    Boolean delete(String id);

    Workflow importDSL(String dsl);

    String exportDSL(String id);
}
