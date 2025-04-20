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

package com.alibaba.cloud.ai.toolcalling.time;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GetCurrentTimeByTimeZoneIdServiceTest {

    @Test
    void testApply() {
        GetCurrentTimeByTimeZoneIdService service = new GetCurrentTimeByTimeZoneIdService();
        String result = service.apply(new GetCurrentTimeByTimeZoneIdService.Request("Asia/Shanghai")).description();
        assertNotNull(result);
        assertTrue(result.contains("Asia/Shanghai"));
        assertTrue(result.matches(".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} .+"));
    }
}