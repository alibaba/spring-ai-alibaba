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
        <a-card>
          <a-space style="width: 100%" direction="vertical" size="middle">
            <a-row>
              <a-flex justify="end" style="width: 100%">
                <a-button
                  type="text"
                  style="color: #0a90d5"
                  @click="isDrawerOpened = !isDrawerOpened"
                >
                  字段说明
                  <DoubleLeftOutlined v-if="!isDrawerOpened" />
                  <DoubleRightOutlined v-else />
                </a-button>
              </a-flex>
              <a-card title="基础信息" style="width: 100%" class="_detail">
                <a-form layout="horizontal">
                  <a-row style="width: 100%">
                    <a-col :span="12">
                      <a-form-item label="规则粒度" required>
                        <a-select
                          v-model:value="baseInfo.ruleGranularity"
                          style="width: 120px"
                          :options="ruleGranularityOptions"
                        ></a-select>
                      </a-form-item>
                      <a-form-item
                        v-if="baseInfo.ruleGranularity === 'service'"
                        label="版本"
                        required
                      >
                        <a-input v-model:value="baseInfo.version" style="width: 300px" />
                      </a-form-item>
                      <a-form-item label="容错保护">
                        <a-switch
                          v-model:checked="baseInfo.faultTolerantProtection"
                          checked-children="开"
                          un-checked-children="关"
                        />
                      </a-form-item>
                      <a-form-item label="运行时生效">
                        <a-switch
                          v-model:checked="baseInfo.runtime"
                          checked-children="开"
                          un-checked-children="关"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="作用对象" required>
                        <a-input v-model:value="baseInfo.objectOfAction" style="width: 300px" />
                      </a-form-item>
                      <a-form-item
                        v-if="baseInfo.ruleGranularity === 'service'"
                        label="分组"
                        required
                      >
                        <a-input v-model:value="baseInfo.group" style="width: 300px" />
                      </a-form-item>
                      <a-form-item label="立即启用">
                        <a-switch
                          v-model:checked="baseInfo.enable"
                          checked-children="开"
                          un-checked-children="关"
                        />
                      </a-form-item>
                      <a-form-item label="优先级">
                        <a-input-number v-model:value="baseInfo.priority" min="1" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>
            </a-row>

            <a-card title="路由列表" style="width: 100%" class="_detail">
              <a-card v-for="(routeItem, routeItemIndex) in routeList">
                <template #title>
                  <a-flex justify="space-between">
                    <a-space align="center">
                      <div>路由【{{ routeItemIndex + 1 }}】</div>
                      <a-tooltip>
                        <template #title>{{ routeItemDes(routeItemIndex) }}</template>
                        <div
                          style="
                            max-width: 400px;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            white-space: nowrap;
                          "
                        >
                          {{ routeItemDes(routeItemIndex) }}
                        </div>
                      </a-tooltip>
                    </a-space>
                    <Icon
                      @click="deleteRoute(routeItemIndex)"
                      class="action-icon"
                      icon="tdesign:delete"
                    />
                  </a-flex>
                </template>

                <a-form layout="horizontal">
                  <a-space style="width: 100%" direction="vertical" size="large">
                    <a-form-item label="请求匹配">
                      <a-card v-if="routeItem.requestMatch.length > 0">
                        <a-space style="width: 100%" direction="vertical" size="small">
                          <a-flex align="center" justify="space-between">
                            <a-form-item label="匹配条件类型">
                              <a-select
                                v-model:value="routeItem.selectedMatchConditionTypes"
                                :options="matchConditionTypeOptions"
                                mode="multiple"
                                style="min-width: 200px"
                              />
                            </a-form-item>
                            <Icon
                              @click="deleteRequestMatch(routeItemIndex)"
                              class="action-icon"
                              icon="tdesign:delete"
                            />
                          </a-flex>
                          <template
                            v-for="(conditionItem, conditionItemIndex) in routeItem.requestMatch"
                          >
                            <!--                        host-->
                            <a-space
                              size="large"
                              align="center"
                              v-if="
                                routeItem.selectedMatchConditionTypes.includes('host') &&
                                conditionItem.type === 'host'
                              "
                            >
                              <a-tag
                                class="match-condition-type-label"
                                :bordered="false"
                                color="processing"
                              >
                                {{ conditionItem?.type }}
                              </a-tag>
                              <a-select
                                v-model:value="conditionItem.condition"
                                style="min-width: 120px"
                                :options="conditionOptions"
                              />
                              <a-input
                                v-model:value="conditionItem.value"
                                placeholder="请求来源ip"
                              />

                              <Icon
                                @click="
                                  deleteMatchConditionTypeItem(conditionItem?.type, routeItemIndex)
                                "
                                class="action-icon"
                                icon="tdesign:delete"
                              />
                            </a-space>
                            <!--application-->
                            <a-space
                              size="large"
                              align="center"
                              v-if="
                                routeItem.selectedMatchConditionTypes.includes('application') &&
                                conditionItem.type === 'application'
                              "
                            >
                              <a-tag
                                class="match-condition-type-label"
                                :bordered="false"
                                color="processing"
                              >
                                {{ conditionItem?.type }}
                              </a-tag>
                              <a-select
                                v-model:value="conditionItem.condition"
                                style="min-width: 120px"
                                :options="conditionOptions"
                              />
                              <a-input
                                v-model:value="conditionItem.value"
                                placeholder="请求来源应用名"
                              />

                              <Icon
                                @click="
                                  deleteMatchConditionTypeItem(conditionItem?.type, routeItemIndex)
                                "
                                class="action-icon"
                                icon="tdesign:delete"
                              />
                            </a-space>
                            <!--                      method-->
                            <a-space
                              size="large"
                              align="center"
                              v-if="
                                routeItem.selectedMatchConditionTypes.includes('method') &&
                                conditionItem.type === 'method'
                              "
                            >
                              <a-tag
                                class="match-condition-type-label"
                                :bordered="false"
                                color="processing"
                              >
                                {{ conditionItem?.type }}
                              </a-tag>
                              <a-select
                                v-model:value="conditionItem.condition"
                                style="min-width: 120px"
                                :options="conditionOptions"
                              />
                              <a-input v-model:value="conditionItem.value" placeholder="方法值" />

                              <Icon
                                @click="
                                  deleteMatchConditionTypeItem(conditionItem?.type, routeItemIndex)
                                "
                                class="action-icon"
                                icon="tdesign:delete"
                              />
                            </a-space>
                            <!--                      arguments-->
                            <a-space
                              style="width: 100%"
                              size="large"
                              align="start"
                              v-if="
                                routeItem.selectedMatchConditionTypes.includes('arguments') &&
                                conditionItem.type === 'arguments'
                              "
                            >
                              <a-tag
                                class="match-condition-type-label"
                                :bordered="false"
                                color="processing"
                              >
                                {{ conditionItem?.type }}
                              </a-tag>
                              <a-space direction="vertical">
                                <a-button
                                  type="primary"
                                  @click="addArgumentsItem(routeItemIndex, conditionItemIndex)"
                                >
                                  添加argument
                                </a-button>
                                <a-table
                                  :pagination="false"
                                  :columns="argumentsColumns"
                                  :data-source="routeItem.requestMatch[conditionItemIndex].list"
                                >
                                  <template
                                    #bodyCell="{ column, record, text, index: argumentIndex }"
                                  >
                                    <template v-if="column.key === 'index'">
                                      <a-input v-model:value="record.index" placeholder="index" />
                                    </template>
                                    <template v-else-if="column.key === 'condition'">
                                      <a-select
                                        v-model:value="record.condition"
                                        :options="conditionOptions"
                                      />
                                    </template>
                                    <template v-else-if="column.key === 'value'">
                                      <a-input v-model:value="record.value" placeholder="value" />
                                    </template>
                                    <template v-else-if="column.key === 'operation'">
                                      <a-space align="center">
                                        <Icon
                                          @click="
                                            deleteArgumentsItem(
                                              routeItemIndex,
                                              conditionItemIndex,
                                              argumentIndex
                                            )
                                          "
                                          icon="tdesign:remove"
                                          class="action-icon"
                                        />
                                        <!--                                      <Icon-->
                                        <!--                                        class="action-icon"-->
                                        <!--                                        @click="-->
                                        <!--                                          addArgumentsItem(routeItemIndex, conditionItemIndex)-->
                                        <!--                                        "-->
                                        <!--                                        icon="tdesign:add"-->
                                        <!--                                      />-->
                                      </a-space>
                                    </template>
                                  </template>
                                </a-table>
                              </a-space>
                            </a-space>
                            <!--                      attachments-->
                            <a-space
                              style="width: 100%"
                              size="large"
                              align="start"
                              v-if="
                                routeItem.selectedMatchConditionTypes.includes('attachments') &&
                                conditionItem.type === 'attachments'
                              "
                            >
                              <a-tag
                                class="match-condition-type-label"
                                :bordered="false"
                                color="processing"
                              >
                                {{ conditionItem?.type }}
                              </a-tag>
                              <a-space direction="vertical">
                                <a-button
                                  type="primary"
                                  @click="addAttachmentsItem(routeItemIndex, conditionItemIndex)"
                                >
                                  添加attachment
                                </a-button>
                                <a-table
                                  :pagination="false"
                                  :columns="attachmentsColumns"
                                  :data-source="routeItem.requestMatch[conditionItemIndex].list"
                                >
                                  <template
                                    #bodyCell="{ column, record, text, index: attachmentsIndex }"
                                  >
                                    <template v-if="column.key === 'myKey'">
                                      <a-input v-model:value="record.myKey" placeholder="key" />
                                    </template>
                                    <template v-else-if="column.key === 'condition'">
                                      <a-select
                                        v-model:value="record.condition"
                                        :options="conditionOptions"
                                      />
                                    </template>
                                    <template v-else-if="column.key === 'value'">
                                      <a-input v-model:value="record.value" placeholder="value" />
                                    </template>
                                    <template v-else-if="column.key === 'operation'">
                                      <a-space align="center">
                                        <Icon
                                          @click="
                                            deleteAttachmentsItem(
                                              routeItemIndex,
                                              conditionItemIndex,
                                              attachmentsIndex
                                            )
                                          "
                                          icon="tdesign:remove"
                                          class="action-icon"
                                        />
                                      </a-space>
                                    </template>
                                  </template>
                                </a-table>
                              </a-space>
                            </a-space>
                            <!--                      other-->
                            <a-space
                              style="width: 100%"
                              size="large"
                              align="start"
                              v-if="
                                routeItem.selectedMatchConditionTypes.includes('other') &&
                                conditionItem.type === 'other'
                              "
                            >
                              <a-tag
                                class="match-condition-type-label"
                                :bordered="false"
                                color="processing"
                              >
                                {{ conditionItem?.type == 'other' ? '其他' : conditionItem?.type }}
                              </a-tag>
                              <a-space direction="vertical">
                                <a-button
                                  type="primary"
                                  @click="addOtherItem(routeItemIndex, conditionItemIndex)"
                                >
                                  添加other
                                </a-button>
                                <a-table
                                  :pagination="false"
                                  :columns="otherColumns"
                                  :data-source="routeItem.requestMatch[conditionItemIndex].list"
                                >
                                  <template #bodyCell="{ column, record, text }">
                                    <template v-if="column.key === 'myKey'">
                                      <a-input v-model:value="record.myKey" placeholder="key" />
                                    </template>
                                    <template v-else-if="column.key === 'condition'">
                                      <a-select
                                        v-model:value="record.condition"
                                        :options="conditionOptions"
                                      />
                                    </template>
                                    <template v-else-if="column.key === 'value'">
                                      <a-input v-model:value="record.value" placeholder="value" />
                                    </template>
                                    <template v-else-if="column.key === 'operation'">
                                      <a-space align="center">
                                        <Icon
                                          @click="
                                            deleteOtherItem(
                                              routeItemIndex,
                                              conditionItemIndex,
                                              record.index
                                            )
                                          "
                                          icon="tdesign:remove"
                                          class="action-icon"
                                        />
                                      </a-space>
                                    </template>
                                  </template>
                                </a-table>
                              </a-space>
                            </a-space>
                          </template>
                        </a-space>
                      </a-card>
                      <a-button
                        @click="addRequestMatch(routeItemIndex)"
                        v-else
                        type="dashed"
                        size="large"
                      >
                        <template #icon>
                          <Icon icon="tdesign:add" />
                        </template>
                        增加匹配条件
                      </a-button>
                    </a-form-item>
                    <a-form-item label="路由分发" required>
                      <a-card>
                        <a-space style="width: 100%" direction="vertical" size="small">
                          <a-flex>
                            <a-form-item label="匹配条件类型">
                              <a-select
                                v-model:value="routeItem.selectedRouteDistributeMatchTypes"
                                :options="routeDistributionTypeOptions"
                                mode="multiple"
                                style="min-width: 200px"
                              />
                            </a-form-item>
                          </a-flex>
                          <template
                            v-for="(conditionItem, conditionItemIndex) in routeItem.routeDistribute"
                            :key="conditionItemIndex"
                          >
                            <!--                        host-->
                            <a-space
                              size="large"
                              align="center"
                              v-if="
                                routeItem.selectedRouteDistributeMatchTypes.includes('host') &&
                                conditionItem.type === 'host'
                              "
                            >
                              <a-tag
                                class="match-condition-type-label"
                                :bordered="false"
                                color="processing"
                              >
                                {{ conditionItem?.type }}
                              </a-tag>
                              <a-select
                                v-model:value="conditionItem.condition"
                                style="min-width: 120px"
                                :options="conditionOptions"
                              />
                              <a-input
                                v-model:value="conditionItem.value"
                                placeholder="请求来源ip"
                              />

                              <Icon
                                @click="
                                  deleteRouteDistributeMatchTypeItem(
                                    conditionItem?.type,
                                    routeItemIndex
                                  )
                                "
                                class="action-icon"
                                icon="tdesign:delete"
                              />
                            </a-space>

                            <!--                      other-->
                            <a-space
                              style="width: 100%"
                              size="large"
                              align="start"
                              v-if="
                                routeItem.selectedRouteDistributeMatchTypes.includes('other') &&
                                conditionItem.type === 'other'
                              "
                            >
                              <a-tag
                                class="match-condition-type-label"
                                :bordered="false"
                                color="processing"
                              >
                                {{ conditionItem?.type == 'other' ? '其他' : conditionItem?.type }}
                              </a-tag>
                              <a-space direction="vertical">
                                <a-button
                                  type="primary"
                                  @click="
                                    addRouteDistributeOtherItem(routeItemIndex, conditionItemIndex)
                                  "
                                >
                                  添加其他
                                </a-button>
                                <a-table
                                  :pagination="false"
                                  :columns="otherColumns"
                                  :data-source="routeItem.routeDistribute[conditionItemIndex].list"
                                >
                                  <template #bodyCell="{ column, record, text, index: otherIndex }">
                                    <template v-if="column.key === 'myKey'">
                                      <a-input v-model:value="record.myKey" placeholder="key" />
                                    </template>
                                    <template v-else-if="column.key === 'condition'">
                                      <a-select
                                        v-model:value="record.condition"
                                        :options="conditionOptions"
                                      />
                                    </template>
                                    <template v-else-if="column.key === 'value'">
                                      <a-input v-model:value="record.value" placeholder="value" />
                                    </template>
                                    <template v-else-if="column.key === 'operation'">
                                      <a-space align="center">
                                        <Icon
                                          @click="
                                            deleteRouteDistributeOtherItem(
                                              routeItemIndex,
                                              conditionItemIndex,
                                              otherIndex
                                            )
                                          "
                                          icon="tdesign:remove"
                                          class="action-icon"
                                        />
                                        <!--                                     <Icon-->
                                        <!--                                       @click="-->
                                        <!--                                          addRouteDistributeOtherItem(-->
                                        <!--                                            routeItemIndex,-->
                                        <!--                                            conditionItemIndex-->
                                        <!--                                          )-->
                                        <!--                                        "-->
                                        <!--                                       icon="tdesign:add"-->
                                        <!--                                       class="action-icon"-->
                                        <!--                                     />-->
                                      </a-space>
                                    </template>
                                  </template>
                                </a-table>
                              </a-space>
                            </a-space>
                          </template>
                        </a-space>
                      </a-card>
                    </a-form-item>
                  </a-space>
                </a-form>
              </a-card>
            </a-card>
            <a-button @click="addRoute" type="primary"> 增加路由</a-button>
          </a-space>
        </a-card>
      </a-col>

      <a-col :span="isDrawerOpened ? sliderSpan : 0" class="right">
        <a-card v-if="isDrawerOpened" class="sliderBox">
          <div>
            <a-descriptions title="字段说明" :column="1">
              <a-descriptions-item label="key">
                作用对象<br />
                可能的值：Dubbo应用名或者服务名
              </a-descriptions-item>
              <a-descriptions-item label="scope">
                规则粒度<br />
                可能的值：application, service
              </a-descriptions-item>
              <a-descriptions-item label="force">
                容错保护<br />
                可能的值：true, false<br />
                描述：如果为true，则路由筛选后若没有可用的地址则会直接报异常；如果为false，则会从可用地址中选择完成RPC调用
              </a-descriptions-item>
              <a-descriptions-item label="runtime">
                运行时生效<br />
                可能的值：true, false<br />
                描述：如果为true，则该rule下的所有路由将会实时生效；若为false，则只有在启动时才会生效
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </a-card>
      </a-col>
    </a-flex>
    <a-card class="footer">
      <a-flex>
        <a-button type="primary" @click="addRoutingRule">确认</a-button>
      </a-flex>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import {
  type ComponentInternalInstance,
  getCurrentInstance,
  reactive,
  ref,
  inject,
  onMounted,
  watch
} from 'vue'
import { DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons-vue'
import useClipboard from 'vue-clipboard3'
import { message } from 'ant-design-vue'
import { PRIMARY_COLOR } from '@/base/constants'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { addConditionRuleAPI } from '@/api/service/traffic'
import { isNil } from 'lodash'
const TAB_STATE = inject(PROVIDE_INJECT_KEY.PROVIDE_INJECT_KEY)
onMounted(() => {
  if (!isNil(TAB_STATE.conditionRule)) {
    const { enabled = true, key, scope, runtime = true, conditions } = TAB_STATE.conditionRule
    // console.log('[ TAB_STATE.conditionRule ] >', TAB_STATE.conditionRule)
    baseInfo.enable = enabled
    baseInfo.objectOfAction = key
    baseInfo.ruleGranularity = scope
    baseInfo.runtime = runtime

    conditions &&
      conditions.length &&
      conditions.forEach((item, index) => {
        const conditionArr = item.split('=>')
        const match = conditionArr[0]?.trim()
        const to = conditionArr[1]?.trim()
        routeList.value[index].requestMatch = parseConditionMatchStringToArray(match, index)
        routeList.value[index].routeDistribute = parseConditionToStringToArray(to, index)
      })
  }

  if (!isNil(TAB_STATE.addConditionRuleSate)) {
    const { version, group } = TAB_STATE.addConditionRuleSate
    baseInfo.version = version
    baseInfo.group = group
  }
})
const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()
const router = useRouter()

const isDrawerOpened = ref(false)

const sliderSpan = ref(8)

let __ = PRIMARY_COLOR

const toClipboard = useClipboard().toClipboard

function copyIt(v: string) {
  message.success(globalProperties.$t('messageDomain.success.copy'))
  toClipboard(v)
}

// base info
const baseInfo = reactive({
  version: '',
  ruleGranularity: '',
  objectOfAction: '',
  enable: true,
  faultTolerantProtection: false,
  runtime: true,
  priority: null,
  group: ''
})

watch(
  baseInfo,
  (newVal) => {
    const { ruleGranularity, enable = true, runtime = true, objectOfAction } = newVal
    TAB_STATE.conditionRule = {
      ...TAB_STATE.conditionRule,
      enabled: enable,
      key: objectOfAction,
      runtime: runtime,
      scope: ruleGranularity
    }
    TAB_STATE.addConditionRuleSate = {
      version: newVal.version,
      group: newVal.group
    }
  },
  {
    immediate: isNil(TAB_STATE.conditionRule) ? true : false
  }
)

const matchConditionTypeOptions = ref([
  {
    label: 'host',
    value: 'host'
  },
  {
    label: 'application',
    value: 'application'
  },
  {
    label: 'method',
    value: 'method'
  },
  {
    label: 'arguments',
    value: 'arguments'
  },
  {
    label: 'attachments',
    value: 'attachments'
  },
  {
    label: '其他',
    value: 'other'
  }
])

const routeDistributionTypeOptions = ref([
  {
    label: 'host',
    value: 'host'
  },
  {
    label: '其他',
    value: 'other'
  }
])

const conditionOptions = ref([
  {
    label: '=',
    value: '='
  },
  {
    label: '!=',
    value: '!='
  }
])

// rule granularity options
const ruleGranularityOptions = ref([
  {
    label: '应用',
    value: 'application'
  },
  {
    label: '服务',
    value: 'service'
  }
])

enum ruleGranularityEnum {
  application = '应用',
  service = '服务'
}

// route list
const routeList = ref([
  {
    selectedMatchConditionTypes: [],
    requestMatch: [],
    selectedRouteDistributeMatchTypes: [],
    routeDistribute: [
      {
        type: 'host',
        condition: '',
        value: ''
      },
      {
        type: 'other',
        list: [
          {
            myKey: '',
            condition: '',
            value: ''
          }
        ]
      }
    ]
  }
])

watch(
  routeList,
  (newVal) => {
    TAB_STATE.conditionRule = {
      ...TAB_STATE.conditionRule,
      conditions: mergeConditions()
    }
  },
  {
    deep: true,
    immediate: isNil(TAB_STATE.conditionRule) ? true : false
  }
)

const addRoute = () => {
  routeList.value.push({
    selectedMatchConditionTypes: [],
    requestMatch: [],
    selectedRouteDistributeMatchTypes: [],
    routeDistribute: [
      {
        type: 'host',
        condition: '=',
        value: '127.0.0.1'
      },
      {
        type: 'other',
        list: [
          {
            myKey: 'key',
            condition: '=',
            value: 'value'
          }
        ]
      }
    ]
  })
}

const deleteRoute = (index: number) => {
  routeList.value.splice(index, 1)
}

const deleteRequestMatch = (index: number) => {
  routeList.value[index].requestMatch = []
  routeList.value[index].selectedMatchConditionTypes = []
}

const addRequestMatch = (index: number) => {
  routeList.value[index].requestMatch = [
    {
      type: 'host',
      condition: '',
      value: ''
    },
    {
      type: 'application',
      condition: '',
      value: ''
    },
    {
      type: 'method',
      condition: '',
      value: ''
    },
    {
      type: 'arguments',
      list: [
        {
          index: 0,
          condition: '',
          value: ''
        }
      ]
    },
    {
      type: 'attachments',
      list: [
        {
          myKey: 'key',
          condition: '',
          value: ''
        }
      ]
    },
    {
      type: 'other',
      list: [
        {
          myKey: 'key',
          condition: '',
          value: ''
        }
      ]
    }
  ]
}

const deleteMatchConditionTypeItem = (type: string, index: number) => {
  // console.log(type, index)
  routeList.value[index].selectedMatchConditionTypes = routeList.value[
    index
  ].selectedMatchConditionTypes.filter((item) => item !== type)
}

const deleteRouteDistributeMatchTypeItem = (type: string, index: number) => {
  routeList.value[index].selectedRouteDistributeMatchTypes = routeList.value[
    index
  ].selectedRouteDistributeMatchTypes.filter((item) => item !== type)
}

const argumentsColumns = [
  {
    dataIndex: 'index',
    key: 'index',
    title: '参数索引'
  },
  {
    dataIndex: 'condition',
    key: 'condition',
    title: '关系'
  },
  {
    dataIndex: 'value',
    key: 'value',
    title: '值'
  },
  {
    dataIndex: 'operation',
    key: 'operation',
    title: '操作'
  }
]

// add argumentsItem
const addArgumentsItem = (routeItemIndex: number, conditionItemIndex: number) => {
  routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.push({
    index: 0,
    condition: '=',
    value: ''
  })
}

// deleteArgumentsItem
const deleteArgumentsItem = (
  routeItemIndex: number,
  conditionItemIndex: number,
  argumentsIndex: number
) => {
  if (routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.length === 1) {
    routeList.value[routeItemIndex].selectedMatchConditionTypes = routeList.value[
      routeItemIndex
    ].selectedMatchConditionTypes.filter((item) => item !== 'arguments')
  }

  routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.splice(argumentsIndex, 1)
}

// attachments
const attachmentsColumns = [
  {
    dataIndex: 'myKey',
    key: 'myKey',
    title: '键'
  },
  {
    dataIndex: 'condition',
    key: 'condition',
    title: '关系'
  },
  {
    dataIndex: 'value',
    key: 'value',
    title: '值'
  },
  {
    dataIndex: 'operation',
    key: 'operation',
    title: '操作'
  }
]

const addAttachmentsItem = (routeItemIndex: number, conditionItemIndex: number) => {
  routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.push({
    key: 'key',
    condition: '=',
    value: ''
  })
}

const deleteAttachmentsItem = (
  routeItemIndex: number,
  conditionItemIndex: number,
  attachmentsItemIndex: number
) => {
  if (routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.length === 1) {
    routeList.value[routeItemIndex].selectedMatchConditionTypes = routeList.value[
      routeItemIndex
    ].selectedMatchConditionTypes.filter((item) => item !== 'attachments')
  }
  routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.splice(
    attachmentsItemIndex,
    1
  )
}

// other
const otherColumns = [
  {
    dataIndex: 'myKey',
    key: 'myKey',
    title: '键'
  },
  {
    dataIndex: 'condition',
    key: 'condition',
    title: '关系'
  },
  {
    dataIndex: 'value',
    key: 'value',
    title: '值'
  },
  {
    dataIndex: 'operation',
    key: 'operation',
    title: '操作'
  }
]

const addOtherItem = (routeItemIndex: number, conditionItemIndex: number) => {
  routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.push({
    myKey: '',
    condition: '=',
    value: ''
  })
}

const deleteOtherItem = (
  routeItemIndex: number,
  conditionItemIndex: number,
  otherItemIndex: number
) => {
  if (routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.length === 1) {
    routeList.value[routeItemIndex].selectedMatchConditionTypes = routeList.value[
      routeItemIndex
    ].selectedMatchConditionTypes.filter((item) => item !== 'other')
    return
  }
  routeList.value[routeItemIndex].requestMatch[conditionItemIndex].list.splice(otherItemIndex, 1)
}

const addRouteDistributeOtherItem = (routeItemIndex: number, conditionItemIndex: number) => {
  routeList.value[routeItemIndex].routeDistribute[conditionItemIndex].list.push({
    myKey: '',
    condition: '=',
    value: ''
  })
}

const deleteRouteDistributeOtherItem = (
  routeItemIndex: number,
  conditionItemIndex: number,
  otherItemIndex: number
) => {
  if (routeList.value[routeItemIndex].routeDistribute[conditionItemIndex].list.length === 1) {
    routeList.value[routeItemIndex].selectedRouteDistributeMatchTypes = routeList.value[
      routeItemIndex
    ].selectedRouteDistributeMatchTypes.filter((item) => item !== 'other')
    return
  }

  routeList.value[routeItemIndex].routeDistribute[conditionItemIndex].list.splice(otherItemIndex, 1)
}

function routeItemDes(routeIndex: number): string {
  const routeItem = routeList.value[routeIndex]
  const { ruleGranularity, objectOfAction } = baseInfo

  const typeText = ruleGranularity === 'service' ? '服务' : '应用'
  let baseDescription = `对于${typeText}【${objectOfAction || '未指定'}】`

  // 构建匹配条件描述 (when)
  let whenConditions: string[] = []
  routeItem.selectedMatchConditionTypes?.forEach((type) => {
    const matchItem = routeItem.requestMatch?.find((item) => item.type === type)
    if (!matchItem) return

    let conditionStr = ''
    const conditionSymbol =
      matchItem.condition === '='
        ? '等于'
        : matchItem.condition === '!='
          ? '不等于'
          : matchItem.condition || ''
    const valueStr = matchItem.value || '未指定'

    switch (type) {
      case 'host':
        conditionStr = `请求来源主机 ${conditionSymbol} ${valueStr}`
        break
      case 'application':
        conditionStr = `请求来源应用 ${conditionSymbol} ${valueStr}`
        break
      case 'method':
        conditionStr = `请求方法 ${conditionSymbol} ${valueStr}`
        break
      case 'arguments':
        const argConditions = matchItem.list
          ?.map((arg) => {
            const argConditionSymbol =
              arg.condition === '='
                ? '等于'
                : arg.condition === '!='
                  ? '不等于'
                  : arg.condition || ''
            const argValueStr = arg.value !== undefined && arg.value !== '' ? arg.value : '未指定'
            return `参数[${arg.index}] ${argConditionSymbol} ${argValueStr}`
          })
          .filter(Boolean)
        if (argConditions?.length > 0) conditionStr = argConditions.join(' 且 ')
        break
      case 'attachments':
        const attachConditions = matchItem.list
          ?.map((attach) => {
            const attachConditionSymbol =
              attach.condition === '='
                ? '等于'
                : attach.condition === '!='
                  ? '不等于'
                  : attach.condition || ''
            const attachValueStr =
              attach.value !== undefined && attach.value !== '' ? attach.value : '未指定'
            return `附件[${attach.myKey || '未指定'}] ${attachConditionSymbol} ${attachValueStr}`
          })
          .filter(Boolean)
        if (attachConditions?.length > 0) conditionStr = attachConditions.join(' 且 ')
        break
      case 'other':
        const otherConditions = matchItem.list
          ?.map((other) => {
            const otherConditionSymbol =
              other.condition === '='
                ? '等于'
                : other.condition === '!='
                  ? '不等于'
                  : other.condition || ''
            const otherValueStr =
              other.value !== undefined && other.value !== '' ? other.value : '未指定'
            return `自定义匹配[${other.myKey || '未指定'}] ${otherConditionSymbol} ${otherValueStr}`
          })
          .filter(Boolean)
        if (otherConditions?.length > 0) conditionStr = otherConditions.join(' 且 ')
        break
    }
    if (conditionStr) {
      // Check for empty mandatory fields
      if ((type === 'host' || type === 'application' || type === 'method') && !matchItem.value) {
        whenConditions.push(
          `${type === 'host' ? '请求来源主机' : type === 'application' ? '请求来源应用' : '请求方法'} 未填写`
        )
      } else {
        whenConditions.push(conditionStr)
      }
    }
  })

  const whenConditionStr = whenConditions.length > 0 ? whenConditions.join(' 且 ') : '任意请求'

  // 构建转发条件描述 (then)
  let thenConditions: string[] = []
  routeItem.selectedRouteDistributeMatchTypes?.forEach((type) => {
    const distributeItem = routeItem.routeDistribute?.find((item) => item.type === type)
    if (!distributeItem) return

    let conditionStr = ''
    const conditionSymbol =
      distributeItem.condition === '='
        ? '等于'
        : distributeItem.condition === '!='
          ? '不等于'
          : distributeItem.condition || ''
    const valueStr = distributeItem.value || '未指定'

    switch (type) {
      case 'host':
        conditionStr = `目标主机 ${conditionSymbol} ${valueStr}`
        break
      case 'other':
        const otherConditions = distributeItem.list
          ?.map((other) => {
            const otherConditionSymbol =
              other.condition === '='
                ? '等于'
                : other.condition === '!='
                  ? '不等于'
                  : other.condition || ''
            const otherValueStr =
              other.value !== undefined && other.value !== '' ? other.value : '未指定'
            return `目标标签[${other.myKey || '未指定'}] ${otherConditionSymbol} ${otherValueStr}`
          })
          .filter(Boolean)
        if (otherConditions?.length > 0) conditionStr = otherConditions.join(' 且 ')
        break
    }
    if (conditionStr) {
      if (type === 'host' && !distributeItem.value) {
        thenConditions.push(`目标主机 未填写`)
      } else {
        thenConditions.push(conditionStr)
      }
    }
  })

  const thenConditionStr =
    thenConditions.length > 0 ? `满足 【${thenConditions.join(' 且 ')}】` : '默认路由规则'

  return `${baseDescription}，将满足 【${whenConditionStr}】 条件的请求，转发到 ${thenConditionStr} 的实例。`
}

// Test case
// const str = 'host=example.com & application=myApp & method=getItem & arguments[1]!=dubbo & arguments[2]=dubbo2 & attachments[myKey]=myValue & other[myKey2]=myValue2';
// const test = parseConditionsStringToArray(str, 0);
// routeList.value[0].requestMatch = test
// console.log('test', test)

function mergeConditions() {
  let conditions: string[] = []
  let matchStr = ''
  let toStr = ''
  routeList.value.forEach((routeItem, routeItemIndex) => {
    // mergeMatch
    // console.log('[ routeItem.selectedMatchConditionTypes ] >', routeItem.selectedMatchConditionTypes)
    routeItem.selectedMatchConditionTypes.forEach((type, typeIndex) => {
      routeItem.requestMatch.forEach((matchItem, matchItemIndex) => {
        if (type == matchItem?.type) {
          // matchStr.length > 0 && (matchStr += ' & ')
          switch (matchItem?.type) {
            case 'arguments':
              {
                matchItem.list.forEach((item, index) => {
                  matchStr.length > 0 && (matchStr += ' & ')
                  matchStr += `${type}[${item.index}]${item.condition}${item.value}`
                })
              }
              break
            case 'attachments':
              {
                matchItem.list.forEach((item, index) => {
                  matchStr.length > 0 && (matchStr += ' & ')
                  matchStr += `${type}[${item.myKey}]${item.condition}${item.value}`
                })
              }
              break
            case 'other':
              {
                matchItem.list.forEach((item, index) => {
                  matchStr.length > 0 && (matchStr += ' & ')
                  matchStr += `${item.myKey}${item.condition}${item.value}`
                })
              }
              break
            default:
              matchStr.length > 0 && (matchStr += ' & ')
              matchStr += `${matchItem.type}${matchItem.condition}${matchItem.value}`
          }
        }
      })
    })

    //   mergeDistribute
    routeItem.selectedRouteDistributeMatchTypes.forEach((type, typeIndex) => {
      routeItem.routeDistribute.forEach((distributeItem, distributeItemIndex) => {
        if (type == distributeItem?.type) {
          // toStr.length > 0 && (toStr += ' & ')
          switch (distributeItem?.type) {
            case 'other':
              {
                distributeItem?.list.forEach((item, index) => {
                  toStr.length > 0 && (toStr += ' & ')
                  toStr += `${item.myKey}${item.condition}${item.value}`
                })
              }
              break
            default:
              toStr.length > 0 && (toStr += ' & ')
              toStr += `${distributeItem.type}${distributeItem.condition}${distributeItem.value}`
          }
        }
      })
    })
    let condition = ''
    if (matchStr.length > 0 && toStr.length > 0) {
      condition = `${matchStr} => ${toStr}`
    } else if (matchStr.length > 0 && toStr.length == 0) {
      condition = `${matchStr}`
    }
    // merge match and tostr
    conditions.push(condition)
  })
  // console.log('matchStr', matchStr)
  // console.log('toStr', toStr)
  return conditions
}

const addRoutingRule = async () => {
  const {
    version,
    ruleGranularity,
    objectOfAction,
    enable,
    faultTolerantProtection,
    runtime,
    group
  } = baseInfo
  const data = {
    configVersion: 'v3.0',
    scope: ruleGranularity,
    key: objectOfAction,
    enabled: enable,
    force: faultTolerantProtection,
    runtime,
    conditions: mergeConditions()
  }

  let ruleName = ''
  if (ruleGranularity == 'application') {
    ruleName = `${objectOfAction}.condition-router`
  } else {
    ruleName = `${objectOfAction}:${version || ''}:${group || ''}.condition-router`
  }
  const res = await addConditionRuleAPI(<string>ruleName, data)
  if (res.code === 200) {
    router.push('/traffic/routingRule')
  }
}

function parseConditionMatchStringToArray(matchStr: string, routeItemIndex: number) {
  const tempArray: any = []
  const parts = matchStr.split(' & ')

  // Initialize default structure
  const defaultStructure = [
    { type: 'host', condition: '', value: '' },
    { type: 'application', condition: '', value: '' },
    { type: 'method', condition: '', value: '' },
    { type: 'arguments', list: [] },
    { type: 'attachments', list: [] },
    { type: 'other', list: [] }
  ]

  // Copy default structure to result array
  defaultStructure.forEach((item) => tempArray.push({ ...item }))

  parts.forEach((part) => {
    part = part.trim()
    // Handle host
    if (part.startsWith('host')) {
      routeList.value[routeItemIndex].selectedMatchConditionTypes.push('host')
      const match = part.match(/^host(!=|=)(.+)/)
      if (match) {
        const condition = match[1]
        const value = match[2].trim()
        const hostObj = tempArray.find((item) => item.type === 'host')
        hostObj.condition = condition
        hostObj.value = value
      }
    }
    // Handle application
    else if (part.startsWith('application')) {
      routeList.value[routeItemIndex].selectedMatchConditionTypes.push('application')
      const match = part.match(/^application(!=|=)(.+)/)
      if (match) {
        const condition = match[1]
        const value = match[2].trim()
        const appObj = tempArray.find((item) => item.type === 'application')
        appObj.condition = condition
        appObj.value = value
      }
    }
    // Handle method
    else if (part.startsWith('method')) {
      routeList.value[routeItemIndex].selectedMatchConditionTypes.push('method')
      const match = part.match(/^method(!=|=)(.+)/)
      if (match) {
        const condition = match[1]
        const value = match[2].trim()
        const methodObj = tempArray.find((item) => item.type === 'method')
        methodObj.condition = condition
        methodObj.value = value
      }
    }
    // Handle arguments
    else if (part.startsWith('arguments')) {
      !routeList.value[routeItemIndex].selectedMatchConditionTypes.includes('arguments') &&
        routeList.value[routeItemIndex].selectedMatchConditionTypes.push('arguments')
      const match = part.match(/^arguments\[(\d+)\](!=|=)(.+)/)
      if (match) {
        const index = parseInt(match[1], 10)
        const condition = match[2]
        const value = match[3].trim()
        const argObj = tempArray.find((item) => item.type === 'arguments')
        argObj.list.push({ index, condition, value })
      }
    }
    // Handle attachments
    else if (part.startsWith('attachments')) {
      !routeList.value[routeItemIndex].selectedMatchConditionTypes.includes('attachments') &&
        routeList.value[routeItemIndex].selectedMatchConditionTypes.push('attachments')
      const match = part.match(/^attachments\[(.+)\](!=|=)(.+)/)
      if (match) {
        const myKey = match[1].trim()
        const condition = match[2]
        const value = match[3].trim()
        const attachObj = tempArray.find((item) => item.type === 'attachments')
        attachObj.list.push({ myKey, condition, value })
      }
    }
    // Handle other
    // else if (part.startsWith('other')) {
    //   !routeList.value[routeItemIndex].selectedMatchConditionTypes.includes('other') &&
    //     routeList.value[routeItemIndex].selectedMatchConditionTypes.push('other')
    //   const match = part.match(/^other\[(.+)\](!=|=)(.+)/)
    //   if (match) {
    //     const myKey = match[1].trim()
    //     const condition = match[2]
    //     const value = match[3].trim()
    //     const otherObj = tempArray.find((item) => item.type === 'other')
    //     otherObj.list.push({ myKey, condition, value })
    //   }
    // }
    else {
      // Parse unknown condition, assume format is key=value
      const match = part.match(/^([^!=]+)(!?=)(.+)$/)
      if (match) {
        !routeList.value[routeItemIndex].selectedMatchConditionTypes.includes('other') &&
          routeList.value[routeItemIndex].selectedMatchConditionTypes.push('other')
        const otherItem = tempArray.find((item) => item.type === 'other')
        if (otherItem) {
          otherItem.list.push({
            myKey: match[1].trim(),
            condition: match[2], // '=' 或 '!='
            value: match[3].trim()
          })
        }
      }
    }
  })

  return tempArray
}

function parseConditionToStringToArray(toStr: string, routeItemIndex: number) {
  const tempArray: any = []
  const parts = toStr?.split(' & ')

  // Initialize default structure
  const defaultStructure = [
    { type: 'host', condition: '', value: '' },
    { type: 'other', list: [] }
  ]

  // Copy default structure to result array
  defaultStructure.forEach((item) => tempArray.push({ ...item }))

  parts?.length &&
    parts.forEach((part) => {
      part = part.trim()
      // Handle host
      if (part.startsWith('host')) {
        routeList.value[routeItemIndex].selectedRouteDistributeMatchTypes.push('host')
        const match = part.match(/^host(!=|=)(.+)/)
        if (match) {
          const condition = match[1]
          const value = match[2].trim()
          const hostObj = tempArray.find((item) => item.type === 'host')
          hostObj.condition = condition
          hostObj.value = value
        }
      }

      // Handle other
      // else if (part.startsWith('other')) {
      //   !routeList.value[routeItemIndex].selectedRouteDistributeMatchTypes.includes('other') &&
      //     routeList.value[routeItemIndex].selectedRouteDistributeMatchTypes.push('other')
      //   const match = part.match(/^other\[(.+)\](!=|=)(.+)/)
      //   if (match) {
      //     const myKey = match[1].trim()
      //     const condition = match[2]
      //     const value = match[3].trim()
      //     const otherObj = tempArray.find((item) => item.type === 'other')
      //     otherObj.list.push({ myKey, condition, value })
      //   }
      // }
      else {
        // Parse unknown condition, assume format is key=value
        const match = part.match(/^([^!=]+)(!?=)(.+)$/)
        if (match) {
          !routeList.value[routeItemIndex].selectedRouteDistributeMatchTypes.includes('other') &&
            routeList.value[routeItemIndex].selectedRouteDistributeMatchTypes.push('other')
          const otherItem = tempArray.find((item) => item.type === 'other')
          if (otherItem) {
            otherItem.list.push({
              myKey: match[1].trim(),
              condition: match[2], // '=' 或 '!='
              value: match[3].trim()
            })
          }
        }
      }
    })
  return tempArray
}
</script>

<style lang="less" scoped>
.__container_routingRule_detail {
  overflow: auto;
  max-height: calc(100vh - 200px);

  &::-webkit-scrollbar {
    display: none;
  }

  .action-icon {
    font-size: 17px;
    margin-left: 10px;
    cursor: pointer;
  }

  .match-condition-type-label {
    min-width: 100px;
    text-align: center;
  }

  .bottom-action-footer {
    width: 100%;
    background-color: white;
    height: 50px;
    display: flex;
    align-items: center;
    padding-left: 20px;
    box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
    /* 添加顶部阴影 */
  }

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
