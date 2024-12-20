package com.alibaba.cloud.ai.dashscope.rag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author nuocheng.lxm
 * @since 2024/7/22 15:14 百炼文档解析相关配置项
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeDocumentCloudReaderOptions {

	/**
	 * 类目ID 如果没有指定类目则会上传到默认类目
	 */
	private @JsonProperty("category_id") String categoryId;

	public DashScopeDocumentCloudReaderOptions() {
		this.categoryId = "default";
	}

	public DashScopeDocumentCloudReaderOptions(String categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryId() {
		return categoryId;
	}

}
