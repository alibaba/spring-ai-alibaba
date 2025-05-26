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
    <a-card title="基础信息" class="dynamic-config-card">
      <div>
        <a-descriptions :column="2" layout="vertical">
          <a-descriptions-item
            :label="$t('flowControlDomain.scope')"
            :labelStyle="{ fontWeight: 'bold' }"
          >
            <a-select
              v-model:value="formViewEdit.basicInfo.scope"
              style="min-width: 120px"
              :options="scopeOptions"
              :disabled="!isEdit"
            >
            </a-select>
          </a-descriptions-item>

          <a-descriptions-item
            :label="$t('flowControlDomain.key')"
            :labelStyle="{ fontWeight: 'bold' }"
          >
            <a-input
              v-model:value="formViewEdit.basicInfo.key"
              style="min-width: 300px"
              :disabled="!isEdit"
            />
          </a-descriptions-item>

          <a-descriptions-item
            :label="$t('flowControlDomain.enabled')"
            :labelStyle="{ fontWeight: 'bold' }"
          >
            <a-switch
              v-model:checked="formViewEdit.basicInfo.enabled"
              checked-children="是"
              un-checked-children="否"
              :disabled="!isEdit"
            />
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-card>

    <a-spin :spinning="loading">
      <a-form ref="formRef">
        <a-card v-for="(config, index) in formViewEdit.config" class="dynamic-config-card">
          <template #title>
            <a-button
              v-if="!isEdit"
              danger
              size="small"
              @click="delConfig(index)"
              style="margin-right: 10px"
              >删除
            </a-button>
            配置【{{ index + 1 }}】
            <div class="desc-config" :style="{ color: PRIMARY_COLOR }">
              对于{{ formViewData?.basicInfo?.scope === 'application' ? '应用' : '服务' }}的{{
                config.side === 'provider' ? '提供者' : '消费者'
              }}，将满足
              <a-tag :color="PRIMARY_COLOR" v-for="item in descMatchesComputed(config)">
                {{ item }}
              </a-tag>
              的实例，配置
              <a-tag :color="PRIMARY_COLOR" v-for="item in descParametersComputed(config)">
                {{ item }}
              </a-tag>
            </div>
          </template>

          <a-descriptions :column="2">
            <a-descriptions-item
              :label="$t('flowControlDomain.enabled')"
              :labelStyle="{ fontWeight: 'bold' }"
            >
              <a-switch
                :disabled="!isEdit"
                v-model:checked="config.enabled"
                checked-children="是"
                un-checked-children="否"
              />
            </a-descriptions-item>
            <a-descriptions-item
              :label="$t('flowControlDomain.side')"
              :labelStyle="{ fontWeight: 'bold' }"
            >
              <a-radio-group
                :disabled="!isEdit"
                v-model:value="config.side"
                :options="sideOptions"
              />
            </a-descriptions-item>
            <a-descriptions-item
              :label="$t('flowControlDomain.matches')"
              :labelStyle="{ fontWeight: 'bold' }"
              :span="2"
            >
              <div>
                <a-button
                  :disabled="!isEdit"
                  v-if="!config.hasMatch"
                  @click="config.hasMatch = true"
                  type="dashed"
                >
                  <Icon style="margin-bottom: -2px; font-size: 16px" icon="si:add-fill"></Icon>
                  {{ $t('dynamicConfigDomain.addMatches') }}
                </a-button>
                <a-card v-else style="display: block; margin-top: 10px; width: 60vw">
                  <a-form-item
                    :label-col="{ style: { width: '9vw' } }"
                    :label="$t('dynamicConfigDomain.matchType')"
                  >
                    <a-input-group compact>
                      <a-select
                        :disabled="!isEdit"
                        ref="select"
                        v-model:value="config.matchesKeys"
                        style="width: 30vw"
                        @change="handleChange(index, 'matches', 'matchesKeys')"
                        mode="multiple"
                        :options="Object.keys(config.matchesValue).map((item) => ({ value: item }))"
                      />
                      <a-button
                        v-if="!isEdit"
                        @click="config.hasMatch = false"
                        style="margin-left: 10px; padding: 5px"
                        danger
                      >
                        <Icon
                          style="font-size: 20px; margin-bottom: -2px"
                          icon="mynaui:minus-solid"
                        ></Icon>
                      </a-button>
                    </a-input-group>
                  </a-form-item>

                  <div
                    v-for="key in Object.keys(config.matchesValue).filter((x) =>
                      config.matchesKeys.includes(x)
                    )"
                    :key="key"
                  >
                    <a-form-item
                      v-if="config.matchesValue[key].type === 'obj'"
                      :label-col="{ style: { width: '9vw' } }"
                    >
                      <template #label>
                        <a-tag :color="PRIMARY_COLOR">{{ key }}</a-tag>
                      </template>
                      <a-input-group compact>
                        <a-input disabled :value="key" style="width: 10vw; margin-bottom: 5px" />
                        <a-select
                          :disabled="!isEdit"
                          placeholder="relation"
                          :options="addressRelation"
                          v-model:value="config.matchesValue[key].relation"
                          style="width: 8vw"
                        />
                        <a-input
                          :disabled="!isEdit"
                          placeholder="value"
                          v-model:value="config.matchesValue[key].value"
                          style="width: 15vw"
                        />
                      </a-input-group>
                    </a-form-item>
                    <a-form-item v-else :label-col="{ style: { width: '9vw' } }">
                      <template #label>
                        <a-tag :color="PRIMARY_COLOR">{{ key }}</a-tag>
                      </template>
                      <a-input-group
                        :disabled="!isEdit"
                        v-for="(item, idx) in config.matchesValue[key].arr"
                        style="margin-bottom: 5px"
                        compact
                      >
                        <a-input
                          placeholder="key"
                          disabled
                          v-if="config.matchesValue[key].type === 'arr'"
                          value="oneof"
                          style="width: 10vw"
                        />
                        <a-input
                          v-else
                          placeholder="key"
                          :disabled="!isEdit || config.matchesValue[key].type === 'arr'"
                          v-model:value="item.key"
                          style="width: 10vw"
                        />
                        <a-select
                          placeholder="relation"
                          :disabled="!isEdit"
                          v-model:value="item.relation"
                          :options="paramRelation"
                          style="width: 8vw"
                        />
                        <a-input
                          placeholder="value"
                          :disabled="!isEdit"
                          v-model:value="item.value"
                          style="width: 15vw"
                        />
                        <a-button
                          v-if="!isEdit"
                          :disabled="config.matchesValue[key].arr.length === 1"
                          @click="config.delArrConfig(config.matchesValue, key, idx)"
                          style="margin-left: 10px; padding: 5px"
                          danger
                        >
                          <Icon
                            style="font-size: 20px; margin-bottom: -2px"
                            icon="mynaui:minus-solid"
                          ></Icon>
                        </a-button>
                        <a-button
                          v-if="!isEdit"
                          @click="config.addArrConfig(config.matchesValue, key, idx)"
                          style="margin-left: 10px; padding: 5px"
                          type="primary"
                        >
                          <Icon
                            style="font-size: 20px; margin-bottom: -2px"
                            icon="mynaui:plus-solid"
                          ></Icon>
                        </a-button>
                      </a-input-group>
                    </a-form-item>
                  </div>
                </a-card>
              </div>
            </a-descriptions-item>
            <a-descriptions-item
              :label="$t('flowControlDomain.configurationItem')"
              :labelStyle="{ fontWeight: 'bold' }"
              :span="2"
            >
              <div>
                <a-card style="display: block; margin-top: 10px; width: 60vw">
                  <a-form-item
                    :label-col="{ style: { width: '9vw' } }"
                    :label="$t('dynamicConfigDomain.configType')"
                  >
                    <a-input-group compact>
                      <a-select
                        ref="select"
                        :disabled="!isEdit"
                        v-model:value="config.parametersKeys"
                        style="width: 30vw"
                        @change="handleChange(index, 'matches', 'matchesKeys')"
                        mode="multiple"
                        :options="
                          Object.keys(config.parametersValue).map((item) => ({ value: item }))
                        "
                      />
                    </a-input-group>
                  </a-form-item>

                  <div
                    v-for="key in Object.keys(config.parametersValue).filter((x) =>
                      config.parametersKeys.includes(x)
                    )"
                    :key="key"
                  >
                    <a-form-item
                      v-if="config.parametersValue[key].type === 'obj'"
                      :label-col="{ style: { width: '9vw' } }"
                    >
                      <template #label>
                        <a-tag :color="PRIMARY_COLOR">{{ key }}</a-tag>
                      </template>
                      <a-input-group compact>
                        <a-input disabled :value="key" style="width: 10vw; margin-bottom: 5px" />
                        <a-input disabled placeholder="relation" value="=" style="width: 8vw" />
                        <a-input
                          :disabled="!isEdit"
                          placeholder="value"
                          v-model:value="config.parametersValue[key].value"
                          style="width: 15vw"
                        />
                      </a-input-group>
                    </a-form-item>
                    <a-form-item v-else :label-col="{ style: { width: '9vw' } }">
                      <template #label>
                        <a-tag :color="PRIMARY_COLOR">{{ key }}</a-tag>
                      </template>
                      <a-input-group
                        v-for="(item, idx) in config.parametersValue[key].arr"
                        style="margin-bottom: 5px"
                        compact
                      >
                        <a-input
                          placeholder="key"
                          disabled
                          v-if="config.parametersValue[key].type === 'arr'"
                          :value="key"
                          style="width: 10vw"
                        />
                        <a-input
                          placeholder="key"
                          v-else
                          :disabled="!isEdit"
                          v-model:value="item.key"
                          style="width: 10vw"
                        />
                        <a-input disabled placeholder="relation" value="=" style="width: 8vw" />
                        <a-input
                          placeholder="value"
                          :disabled="!isEdit"
                          v-model:value="item.value"
                          style="width: 15vw"
                        />
                        <a-button
                          v-if="!isEdit"
                          :disabled="config.parametersValue[key].arr.length === 1"
                          @click="config.delArrConfig(config.parametersValue, key, idx)"
                          style="margin-left: 10px; padding: 5px"
                          danger
                        >
                          <Icon
                            style="font-size: 20px; margin-bottom: -2px"
                            icon="mynaui:minus-solid"
                          ></Icon>
                        </a-button>
                        <a-button
                          v-if="!isEdit"
                          @click="
                            config.addArrConfig(config.parametersValue, key, idx, { relation: '=' })
                          "
                          style="margin-left: 10px; padding: 5px"
                          type="primary"
                        >
                          <Icon
                            style="font-size: 20px; margin-bottom: -2px"
                            icon="mynaui:plus-solid"
                          ></Icon>
                        </a-button>
                      </a-input-group>
                    </a-form-item>
                  </div>
                </a-card>
              </div>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-form>
    </a-spin>

    <a-button style="margin-bottom: 20px" v-if="isEdit" @click="addConfig"
      >{{ $t('dynamicConfigDomain.addConfig') }}
    </a-button>

    <a-card class="footer">
      <a-flex v-if="isEdit">
        <a-button type="primary" @click="saveConfig">{{ $t('dynamicConfigDomain.save') }}</a-button>
        <a-button style="margin-left: 30px" @click="resetConfig">{{
          $t('dynamicConfigDomain.reset')
        }}</a-button>
      </a-flex>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import type { ComponentInternalInstance } from 'vue'
