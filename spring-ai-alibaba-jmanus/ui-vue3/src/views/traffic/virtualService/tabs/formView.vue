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
            <p @click="copyIt('shop-user')" class="description-item-content with-card">
              shop-user
              <CopyOutlined />
            </p>
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
        </a-descriptions>
      </a-col>
    </a-row>
  </a-card>

  <a-card>
    <a-descriptions title="路由列表" />

    <a-card title="路由【1】">
      <a-space size="middle" direction="vertical">
        <span>名称：未指定</span>
        <a-space size="small">
          <span>服务生效范围:</span>
          <a-tooltip>
            <template #title>服务精确匹配org.apache.dubbo.samples.UserService</template>
            <a-space>
              <a-space-compact direction="horizontal">
                <a-button type="primary">service</a-button>
                <a-button type="primary">exact</a-button>
                <a-button type="primary">org.apache.dubbo.samples.UserService</a-button>
              </a-space-compact>
            </a-space>
          </a-tooltip>
        </a-space>
        <a-space size="middle">
          <span>路由:</span>
          <a-card style="min-width: 400px">
            <a-space direction="vertical" size="middle">
              <a-space size="middle">
                <span>名称</span>
                <span>未指定</span>
              </a-space>

              <a-space size="middle" align="start">
                <span>匹配条件</span>
                <a-space size="middle" direction="vertical">
                  <a-space size="middle" v-for="i in 3">
                    {{ i + '.' }}
                    <a-tooltip>
                      <template #title>方法前缀匹配get</template>
                      <a-space>
                        <a-space-compact direction="horizontal">
                          <a-button type="primary">method</a-button>
                          <a-button type="primary">prefix</a-button>
                          <a-button type="primary">get</a-button>
                        </a-space-compact>
                      </a-space>
                    </a-tooltip>
                    <a-tooltip>
                      <template #title>参数范围匹配1-100</template>
                      <a-space>
                        <a-space-compact direction="horizontal">
                          <a-button type="primary">arg[1]</a-button>
                          <a-button type="primary">range</a-button>
                          <a-button type="primary"> 1-100</a-button>
                        </a-space-compact>
                      </a-space>
                    </a-tooltip>
                  </a-space>
                </a-space>
              </a-space>
              <a-space size="middle">
                <span>路由分发</span>
                <a-tooltip>
                  <template #title>subset=-v1的地址子集赋予权重70</template>
                  <a-space>
                    <a-space>
                      <a-space-compact direction="horizontal">
                        <a-button type="primary">subset=v1</a-button>
                        <a-button type="primary">weight=70</a-button>
                      </a-space-compact>
                    </a-space>
                  </a-space>
                </a-tooltip>
                <a-tooltip>
                  <template #title>subset=-v2的地址子集赋予权重30</template>
                  <a-space>
                    <a-space-compact direction="horizontal">
                      <a-button type="primary">subset=v2</a-button>
                      <a-button type="primary">weight=30</a-button>
                    </a-space-compact>
                  </a-space>
                </a-tooltip>
              </a-space>
            </a-space>
          </a-card>
        </a-space>
      </a-space>
    </a-card>
  </a-card>
</template>

<script setup lang="ts">
import { CopyOutlined } from '@ant-design/icons-vue'
import useClipboard from 'vue-clipboard3'
import { message } from 'ant-design-vue'
import { type ComponentInternalInstance, getCurrentInstance } from 'vue'
import { PRIMARY_COLOR, PRIMARY_COLOR_T } from '@/base/constants'

const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()

let __ = PRIMARY_COLOR
let PRIMARY_COLOR_20 = PRIMARY_COLOR_T('20')

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
