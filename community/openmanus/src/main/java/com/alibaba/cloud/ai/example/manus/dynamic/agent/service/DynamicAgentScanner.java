package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.annotation.DynamicAgentDefinition;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.repository.DynamicAgentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.beans.factory.config.BeanDefinition;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;

@Service
public class DynamicAgentScanner {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicAgentScanner.class);
    
    private final DynamicAgentRepository repository;
    private final String basePackage = "com.alibaba.cloud.ai.example.manus";
    private final ApplicationContext applicationContext;

    @Autowired
    public DynamicAgentScanner(DynamicAgentRepository repository, ApplicationContext applicationContext) {
        this.repository = repository;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void scanAndSaveAgents() {
        // 创建扫描器
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        
        // 添加注解过滤器
        scanner.addIncludeFilter(new AnnotationTypeFilter(DynamicAgentDefinition.class));
        
        // 扫描指定包下的所有类
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
        
        for (BeanDefinition beanDefinition : candidates) {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                DynamicAgentDefinition annotation = clazz.getAnnotation(DynamicAgentDefinition.class);
                if (annotation != null) {
                    saveDynamicAgent(annotation, clazz);
                }
            }
            catch (ClassNotFoundException e) {
                log.error("Failed to load class: " + beanDefinition.getBeanClassName(), e);
            }
        }
    }

    private void saveDynamicAgent(DynamicAgentDefinition annotation, Class<?> clazz) {
        DynamicAgentEntity entity = repository.findByAgentName(annotation.agentName());
        if (entity == null) {
            entity = new DynamicAgentEntity();
        }

        entity.setAgentName(annotation.agentName());
        entity.setAgentDescription(annotation.agentDescription());
        entity.setSystemPrompt(annotation.systemPrompt());
        entity.setNextStepPrompt(annotation.nextStepPrompt());
        entity.setAvailableToolKeys(Arrays.asList(annotation.availableToolKeys()));
        entity.setClassName(clazz.getName());

        repository.save(entity);
        log.info("已保存动态代理定义: {}", entity.getAgentName());
    }
}
