package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.observation.AlibabaObservationHandler;
import com.alibaba.cloud.ai.service.impl.ModelObservationDetailServiceImpl;
import com.alibaba.cloud.ai.service.impl.ModelObservationServiceImpl;
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
    public ObservationRegistry observationRegistry(ModelObservationServiceImpl modelObservationService, ModelObservationDetailServiceImpl modelObservationDetailService) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig().observationHandler(new AlibabaObservationHandler(modelObservationService, modelObservationDetailService));
        return observationRegistry;
    }

}
