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
  <div class="__container_services_tabs_debug">
    <div class="tabs-title">方法列表</div>
    <a-tabs v-model:activeKey="activeKey" tab-position="left" :tabBarStyle="{ width: '200px' }">
      <a-tab-pane v-for="tabName in methodTabs" :key="tabName" :tab="`${tabName}`">
        <a-descriptions :column="2" layout="vertical">
          <a-descriptions-item label="接口">
            <p class="description-item-content">
              org.apache.dubbo.samples.UserService:getPermissions
            </p>
          </a-descriptions-item>
          <a-descriptions-item label="指定生产者">
            <a-select
              v-model:value="versionAndGroup"
              size="large"
              :options="versionAndGroupOptions"
              class="description-item-content"
            ></a-select>
          </a-descriptions-item>
          <a-descriptions-item label="入参类型">
            <a-tree block-node :tree-data="enterParamType" class="description-item-content" />
          </a-descriptions-item>
          <a-descriptions-item label="出参类型">
            <a-tree block-node :tree-data="outputParamType" class="description-item-content" />
          </a-descriptions-item>
          <a-descriptions-item label="请求">
            <monaco-editor editorId="requestEditor" width="90%" height="300px" />
          </a-descriptions-item>
          <a-descriptions-item label="响应">
            <monaco-editor editorId="responseEditor" width="90%" height="300px" />
          </a-descriptions-item>
        </a-descriptions>
        <a-button type="primary">发送请求</a-button>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import MonacoEditor from '@/components/editor/MonacoEditor.vue'

const methodTabs = reactive([
  'login',
  'register',
  'logout',
  'query',
  'suspend',
  'updateInfo',
  'getPermissions',
  'auth',
  'comment'
])

const activeKey = ref(methodTabs[0])

const versionAndGroupOptions = reactive([
  {
    value: 'version=1.0.0'
  },
  {
    value: 'group=group1'
  },
  {
    value: 'version=1.0.0,group=group1'
  }
])

const versionAndGroup = ref(versionAndGroupOptions[0].value)

const enterParamType = [
  {
    title: 'param0: java.lang.String',
    key: '0'
  },
  {
    title: 'param1: org.apache.dubbo.samples.api.User',
    key: '1',
    children: [
      {
        title: 'name: java.lang.String',
        key: '1-0'
      },
      {
        title: 'id: java.lang.Long',
        key: '1-1'
      },
      {
        title: 'createTime: java.Date.DateTime',
        key: '1-2'
      }
    ]
  }
]

const outputParamType = [
  {
    title: 'param0: org.apache.dubbo.samples.api.Response',
    key: '0',
    children: [
      {
        title: 'success: boolean',
        key: '0-0'
      },
      {
        title: 'phone: java.lang.String',
        key: '0-1'
      },
      {
        title: 'lastLoginTime: java.Date.LocalDateTime',
        key: '0-2'
      },
      {
        title: 'follows: java.util.List',
        key: '0-3'
      }
    ]
  }
]
</script>

<style lang="less" scoped>
.__container_services_tabs_debug {
  width: 100%;
  .tabs-title {
    width: 200px;
    text-align: center;
  }
  .description-item-content {
    margin-left: 20px;
    width: 80%;
  }
}
</style>
