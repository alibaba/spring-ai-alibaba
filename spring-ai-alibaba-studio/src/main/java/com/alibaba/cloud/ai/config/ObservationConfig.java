package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.mapper.ModelObservationDetailMapper;
import com.alibaba.cloud.ai.mapper.ModelObservationMapper;
import com.alibaba.cloud.ai.observation.AlibabaObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/18
 */
@Configuration
public class ObservationConfig {

    @Bean
    public ObservationRegistry observationRegistry(ModelObservationMapper modelObservationMapper, ModelObservationDetailMapper modelObservationDetailMapper) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig().observationHandler(new AlibabaObservationHandler(modelObservationMapper, modelObservationDetailMapper));
        return observationRegistry;
    }

}
