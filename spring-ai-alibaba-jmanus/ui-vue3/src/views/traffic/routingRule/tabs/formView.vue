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
  <div class="__container_routingRule_detail">
    <a-flex style="width: 100%">
      <a-col :span="isDrawerOpened ? 24 - sliderSpan : 24" class="left">
        <a-row>
          <a-flex justify="space-between" style="width: 100%">
            <a-typography-title :level="3"> 基础信息</a-typography-title>
            <a-button type="text" style="color: #0a90d5" @click="isDrawerOpened = !isDrawerOpened">
              {{ $t('flowControlDomain.versionRecords') }}
              <DoubleLeftOutlined v-if="!isDrawerOpened" />
              <DoubleRightOutlined v-else />
            </a-button>
          </a-flex>
          <a-card class="_detail">
            <a-descriptions :column="2" layout="vertical" title="">
              <!-- ruleName -->
              <a-descriptions-item
                :label="$t('flowControlDomain.ruleName')"
                :labelStyle="{ fontWeight: 'bold' }"
              >
                <p
                  class="description-item-content with-card"
                  @click="copyIt(conditionRuleDetail.key)"
                >
                  {{ conditionRuleDetail.key }}
                  <CopyOutlined />
                </p>
              </a-descriptions-item>

              <!-- ruleGranularity -->
              <a-descriptions-item
                :label="$t('flowControlDomain.ruleGranularity')"
                :labelStyle="{ fontWeight: 'bold' }"
              >
                <a-typography-paragraph>
                  {{ conditionRuleDetail.scope }}
                </a-typography-paragraph>
              </a-descriptions-item>
              <a-descriptions-item
                label="版本"
                :labelStyle="{ fontWeight: 'bold' }"
                v-if="conditionRuleDetail.scope == 'service'"
              >
                <p
                  class="description-item-content with-card"
                  @click="copyIt(conditionRuleDetail.version)"
                >
                  {{ conditionRuleDetail.version }}
                  <CopyOutlined v-if="conditionRuleDetail.version.length" />
                </p>
              </a-descriptions-item>

              <a-descriptions-item
                label="分组"
                :labelStyle="{ fontWeight: 'bold' }"
                v-if="conditionRuleDetail.scope == 'service'"
              >
                <p
                  class="description-item-content with-card"
                  @click="copyIt(conditionRuleDetail.group)"
                >
                  {{ conditionRuleDetail.group }}
                  <CopyOutlined v-if="conditionRuleDetail.group.length" />
                </p>
              </a-descriptions-item>

              <!-- actionObject -->
              <a-descriptions-item
                :label="$t('flowControlDomain.actionObject')"
                :labelStyle="{ fontWeight: 'bold' }"
              >
                <p class="description-item-content with-card" @click="copyIt(actionObj)">
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
              <a-descriptions-item
                :label="$t('flowControlDomain.faultTolerantProtection')"
                :labelStyle="{ fontWeight: 'bold' }"
              >
                <a-typography-paragraph>
                  {{
                    conditionRuleDetail.force
                      ? $t('flowControlDomain.opened')
                      : $t('flowControlDomain.closed')
                  }}
                </a-typography-paragraph>
              </a-descriptions-item>

              <!-- enabledState -->
              <a-descriptions-item
                :label="$t('flowControlDomain.enabledState')"
                :labelStyle="{ fontWeight: 'bold' }"
              >
                <a-typography-paragraph>
                  {{
                    conditionRuleDetail.enabled
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
                    conditionRuleDetail.runtime
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
              <!--              {{ $t('flowControlDomain.notSet') }}-->
              <!--            </a-typography-paragraph>-->
              <!--          </a-descriptions-item>-->
            </a-descriptions>
          </a-card>
        </a-row>

        <a-card style="margin-top: 10px" class="_detail">
          <a-space align="start" style="width: 100%">
            <a-typography-title :level="5"
              >{{ $t('flowControlDomain.requestParameterMatching') }}:
            </a-typography-title>

            <a-space align="center" direction="horizontal" size="middle" wrap>
              <a-tag v-for="(item, index) in requestParameterMatch" :key="index" color="#2db7f5">
                {{ item }}
              </a-tag>
            </a-space>
          </a-space>

          <a-space align="start" style="width: 100%" wrap>
            <a-typography-title :level="5"
              >{{ $t('flowControlDomain.addressSubsetMatching') }}:
            </a-typography-title>
            <a-tag v-for="(item, index) in addressSubsetMatch" :key="index" color="#87d068">
              {{ item }}
            </a-tag>
          </a-space>
        </a-card>
      </a-col>

      <a-col :span="isDrawerOpened ? sliderSpan : 0" class="right">
        <a-card v-if="isDrawerOpened" class="sliderBox">
          <a-card v-for="i in 2" :key="i">
            <p>修改时间: 2024/3/20 15:20:31</p>
            <p>版本号: xo842xqpx834</p>

            <a-flex justify="flex-end">
              <a-button type="text" style="color: #0a90d5">查看</a-button>
              <a-button type="text" style="color: #0a90d5">回滚</a-button>
            </a-flex>
          </a-card>
        </a-card>
      </a-col>
    </a-flex>
  </div>
</template>

<script lang="ts" setup>
import {
  type ComponentInternalInstance,
  computed,
  getCurrentInstance,
  onMounted,
  reactive,
  ref
} from 'vue'
import { CopyOutlined, DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons-vue'
import useClipboard from 'vue-clipboard3'
import { message } from 'ant-design-vue'
import { PRIMARY_COLOR } from '@/base/constants'
import { getConditionRuleDetailAPI } from '@/api/service/traffic'
import { useRoute } from 'vue-router'

const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()
const route = useRoute()

const isDrawerOpened = ref(false)

const sliderSpan = ref(8)

let __ = PRIMARY_COLOR

const toClipboard = useClipboard().toClipboard

function copyIt(v: string) {
  message.success(globalProperties.$t('messageDomain.success.copy'))
  toClipboard(v)
}

// Condition routing details
const conditionRuleDetail = reactive({
  configVersion: 'v3.0',
  scope: 'service',
  key: 'org.apache.dubbo.samples.UserService',
  enabled: true,
  runtime: true,
  force: false,
  conditions: ['=>host!=192.168.0.68'],
  group: '',
  version: ''
})

const actionObj = computed(() => {
  const arr = conditionRuleDetail.key.split(':')
  conditionRuleDetail.version = arr[1] || ''
  conditionRuleDetail.group = arr[2] || ''
  return arr[0] ? arr[0] : ''
})

// Request parameter matching
const requestParameterMatch = ref<string[]>([])

// Address subset matching
const addressSubsetMatch = ref<string[]>([])

// Get condition routing details
async function getRoutingRuleDetail() {
  let res = await getConditionRuleDetailAPI(<string>route.params?.ruleName)
  console.log(res)
  if (res?.code === 200) {
    Object.assign(conditionRuleDetail, res?.data || {})

    conditionRuleDetail.conditions.forEach((item: any, index: number) => {
      const arr = item.split(' => ')
      const addressArr = arr[1]?.split(' & ')
      const requestMatchArr = arr[0]?.split(' & ')
      requestParameterMatch.value = requestParameterMatch.value.concat(requestMatchArr)
      addressSubsetMatch.value = addressSubsetMatch.value.concat(addressArr)
    })
  }
}

const getVersionAndGroup = () => {
  const conditionName = route.params?.ruleName
  if (conditionName && conditionRuleDetail.scope === 'service') {
    const arr = conditionName?.split(':')
    conditionRuleDetail.version = arr[1]
    conditionRuleDetail.group = arr[2].split('.')[0]
  }
}

onMounted(async () => {
  await getRoutingRuleDetail()
  getVersionAndGroup()
})
</script>

<style lang="less" scoped>
.__container_routingRule_detail {
  .sliderBox {
    margin-left: 5px;
    max-height: 530px;
    overflow: auto;
  }

  &:deep(.left.ant-col) {
    transition: all 0.5s ease;
  }

  &:deep(.right.ant-col) {
    transition: all 0.5s ease;
  }
}
</style>
