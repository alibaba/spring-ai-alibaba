package com.alibaba.cloud.ai.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
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
@TableName("tb_observation")
public class ObservationEntity {

	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@ExcelProperty("id")
	private String id;

	@ExcelProperty("name")
	private String name;

	@ExcelProperty("userText")
	private String userText;

	@ExcelProperty("parentId")
	private Integer parentId;

	@ExcelProperty("totalTokens")
	private Integer totalTokens;

	@ExcelProperty("model")
	private String model;

	@ExcelProperty("error")
	private String error;

	/*
	 * Unit: Milliseconds
	 */
	@ExcelProperty("duration")
	private Long duration;

	@ExcelProperty("addTime")
	private Long addTime;

}
