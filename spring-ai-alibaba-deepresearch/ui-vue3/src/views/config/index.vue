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
  <div class="__container_config_index">
    <div style="padding: 20px 10vw">
      <div class="header">
        <a-flex justify="space-between">
          <h1>Deep Research Config</h1>
          <!-- <a-button @click="back" type="text"><CloseOutlined /></a-button> -->
        </a-flex>
      </div>
      <a-divider />

      <a-form :model="form" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <Title>一般配置</Title>
        <a-form-item label="是否自动接受计划">
          <a-switch v-model:checked="form.auto_accepted_plan"></a-switch>
        </a-form-item>
        <a-form-item label="扩展优化后查询数量">
          <a-input-number v-model:value="form.optimize_query_num"></a-input-number>
        </a-form-item>
        <a-form-item label="计划最大步骤数量">
          <a-input-number v-model:value="form.max_plan_iterations"></a-input-number>
        </a-form-item>
        <a-form-item label="最大迭代次数">
          <a-input-number v-model:value="form.max_step_num"></a-input-number>
        </a-form-item>
        <Title>搜索配置</Title>
        <a-form-item label="搜索引擎">
          <a-radio-group v-model:value="form.search_engine">
            <a-radio value="tavily">Tavily</a-radio>
            <a-radio value="baidu">Baidu</a-radio>
            <a-radio value="serpapi">Serpapi</a-radio>
            <a-radio value="aliyun">Aliyun</a-radio>
          </a-radio-group>
        </a-form-item>
        <Title>MCP</Title>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import Title from '@/components/toolkit/Title.vue'
import { useConfigStore } from '@/store/ConfigStore'
import { useRouter } from 'vue-router'
import { useRouterStore } from '@/store/RouterStore'

const configStore = useConfigStore()
const { form } = configStore

const routerStore = useRouterStore()
const router = useRouter()

function back() {
  const pop = routerStore.pop()
  if (pop) {
    router.push(pop)
  } else {
    router.back()
  }
}
</script>
<style lang="less" scoped>
.__container_config_index {
  width: 100%;
}
</style>
