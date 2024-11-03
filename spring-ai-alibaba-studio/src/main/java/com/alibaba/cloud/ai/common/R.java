package com.alibaba.cloud.ai.common;

import lombok.Data;

@Data
public class R<T> {

	private Integer code;

	private String msg;

	private T data;

	private long timestamp;

	public R() {
		this.timestamp = System.currentTimeMillis();
	}

	public static <T> R<T> success(T data) {
		R<T> r = new R<>();
		r.setCode(ReturnCode.RC200.getCode());
		r.setMsg(ReturnCode.RC200.getMsg());
		r.setData(data);
		return r;
	}

	public static <T> R<T> error(int code, String msg) {
		R<T> r = new R<>();
		r.setCode(code);
		r.setMsg(msg);
		r.setData(null);
		return r;
	}

}
