package com.alibaba.cloud.ai.toolcalling.sinanews;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;

public class SinaNewsProperties extends CommonToolCallProperties {

	public SinaNewsProperties() {
		super("https://newsapp.sina.cn/api/hotlist?newsId=HB-1-snhs%2Ftop_news_list-all");
	}

}
