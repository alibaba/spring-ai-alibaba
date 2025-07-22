package com.alibaba.cloud.ai.studio.core.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("provider")
public class ProviderEntity {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("workspace_id")
	private String workspaceId;

	private String icon;

	private String name;

	private String description;

	private String provider;

	private Boolean enable;

	/**
	 * 协议，默认openai协议
	 */
	private String protocol = "openai";

	private String source;

	// 支持的模型类型列表，逗号分隔
	@TableField("supported_model_types")
	private String supportedModelTypes;

	private String credential;

	@TableField("gmt_create")
	private Date gmtCreate;

	@TableField("gmt_modified")
	private Date gmtModified;

	private String creator;

	private String modifier;

}
