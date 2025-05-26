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
  <a-card>
    <a-row>
      <a-descriptions title="基础信息" />
      <a-col :span="12">
        <a-descriptions title="" layout="vertical" :column="1">
          <a-descriptions-item label="规则名" :labelStyle="{ fontWeight: 'bold', width: '100px' }">
            <p
              @click="copyIt('shop-user/StandardRouter')"
              class="description-item-content with-card"
            >
              shop-user/StandardRouter
              <CopyOutlined />
            </p>
          </a-descriptions-item>
          <a-descriptions-item
            label="作用对象"
            :labelStyle="{ fontWeight: 'bold', width: '100px' }"
          >
            shop-user
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
      <a-col :span="12">
        <a-descriptions title="" layout="vertical" :column="1">
          <a-descriptions-item
            label="创建时间"
            :labelStyle="{ fontWeight: 'bold', width: '100px' }"
          >
            2024/2/19/ 11:38:21
          </a-descriptions-item>
          <a-descriptions-item
            label="最近修改时间"
            :labelStyle="{ fontWeight: 'bold', width: '100px' }"
          >
            2024/3/20 15:20:31
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
    </a-row>

    <a-row>
      <a-descriptions title="流量策略" />
      <a-col :span="12">
        <a-descriptions title="" layout="vertical" :column="1">
          <a-descriptions-item
            label="负载均衡"
            :labelStyle="{ fontWeight: 'bold', width: '100px' }"
          >
            不指定
          </a-descriptions-item>
        </a-descriptions>
      </a-col>
    </a-row>
  </a-card>

  <a-card>
    <a-descriptions title="地址子集" />

    <a-card title="地址子集【1】">
      <a-space size="middle" direction="vertical">
        <a-descriptions :column="1">
          <a-descriptions-item
            label="子集名称"
            :labelStyle="{ fontWeight: 'bold', width: '100px' }"
          >
            v1-beijing
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions :column="1">
          <a-descriptions-item
            label="标签匹配"
            :labelStyle="{ fontWeight: 'bold', width: '100px' }"
          >
            <a-space>
              <a-tag color="#2db7f5">version=v1</a-tag>
              <a-tag color="#2db7f5">region=beijing</a-tag>
            </a-space>
          </a-descriptions-item>
        </a-descriptions>
      </a-space>
    </a-card>

    <a-card title="地址子集【2】">
      <a-space size="middle" direction="vertical">
        <a-descriptions :column="1">
          <a-descriptions-item
            label="子集名称"
            :labelStyle="{ fontWeight: 'bold', width: '100px' }"
          >
            v1-beijing
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions :column="1">
          <a-descriptions-item
            label="标签匹配"
            :labelStyle="{ fontWeight: 'bold', width: '100px' }"
          >
            <a-space>
              <a-tag color="#2db7f5">version=v1</a-tag>
              <a-tag color="#2db7f5">region=beijing</a-tag>
            </a-space>
          </a-descriptions-item>
        </a-descriptions>
      </a-space>
    </a-card>
  </a-card>
</template>

<script setup lang="ts">
defineOptions({
  name: 'FormView'
})
import { CopyOutlined } from '@ant-design/icons-vue'
import { type ComponentInternalInstance, getCurrentInstance } from 'vue'
import { PRIMARY_COLOR } from '@/base/constants'
import useClipboard from 'vue-clipboard3'
import { message } from 'ant-design-vue'

const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()

const toClipboard = useClipboard().toClipboard

function copyIt(v: string) {
  message.success(globalProperties.$t('messageDomain.success.copy'))
  toClipboard(v)
}
</script>

<style scoped lang="less">
.description-item-content {
  &.no-card {
    padding-left: 20px;
  }
  &.with-card:hover {
    color: v-bind('PRIMARY_COLOR');
  }
}
</style>
