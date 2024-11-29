package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.Workflow;

import java.util.List;

/**
 * WorkflowDelegate defines the workflow crud operations.
 */
public interface WorkflowDelegate {

    default Workflow create(){
        return null;
    }

    default Workflow get(String id){
        return null;
    }

    default List<Workflow> list(){
        return null;
    }

    default Boolean post(Workflow workflow){
        return false;
    }

    default Boolean delete(String id){
        return false;
    }

    default Workflow importDSL(String dsl){
        return null;
    }

    default String exportDSL(String id){
        return "";
    }
}
