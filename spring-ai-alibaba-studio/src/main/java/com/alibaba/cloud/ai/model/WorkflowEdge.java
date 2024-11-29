package com.alibaba.cloud.ai.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowEdge {

     private String id;

     private String source;

     private String target;

     private String type;

     private List<Case> cases;

     private Map<String, Object> data;

}

@Data
class Case{
     private String case_id;
     private String id;
     private String logicOperator;
     private List<Condition> conditions;
}

@Data
class Condition{
     private String comparisonOperator;
     private String value;
     private String varType;
     private List<String> variableSelector;
}