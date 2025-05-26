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
  <div class="__container_traffic_config_form">
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
                <a-form
                  ref="baseInfoRef"
                  :model="baseInfo"
                  :rules="baseInfoRules"
                  :labelCol="{ span: 8 }"
                  layout="horizontal"
                >
                  <a-row style="width: 100%">
                    <a-col :span="12">
                      <a-form-item label="规则粒度" name="scope">
                        <a-select
                          v-model:value="baseInfo.scope"
                          style="width: 100%"
                          :options="scopeOptions"
                        ></a-select>
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="作用对象" name="key">
                        <a-input v-model:value="baseInfo.key" style="width: 100%" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row style="width: 100%">
                    <a-col :span="12">
                      <a-form-item label="是否启用" name="enable">
                        <a-switch
                          v-model:checked="baseInfo.enabled"
                          checked-children="是"
                          un-checked-children="否"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>
            </a-row>

            <a-card title="配置列表" style="width: 100%" class="_detail">
              <a-spin :spinning="loading">
                <a-card v-for="(config, index) in formViewEdit.config" class="dynamic-config-card">
                  <template #title>
                    <a-button
                      danger
                      size="small"
                      @click="delConfig(index)"
                      style="margin-right: 10px"
                      >删除 </a-button
                    >配置【{{ index + 1 }}】
                    <span
                      style="font-weight: normal; font-size: 12px"
                      :style="{ color: PRIMARY_COLOR }"
                    >
                      对于{{
                        formViewData?.basicInfo?.scope === 'application' ? '应用' : '服务'
                      }}的{{ config.side === 'provider' ? '提供者' : '消费者' }}，将满足
                      <a-tag
                        :color="PRIMARY_COLOR"
                        v-for="item in config.matches.map(
                          (x: any) => x.key + ' ' + x.relation + ' ' + x.value
                        )"
                      >
                        {{ item }}
                      </a-tag>
                      的实例，配置
                      <a-tag
                        :color="PRIMARY_COLOR"
                        v-for="item in config.parameters.map((x: any) => x.key + ' = ' + x.value)"
                      >
                        {{ item }}
                      </a-tag>
                    </span>
                  </template>
                  <a-descriptions :column="2">
                    <a-descriptions-item
                      :label="$t('flowControlDomain.enabled')"
                      :labelStyle="{ fontWeight: 'bold' }"
                    >
                      <a-switch
                        v-model:checked="config.enabled"
                        checked-children="是"
                        un-checked-children="否"
                      />
                    </a-descriptions-item>
                    <a-descriptions-item
                      :label="$t('flowControlDomain.side')"
                      :labelStyle="{ fontWeight: 'bold' }"
                    >
                      <a-radio-group v-model:value="config.side" :options="sideOptions" />
                    </a-descriptions-item>
                    <a-descriptions-item
                      :label="$t('flowControlDomain.matches')"
                      :labelStyle="{ fontWeight: 'bold' }"
                      :span="2"
                    >
                      <div>
                        <a-select
                          ref="select"
                          v-model:value="config.matchKeys"
                          style="width: 20vw"
                          @change="handleChange(index, 'matches', 'matchKeys')"
                          mode="multiple"
                          :options="matchesArr.map((item) => ({ value: item }))"
                        />
                        <div
                          v-for="item in config.matches"
                          :key="item.key"
                          style="margin-top: 20px"
                        >
                          <a-input-group compact>
                            <a-input disabled :value="item.key" style="width: 10vw" />
                            <a-input
                              placeholer="relation"
                              v-model:value="item.relation"
                              style="width: 10vw"
                            />
                            <a-input
                              placeholer="value"
                              v-model:value="item.value"
                              style="width: 15vw"
                            />
                          </a-input-group>
                        </div>
                      </div>
                    </a-descriptions-item>
                    <a-descriptions-item
                      :label="$t('flowControlDomain.configurationItem')"
                      :labelStyle="{ fontWeight: 'bold' }"
                      :span="2"
                    >
                      <div>
                        <a-select
                          ref="select"
                          v-model:value="config.parameterKeys"
                          style="width: 20vw"
                          @change="handleChange(index, 'parameters', 'parameterKeys')"
                          mode="multiple"
                          :options="parametersArr.map((item) => ({ value: item }))"
                        />
                        <div
                          v-for="item in config.parameters"
                          :key="item.key"
                          style="margin-top: 20px"
                        >
                          <a-input-group compact>
                            <a-input disabled :value="item.key" style="width: 10vw" />
                            <a-input
                              disabled
                              placeholer="relation"
                              :value="item.relation"
                              style="width: 10vw"
                            />
                            <a-input
                              placeholer="value"
                              v-model:value="item.value"
                              style="width: 15vw"
                            />
                          </a-input-group>
                        </div>
                      </div>
                    </a-descriptions-item>
                  </a-descriptions>
                </a-card>
              </a-spin>
            </a-card>
            <a-button @click="addConfig" type="primary"> 增加配置</a-button>
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
              <a-descriptions-item label="enabled">
                是否启用<br />
                可能的值：true, false<br />
                描述：如果为true，则所有配置项都会启用（配置项没有设置enabled的情况下）；如果为false，则所有配置项都会禁用
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </a-card>
      </a-col>
    </a-flex>

    <a-card class="footer">
      <a-flex>
        <a-button type="primary" @click="saveConfig">确认</a-button>
        <a-button style="margin-left: 30px" @click="router.replace('/traffic/dynamicConfig')"
          >取消
        </a-button>
      </a-flex>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import type { ComponentInternalInstance } from 'vue'
