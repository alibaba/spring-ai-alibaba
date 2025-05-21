package com.alibaba.cloud.ai.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

	private String id;

	private String vectorType;

}
