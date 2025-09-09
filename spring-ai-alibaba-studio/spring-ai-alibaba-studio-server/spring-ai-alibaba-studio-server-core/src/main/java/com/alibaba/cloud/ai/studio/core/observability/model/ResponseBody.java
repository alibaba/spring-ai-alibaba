package com.alibaba.cloud.ai.studio.core.observability.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.alibaba.cloud.ai.studio.core.observability.model.ResponseResultType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBody {
    ResponseResultType result;
    String message;
    Object data;
}
