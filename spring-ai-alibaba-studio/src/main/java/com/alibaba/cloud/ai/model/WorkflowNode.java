package com.alibaba.cloud.ai.model;

import lombok.Data;

import java.util.Map;


@Data
public class WorkflowNode {

    private String id;

    private String type;

    private Map<String, Object> data;

    private Float width;

    private Float height;

    private Coordinate position;

    private Coordinate positionAbsolute;

    private Boolean selected;

    private Integer zIndex;

    private String sourcePosition;

    private String targetPosition;

}

@Data
class Coordinate {
    private Float x;
    private Float y;
}
