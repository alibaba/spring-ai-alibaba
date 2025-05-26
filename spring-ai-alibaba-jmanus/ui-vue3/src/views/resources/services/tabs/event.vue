<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<template>
  <div class="__container_services_tabs_event">
    <a-card class="timeline-container">
      <a-timeline class="timeline">
        <a-timeline-item>
          <template #dot><MinusCircleOutlined style="font-size: 18px" /></template>
        </a-timeline-item>
        <a-timeline-item v-for="(item, index) in eventData" :key="index">
          <a-tag class="time" :color="PRIMARY_COLOR">{{ item.time }}</a-tag>
          <span class="description">{{ item.description }}</span>
        </a-timeline-item>
        <a-timeline-item>
          <template #dot></template>
          <span>过期事件不会存储</span>
        </a-timeline-item>
      </a-timeline>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { PRIMARY_COLOR } from '@/base/constants'
import { MinusCircleOutlined } from '@ant-design/icons-vue'

let __null = PRIMARY_COLOR
const eventData = [
  {
    time: '2022-01-01',
    description: 'description'
  },
  {
    time: '2022-01-02',
    description: 'description'
  },
  {
    time: '2022-01-03',
    description: 'description'
  },
  {
    time: '2022-01-04',
    description: 'description'
  },
  {
    time: '2022-01-05',
    description: 'description'
  }
]
</script>

<style lang="less" scoped>
.__container_services_tabs_event {
  display: flex;
  .timeline-container {
    width: 100%;
    .timeline {
      margin-left: 30px;
      .description {
        font-size: 18px;
      }
    }
  }
}
</style>
