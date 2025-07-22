package com.alibaba.cloud.ai.studio.core.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("model")
public class ModelEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("workspace_id")
	private String workspaceId;

	private String icon;

	private String name;

	@TableField("model_id")
	private String modelId;

	private String provider;

	private String type;

	private Boolean enable;

	private String tags;

	private String mode;

	private String source;

	@TableField("gmt_create")
	private Date gmtCreate;

	@TableField("gmt_modified")
	private Date gmtModified;

	private String creator;

	private String modifier;

}
