package com.alibaba.cloud.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/18
 */
@Data
@TableName("tb_model_observation_detail")
public class ModelObservationDetailEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String modelObservationId;

    private String highCardinalityKeyValues;

    private String lowCardinalityKeyValues;

    private String operationMetadata;

    private String request;

    private String response;

    private String contextualName;

    private Long addTime;

}
