package com.alibaba.cloud.ai.studio.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PromptTemplateListRequest {

    /**
     * 查询模式：accurate-精确，blur-模糊
     */
    @Pattern(regexp = "^(accurate|blur)$", message = "搜索模式必须是accurate或blur")
    private String search = "blur";

    /**
     * 标签名称
     */
    private String tag;

    /**
     * Prompt模板Key
     */
    private String promptTemplateKey;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNo = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 10;
}
