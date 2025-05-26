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
  <div class="__container_ins_config">
    <config-page :options="options">
      <template v-slot:form_log="{ current }">
        <a-form-item :label="$t('instanceDomain.operatorLog')" name="logFlag">
          <a-switch v-model:checked="current.form.logFlag"></a-switch>
        </a-form-item>
      </template>

      <template v-slot:form_flowDisabled="{ current }">
        <a-form-item :label="$t('instanceDomain.flowDisabled')" name="flowDisabledFlag">
          <a-switch v-model:checked="current.form.flowDisabledFlag"></a-switch>
        </a-form-item>
      </template>
    </config-page>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import ConfigPage from '@/components/ConfigPage.vue'
import { useRoute } from 'vue-router'
import {
  getInstanceLogSwitchAPI,
  getInstanceTrafficSwitchAPI,
  updateInstanceLogSwitchAPI,
  updateInstanceTrafficSwitch,
  updateInstanceTrafficSwitchAPI
} from '@/api/service/instance'
const route = useRoute()

let options: any = reactive({
  list: [
    {
      title: 'instanceDomain.operatorLog',
      key: 'log',
      form: {
        logFlag: false
      },
      submit: (form: any) => {
        return new Promise((resolve) => {
          resolve(updateInstanceLogSwitch(form?.logFlag))
        })
      },
      reset(form: any) {
        form.logFlag = false
      }
    },
    {
      title: 'instanceDomain.flowDisabled',
      form: {
        flowDisabledFlag: false
      },
      key: 'flowDisabled',
      submit: (form: any) => {
        return new Promise((resolve) => {
          resolve(updateInstanceTrafficSwitch(form?.flowDisabledFlag))
        })
      },
      reset(form: any) {
        form.logFlag = false
      }
    }
  ],
  current: [0]
})

// Get whether execution logs are enabled.
const getInstanceLogSwitch = async () => {
  const res = await getInstanceLogSwitchAPI(
    <string>route.params?.pathId,
    <string>route.params?.appName
  )
  if (res?.code == 200) {
    options.list.forEach((item: any) => {
      if (item.key === 'log') {
        item.form.logFlag = res.data.operatorLog
        return
      }
    })
  }
}

// Switch whether to enable execution logs.
const updateInstanceLogSwitch = async (operatorLog: boolean) => {
  const res = await updateInstanceLogSwitchAPI(
    <string>route.params?.pathId,
    <string>route.params?.appName,
    operatorLog
  )
  if (res?.code == 200) {
    await getInstanceLogSwitch()
  }
}

// Get whether traffic is disabled.
const getInstanceTrafficSwitch = async () => {
  const res = await getInstanceTrafficSwitchAPI(
    <string>route.params?.pathId,
    <string>route.params?.appName
  )
  if (res?.code == 200) {
    options.list.forEach((item: any) => {
      if (item.key === 'flowDisabled') {
        item.form.flowDisabledFlag = res.data.trafficDisable
      }
    })
  }
}

//  Switch whether to enable traffic.
const updateInstanceTrafficSwitch = async (trafficDisable: boolean) => {
  const res = await updateInstanceTrafficSwitchAPI(
    <string>route.params?.pathId,
    <string>route.params?.appName,
    trafficDisable
  )
  console.log(res)
  return
  if (res?.code == 200) {
    await getInstanceTrafficSwitch()
  }
}

onMounted(() => {
  console.log(333)
  getInstanceLogSwitch()
  getInstanceTrafficSwitch()
})
</script>

<style lang="less" scoped>
.__container_app_config {
}
</style>
