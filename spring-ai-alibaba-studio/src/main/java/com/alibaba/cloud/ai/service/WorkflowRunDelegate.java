package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.WorkflowRunEvent;

/**
 * WorkflowDelegate defines the workflow execution operations.
 */
public interface WorkflowRunDelegate {

    WorkflowRunEvent run(String id);

}
