package com.alibaba.cloud.ai.vo;

import com.alibaba.cloud.ai.param.RunActionParam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatModelRunResult {

	private RunActionParam input;

	private ActionResult result;

	private TelemetryResult telemetry;

}
