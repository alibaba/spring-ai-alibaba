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
@TableName("tb_model_observation")
public class ModelObservationEntity{

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String name;

    private Long addTime;

    private Integer parentId;

    private Integer totalTokens;

    private String model;

    private String error;

    /*
    Unit: Milliseconds
     */
    private Long duration;
}
