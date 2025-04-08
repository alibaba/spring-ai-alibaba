package com.alibaba.cloud.ai.example.graph.react.tool.weather.function;

import cn.hutool.extra.pinyin.PinyinUtil;

public class WeatherUtils {

	public static String preprocessLocation(String location) {
		if (containsChinese(location)) {
			return PinyinUtil.getPinyin(location, "");
		}
		return location;
	}

	public static boolean containsChinese(String str) {
		return str.matches(".*[\u4e00-\u9fa5].*");
	}

}
