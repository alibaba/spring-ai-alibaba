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
  <div class="__container_app_detail">
    <a-flex>
      <a-card class="_detail">
        <a-descriptions :column="2" layout="vertical" title="">
          <!-- ruleName -->
          <a-descriptions-item
            :label="$t('flowControlDomain.ruleName')"
            :labelStyle="{ fontWeight: 'bold' }"
          >
            <p @click="copyIt(tagRuleDetail.key)" class="description-item-content with-card">
              {{ tagRuleDetail.key }}
              <CopyOutlined />
            </p>
          </a-descriptions-item>

          <!-- ruleGranularity -->
          <a-descriptions-item
            :label="$t('flowControlDomain.ruleGranularity')"
            :labelStyle="{ fontWeight: 'bold' }"
          >
            <a-typography-paragraph>
              {{ tagRuleDetail.scope }}
            </a-typography-paragraph>
          </a-descriptions-item>

          <!-- actionObject -->
          <a-descriptions-item
            :label="$t('flowControlDomain.actionObject')"
            :labelStyle="{ fontWeight: 'bold' }"
          >
            <p @click="copyIt(actionObj)" class="description-item-content with-card">
              {{ actionObj }}
              <CopyOutlined />
            </p>
          </a-descriptions-item>

          <!-- effectTime -->
          <!--          <a-descriptions-item-->
          <!--            :label="$t('flowControlDomain.effectTime')"-->
          <!--            :labelStyle="{ fontWeight: 'bold' }"-->
          <!--          >-->
          <!--            <a-typography-paragraph> 20230/12/19 22:09:34</a-typography-paragraph>-->
          <!--          </a-descriptions-item>-->

          <!-- faultTolerantProtection -->
          <!--          <a-descriptions-item-->
          <!--            :label="$t('flowControlDomain.faultTolerantProtection')"-->
          <!--            :labelStyle="{ fontWeight: 'bold' }"-->
          <!--          >-->
          <!--            <a-typography-paragraph>-->
          <!--              {{ $t('flowControlDomain.opened') }}-->
          <!--            </a-typography-paragraph>-->
          <!--          </a-descriptions-item>-->

          <!-- enabledState -->
          <a-descriptions-item
            :label="$t('flowControlDomain.enabledState')"
            :labelStyle="{ fontWeight: 'bold' }"
          >
            <a-typography-paragraph>
              {{
                tagRuleDetail.enabled
                  ? $t('flowControlDomain.enabled')
                  : $t('flowControlDomain.disabled')
              }}
            </a-typography-paragraph>
          </a-descriptions-item>

          <!-- runTimeEffective -->
          <a-descriptions-item
            :label="$t('flowControlDomain.runTimeEffective')"
            :labelStyle="{ fontWeight: 'bold' }"
          >
            <a-typography-paragraph>
              {{
                tagRuleDetail.runtime
                  ? $t('flowControlDomain.opened')
                  : $t('flowControlDomain.closed')
              }}
            </a-typography-paragraph>
          </a-descriptions-item>

          <!-- priority -->
          <!--          <a-descriptions-item-->
          <!--            :label="$t('flowControlDomain.priority')"-->
          <!--            :labelStyle="{ fontWeight: 'bold' }"-->
          <!--          >-->
          <!--            <a-typography-paragraph>-->
          <!--              {{ '未设置' }}-->
          <!--            </a-typography-paragraph>-->
          <!--          </a-descriptions-item>-->
        </a-descriptions>
      </a-card>
    </a-flex>

    <a-card
      v-for="(item, index) in tagRuleDetail.tags"
      :title="`标签【${index + 1}】`"
      style="margin-top: 10px"
      class="_detail"
    >
      <a-space align="center">
        <a-typography-title :level="5">
          {{ $t('flowControlDomain.labelName') }}:
          <a-typography-text class="labelName">
            {{ item.name }}
          </a-typography-text>
        </a-typography-title>
      </a-space>
      <a-space align="start" style="width: 100%">
        <a-typography-title :level="5"
          >{{ $t('flowControlDomain.actuatingRange') }}:
        </a-typography-title>
        <a-tag v-for="(scope, index) in item.match" :key="index" color="#2db7f5">
          {{ scope.key }}: {{ Object.keys(scope.value)[0] }}={{ Object.values(scope.value)[0] }}
        </a-tag>
      </a-space>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { CopyOutlined } from '@ant-design/icons-vue'
import useClipboard from 'vue-clipboard3'
import { message } from 'ant-design-vue'
import {
  type ComponentInternalInstance,
  computed,
  getCurrentInstance,
  onMounted,
  reactive
} from 'vue'
import { PRIMARY_COLOR } from '@/base/constants'
import { getTagRuleDetailAPI } from '@/api/service/traffic'
import { useRoute } from 'vue-router'

const route = useRoute()
const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()

let __ = PRIMARY_COLOR
const toClipboard = useClipboard().toClipboard

function copyIt(v: string) {
  message.success(globalProperties.$t('messageDomain.success.copy'))
  toClipboard(v)
}

const actionObj = computed(() => {
  const arr = tagRuleDetail.key.split(':')
  return arr[0] ? arr[0] : ''
})

// Route details
const tagRuleDetail = reactive({
  configVersion: 'v3.0',
  scope: 'application',
  key: 'shop-user',
  enabled: true,
  runtime: true,
  tags: [
    {
      name: 'gray',
      match: [
        {
          key: 'version',
          value: {
            exact: 'v1'
          }
        }
      ]
    }
  ]
})

// Get label routing details
const getTagRuleDetail = async () => {
  const res = await getTagRuleDetailAPI(<string>route.params?.ruleName)
  if (res.code === 200) {
    Object.assign(tagRuleDetail, res.data || {})
  }
}

onMounted(() => {
  getTagRuleDetail()
})
</script>

<style lang="less" scoped>
.description-item-content {
  &.no-card {
    padding-left: 20px;
  }

  &.with-card:hover {
    color: v-bind('PRIMARY_COLOR');
  }
}
</style>
