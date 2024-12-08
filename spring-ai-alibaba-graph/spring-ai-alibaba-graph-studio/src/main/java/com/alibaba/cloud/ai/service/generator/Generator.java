package com.alibaba.cloud.ai.service.generator;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.param.GenerateParam;

import java.nio.file.Path;

public interface Generator {

	Boolean supportAppMode(String appMode);

	Path generate(App app, GenerateParam param);

}
