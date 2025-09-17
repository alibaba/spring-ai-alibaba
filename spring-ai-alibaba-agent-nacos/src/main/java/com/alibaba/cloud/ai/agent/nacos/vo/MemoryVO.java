package com.alibaba.cloud.ai.agent.nacos.vo;

import lombok.Data;

@Data
public class MemoryVO {

	String storageType;

	String address;

	String credential;

	String compressionStrategy;

	String searchStrategy;

}
