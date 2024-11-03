package com.alibaba.cloud.ai.vo;

import lombok.Data;

import java.util.List;

@Data
public class ActionResult {

	private String Response;

	private List<String> streamResponse;

}
