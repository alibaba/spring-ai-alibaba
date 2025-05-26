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
    <a-spin :spinning="loading">
      <a-flex style="width: 100%">
        <a-col :span="isDrawerOpened ? 24 - sliderSpan : 24" class="left">
          <a-flex vertical align="end">
            <a-row style="width: 100%" justify="space-between">
              <a-col :span="12"> </a-col>
              <a-col :span="12">
                <!--                todo 版本记录后续添加-->
                <!--                <a-button-->
                <!--                  type="text"-->
                <!--                  style="color: #0a90d5; float: right; margin-top: -5px"-->
                <!--                  @click="isDrawerOpened = !isDrawerOpened"-->
                <!--                >-->
                <!--                  {{ $t('flowControlDomain.versionRecords') }}-->
                <!--                  <DoubleLeftOutlined v-if="!isDrawerOpened" />-->
                <!--                  <DoubleRightOutlined v-else />-->
                <!--                </a-button>-->
              </a-col>
            </a-row>

            <div class="editorBox">
              <MonacoEditor
                v-model:modelValue="YAMLValue"
                @change="changeEditor"
                theme="vs-dark"
                height="calc(100vh - 450px)"
                language="yaml"
                :readonly="!isEdit"
              />
            </div>
          </a-flex>
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
    </a-spin>
  </a-card>

  <a-flex v-if="isEdit" style="margin-top: 30px">
    <a-button type="primary" @click="saveConfig">保存</a-button>
    <a-button style="margin-left: 30px" @click="resetConfig">重置</a-button>
  </a-flex>
</template>

<script setup lang="ts">
import MonacoEditor from '@/components/editor/MonacoEditor.vue'
import { DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons-vue'
import { computed, inject, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import {
  addConfiguratorDetail,
  getConfiguratorDetail,
  saveConfiguratorDetail
} from '@/api/service/traffic'
// @ts-ignore
import yaml from 'js-yaml'
import { message } from 'ant-design-vue'
import { PRIMARY_COLOR } from '@/base/constants'
import { ViewDataModel } from '@/views/traffic/dynamicConfig/model/ConfigModel'

const route = useRoute()
const isEdit = ref(route.params.isEdit === '1')
const isDrawerOpened = ref(false)
const loading = ref(false)
const sliderSpan = ref(8)
const TAB_STATE = inject(PROVIDE_INJECT_KEY.PROVIDE_INJECT_KEY)

const YAMLValue = ref()
const initValue = ref()
const ruleName = ref('')

onMounted(async () => {
  await initConfig()
})
const modify = computed(() => {
  return initValue.value !== JSON.stringify(YAMLValue.value)
})
const viewData = reactive(new ViewDataModel())
async function initConfig() {
  if (TAB_STATE.dynamicConfigForm?.data) {
    viewData.fromData(TAB_STATE.dynamicConfigForm.data)
  } else {
    if (route.params?.pathId === '_tmp') {
      isEdit.value = true
      viewData.isAdd = true
    } else {
      viewData.isAdd = false
      const res = await getConfiguratorDetail({ name: route.params?.pathId })
      viewData.fromApiOutput(res.data)
    }
    TAB_STATE.dynamicConfigForm = reactive({
      data: viewData
    })
  }
  const toApiInput = viewData.toApiInput()
  ruleName.value = toApiInput.ruleName
  toApiInput.ruleName = undefined
  const json = yaml.dump(toApiInput) // 输出为 json 格式
  initValue.value = JSON.stringify(json)
  YAMLValue.value = json
}
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
const router = useRouter()
async function saveConfig() {
  loading.value = true
  let data = yaml.load(YAMLValue.value)
  try {
    if (viewData.isAdd === true) {
      addConfiguratorDetail({ name: viewData.basicInfo.key + '.configurators' }, data)
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
    viewData.fromApiOutput(res.data)
    message.success('config save success')
  } finally {
    loading.value = false
  }
}

function changeEditor(val) {
  viewData.fromApiOutput(yaml.load(YAMLValue.value))
}
</script>

<style scoped lang="less">
.editorBox {
  border-radius: 0.3rem;
  overflow: hidden;
  width: 100%;
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
</style>
