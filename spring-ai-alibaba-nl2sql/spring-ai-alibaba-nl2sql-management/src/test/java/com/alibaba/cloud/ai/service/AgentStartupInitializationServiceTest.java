/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentStartupInitializationService 测试类
 */
@ExtendWith(MockitoExtension.class)
class AgentStartupInitializationServiceTest {

	@Mock
	private AgentService agentService;

	@Mock
	private ApplicationArguments applicationArguments;

	@InjectMocks
	private AgentStartupInitializationService agentStartupInitializationService;

	@Test
    void testRunWithNoPublishedAgents() throws Exception {
        // 配置没有已发布的智能体
        when(agentService.findByStatus("published")).thenReturn(new ArrayList<>());

        // 执行
        agentStartupInitializationService.run(applicationArguments);

        // 等待异步任务完成
        Thread.sleep(500);

        // 验证调用了查询方法
        verify(agentService).findByStatus("published");
    }

}