import { computed, getCurrentInstance, inject, nextTick, onMounted, reactive, ref } from 'vue'
import { PRIMARY_COLOR } from '@/base/constants'
import useClipboard from 'vue-clipboard3'
import { message, Modal } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addConfiguratorDetail,
  getConfiguratorDetail,
  saveConfiguratorDetail
} from '@/api/service/traffic'
import gsap from 'gsap'
import { Icon } from '@iconify/vue'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { ConfigModel, ViewDataModel } from '@/views/traffic/dynamicConfig/model/ConfigModel'

let __ = PRIMARY_COLOR
const TAB_STATE = inject(PROVIDE_INJECT_KEY.PROVIDE_INJECT_KEY)
const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()

const route = useRoute()
const router = useRouter()
const isEdit = ref(route.params.isEdit === '1')

const toClipboard = useClipboard().toClipboard

function copyIt(v: string) {
  message.success(globalProperties.$t('messageDomain.success.copy'))
  toClipboard(v)
}

const formViewData: ViewDataModel = reactive(new ViewDataModel())

const formViewEdit: ViewDataModel = formViewData
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
const addressRelation = [
  {
    label: 'wildcard',
    value: 'wildcard'
  },
  {
    label: 'cird',
    value: 'cird'
  },
  {
    label: 'exact',
    value: 'exact'
  }
]
const paramRelation = [
  {
    label: 'exact',
    value: 'exact'
  },
  {
    label: 'prefix',
    value: 'prefix'
  },
  {
    label: 'regex',
    value: 'regex'
  },
  {
    label: 'noempty',
    value: 'noempty'
  },
  {
    label: 'empty',
    value: 'empty'
  },
  {
    label: 'wildcard',
    value: 'wildcard'
  }
]

