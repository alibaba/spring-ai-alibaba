package com.alibaba.cloud.ai.entity;

import com.alibaba.excel.annotation.ExcelProperty;
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
@TableName("tb_observation_detail")
public class ObservationDetailEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ExcelProperty("id")
    private String id;

    @ExcelProperty("modelObservationId")
    private String modelObservationId;

    @ExcelProperty("highCardinalityKeyValues")
    private String highCardinalityKeyValues;

    @ExcelProperty("lowCardinalityKeyValues")
    private String lowCardinalityKeyValues;

    @ExcelProperty("operationMetadata")
    private String operationMetadata;

    @ExcelProperty("request")
    private String request;

    @ExcelProperty("response")
    private String response;

    @ExcelProperty("contextualName")
    private String contextualName;

    @ExcelProperty("addTime")
    private Long addTime;

}
