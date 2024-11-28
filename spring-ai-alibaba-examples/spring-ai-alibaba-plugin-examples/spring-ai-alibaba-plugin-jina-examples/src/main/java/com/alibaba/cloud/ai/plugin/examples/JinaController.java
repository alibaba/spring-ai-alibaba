package com.alibaba.cloud.ai.plugin.examples;

import com.alibaba.cloud.ai.plugin.jina.service.JinaService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RequestMapping("/ai")
@RestController
public class JinaController {

	private final JinaService jinaService;

	public JinaController(JinaService jinaService) {
		this.jinaService = jinaService;
	}

	@GetMapping("/jina")
	public String jinaService() {

		return jinaService.spiderByJina("https://www.baidu.com");
	}

}