const descMatchesComputed = computed(() => {
  return (config) => config.descMatches()
})
const descParametersComputed = computed(() => {
  return (config) => config.descParameters()
})

const addConfig = () => {
  const container: any = document.getElementById('layout-tab-body')
  formViewEdit.config.push(new ConfigModel(null))
  nextTick(() => {
    gsap.to(container, {
      duration: 1, // 动画持续时间（秒）
      scrollTop: container.scrollHeight,
      ease: 'power2.out'
    })
  })
}

const handleChange = (index: number, name: string, keys: string) => {
  const config: ConfigModel = formViewData.config[index]
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
    formViewData.fromApiOutput(data)
  }
}

const hasUnsavedChanges = ref(true)

onMounted(async () => {
  await initConfig()
})
const delConfig = (idx) => {
  Modal.confirm({
    title: '确认删除该配置么？',
    onOk() {
      formViewEdit.config.splice(idx, 1)
    }
  })
}
const loading = ref(false)

async function resetConfig() {
  loading.value = true
  try {
    TAB_STATE.dynamicConfigForm.data = null
    await initConfig()
    message.success('config reset success')
  } finally {
    loading.value = false
  }
}

async function initConfig() {
  if (TAB_STATE.dynamicConfigForm?.data) {
    formViewData.fromData(TAB_STATE.dynamicConfigForm.data)
  } else {
    if (route.params?.pathId !== '_tmp') {
      const res = await getConfiguratorDetail({ name: route.params?.pathId })
      transApiData(res.data)
    } else {
      formViewData.basicInfo.ruleName = '_tmp'
      isEdit.value = true
      formViewData.isAdd = true
    }
    TAB_STATE.dynamicConfigForm = reactive({
      data: formViewData
    })
  }
}

async function saveConfig() {
  loading.value = true

  try {
    let data = formViewEdit.toApiInput(true)
    if (formViewData.isAdd === true) {
      addConfiguratorDetail({ name: formViewEdit.basicInfo.key + '.configurators' }, data)
        .then((res) => {
          TAB_STATE.dynamicConfigForm.data = null
          nextTick(() => {
            router.replace('/traffic/dynamicConfig')
            message.success('config add success')
          })
        })
        .catch((e) => {
          message.error('添加失败： ' + e.msg)
        })
      return
    }
    let res = await saveConfiguratorDetail({ name: route.params?.pathId }, data)
    transApiData(res.data)
    message.success('config save success')
  } catch (e) {
    message.error(formViewEdit.errorMsg.join(';'))
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

    &:deep(.ant-card-head-title) {
      overflow: unset !important;
    }

    .desc-config {
      max-width: 60vw;
      display: inline-flex;
      font-weight: normal;
      font-size: 12px;
      line-height: 20px;
      flex-wrap: wrap;
      row-gap: 2px;
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