import { getCurrentInstance, nextTick, onMounted, reactive, ref } from 'vue'
import { PRIMARY_COLOR } from '@/base/constants'
import useClipboard from 'vue-clipboard3'
import { FormInstance, message, Modal } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { addConfiguratorDetail, saveConfiguratorDetail } from '@/api/service/traffic'
import gsap from 'gsap'

let __ = PRIMARY_COLOR
const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()

const route = useRoute()
const router = useRouter()
const isEdit = ref(route.params.isEdit === '1')
const sliderSpan = ref(8)
const isDrawerOpened = ref(false)
const baseInfo = reactive({
  scope: 'application',
  key: null,
  enabled: false
})
const baseInfoRules = {
  scope: [{ required: true, message: '请添加作用范围!' }],
  key: [{ required: true, message: '请添加作用对象!' }],
  enabled: []
}
const scopeOptions = reactive([
  {
    label: '应用',
    value: 'application'
  },
  {
    label: '服务',
    value: 'service'
  }
])
const toClipboard = useClipboard().toClipboard

function copyIt(v: string) {
  message.success(globalProperties.$t('messageDomain.success.copy'))
  toClipboard(v)
}

const formViewData: any = reactive({
  basicInfo: {
    ruleName: 'org.apache.dubbo.samples.UserService::.condition-router',
    scope: '服务',
    key: 'org.apache.dubbo.samples.UserService',
    effectTime: '20230/12/19 22:09:34',
    enabled: true
  },
  config: [
    {
      enabled: true,
      side: 'provider',
      matchKeys: [],
      matches: [],
      parameterKeys: [],
      parameters: []
    }
  ]
})

const matchesArr = ['address', 'providerAddress', 'service', 'app', 'param']
const parametersArr = ['retries', 'timeout', 'accesslog', 'weight', '其他']

const formViewEdit: any = formViewData

const sideOptions = [
  {
    label: 'provider',
    value: 'provider'
  },
  {
    label: 'consumer',
    value: 'consumer'
  }
]
const baseInfoRef = ref<FormInstance>()
const delConfig = (idx) => {
  Modal.confirm({
    title: '确认删除该配置么？',
    onOk() {
      formViewEdit.config.splice(idx, 1)
    }
  })
}
const addConfig = async () => {
  const container: any = document.getElementById('layout-tab-body')
  formViewEdit.config.push({
    enabled: true,
    side: 'provider',
    matches: [],
    parameters: []
  })
  nextTick(() => {
    gsap.to(container, {
      duration: 1, // 动画持续时间（秒）
      scrollTop: container.scrollHeight,
      ease: 'power2.out'
    })
  })
}

const handleChange = (index: number, name: string, keys: string) => {
  const config: any = formViewData.config[index]
  config[name] = config[name].filter((item: any) => {
    return config[keys].find((i: any) => {
      return i === item.key
    })
  })
  config[keys].forEach((item: any) => {
    if (
      !config[name].find((i: any) => {
        return i.key === item
      })
    ) {
      config[name].push({
        key: item,
        relation: name === 'parameters' ? '=' : 'exact',
        value: ''
      })
    }
  })
}

function transApiData(data: any) {
  if (data) {
    formViewData.basicInfo.configVerison = data.configVerison
    formViewData.basicInfo.scope = data.scope
    formViewData.basicInfo.key = data.key
    formViewData.basicInfo.enabled = data.enabled
    formViewData.config = data.configs.map((x: any) => {
      let matches = []
      for (let matchKey in x.match) {
        let relation = Object.keys(x.match[matchKey])[0]
        matches.push({
          key: matchKey,
          relation: relation,
          value: x.match[matchKey][relation]
        })
      }
      let parameters = []
      for (let paramKey in x.parameters) {
        parameters.push({
          key: paramKey,
          relation: '=',
          value: x.parameters[paramKey]
        })
      }

      return {
        enabled: x.enabled,
        side: x.side,
        matchKeys: Object.keys(x.match),
        matches: matches,
        parameterKeys: Object.keys(x.parameters),
        parameters: parameters
      }
    })
  }
}

const hasUnsavedChanges = ref(true)

onMounted(() => {})

const loading = ref(false)

async function saveConfig() {
  try {
    const values = await baseInfoRef.value?.validateFields()
    // message.success("form valid success: " + JSON.stringify(values))
  } catch (errorInfo) {
    console.error('form valid error: ' + JSON.stringify(errorInfo))
    message.error('表单验证失败')
    return
  }
  loading.value = true
  let newVal = {
    scope: baseInfo.scope,
    key: baseInfo.key,
    enabled: baseInfo.enabled,
    configVersion: 'v3.0',
    configs: formViewEdit.config.map((x: any) => {
      const match: any = {}
      const parameters: any = {}
      for (let m of x.matches) {
        match[m.key] = { [m.relation]: m.value }
      }
      for (let m of x.parameters) {
        parameters[m.key] = m.value
      }
      return {
        match,
        parameters,
        enabled: x.enabled,
        side: x.side
      }
    })
  }
  try {
    let res = await addConfiguratorDetail({ name: baseInfo.key + '.configurators' }, newVal)
    transApiData(res.data)
    message.success('config save success')
  } catch (e: any) {
    message.error(e.msg)
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="less" scoped>
.__container_traffic_config_form {
  position: relative;
  width: 100%;

  .footer {
    //position: sticky; //bottom: 0; //width: 100%;
  }

  .dynamic-config-card {
    :deep(.ant-descriptions-item-label) {
      width: 120px;
      text-align: right;
    }

    margin-bottom: 20px;

    .description-item-content {
      &.no-card {
        padding-left: 20px;
      }

      &.with-card:hover {
        color: v-bind('PRIMARY_COLOR');
      }
    }
  }
}
</style>
