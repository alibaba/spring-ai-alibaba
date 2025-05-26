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
  <div class="container-services-tabs-scene-config">
    <config-page :options="options">
      <template v-slot:form_timeout="{ current }">
        <a-form-item :label="$t('serviceDomain.timeout')" name="timeout">
          <a-input-number
            v-model:value="current.form.timeout"
            addon-after="ms"
            style="width: 150px"
          />
        </a-form-item>
      </template>
      <template v-slot:form_retryNum="{ current }">
        <a-form-item :label="$t('serviceDomain.retryNum')" name="retryNum">
          <a-input-number
            v-model:value="current.form.retryNum"
            addon-after="次"
            style="width: 150px"
          />
        </a-form-item>
      </template>
      <template v-slot:form_sameAreaFirst="{ current }">
        <a-form-item :label="$t('serviceDomain.sameAreaFirst')" name="sameAreaFirst">
          <a-radio-group v-model:value="current.form.sameAreaFirst" button-style="solid">
            <a-radio-button :value="false">{{ $t('serviceDomain.closed') }}</a-radio-button>
            <a-radio-button :value="true">{{ $t('serviceDomain.opened') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </template>
      <template v-slot:form_paramRoute="{ current }">
        <a-form-item name="paramRoute">
          <div class="param-route">
            <ParamRoute
              v-for="(item, index) in current.form.paramRoute"
              :key="index"
              :paramRouteForm="item"
              :index="index"
              @deleteParamRoute="deleteParamRoute"
            />
            <a-button type="primary" style="margin-top: 20px" @click="addParamRoute"
              >增加路由</a-button
            >
          </div>
        </a-form-item>
      </template>
    </config-page>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import ParamRoute from './paramRoute.vue'
import ConfigPage from '@/components/ConfigPage.vue'
import {
  getParamRouteAPI,
  getServiceIntraRegionPriorityAPI,
  getServiceRetryAPI,
  getServiceTimeoutAPI,
  updateParamRouteAPI,
  updateServiceIntraRegionPriorityAPI,
  updateServiceRetryAPI,
  updateServiceTimeoutAPI
} from '@/api/service/service'
import { useRoute } from 'vue-router'

const route = useRoute()

const options: any = reactive({
  list: [
    // timeout
    {
      title: 'serviceDomain.timeout',
      key: 'timeout',
      form: {
        timeout: 1000
      },
      submit: (form: any) => {
        return new Promise((resolve) => {
          resolve(updateServiceTimeout(form?.timeout))
        })
      },
      async reset(form: any) {
        await getServiceTimeout()
      }
    },
    // retryNum
    {
      title: 'serviceDomain.retryNum',
      key: 'retryNum',
      form: {
        retryNum: 0
      },
      submit: (form: any) => {
        return new Promise((resolve) => {
          resolve(updateServiceRetry(form?.retryNum))
        })
      },
      async reset(form: any) {
        await getServiceRetry()
      }
    },
    // sameAreaFirst
    {
      title: 'serviceDomain.sameAreaFirst',
      key: 'sameAreaFirst',
      form: {
        sameAreaFirst: false
      },
      submit: (form: any) => {
        return new Promise((resolve) => {
          resolve(updateServiceIntraRegionPriority(form?.sameAreaFirst))
        })
      },
      async reset(form: any) {
        await getServiceIntraRegionPriority()
      }
    },
    // paramRoute
    {
      title: 'serviceDomain.paramRoute',
      key: 'paramRoute',
      form: {
        paramRoute: []
      },
      submit: (form: any) => {
        return new Promise((resolve) => {
          resolve(updateParamRoute(form?.paramRoute))
        })
      },
      reset(form: any) {
        return new Promise((resolve) => {
          resolve(getParamRoute())
        })
      }
    }
  ],
  current: [0]
})

const addParamRoute = () => {
  options.list.forEach((item: any) => {
    if (item.key === 'paramRoute') {
      const newData = {
        method: 'string',
        conditions: [
          {
            index: 'string',
            relation: 'string',
            value: 'string'
          }
        ],
        destinations: [
          {
            conditions: [
              {
                tag: 'string',
                relation: 'string',
                value: 'string'
              }
            ],
            weight: 0
          }
        ]
      }
      item.form.paramRoute.push(newData)
    }
  })
}

const updateParamRoute = async () => {
  const { pathId: serviceName, group, version } = route.params
  options.list.forEach(async (item: any) => {
    if (item.key === 'paramRoute') {
      await updateParamRouteAPI({
        serviceName: serviceName,
        group: group || '',
        version: version || '',
        routes: item.form.paramRoute
      })
      await getParamRoute()
    }
  })
}
const deleteParamRoute = (index: number) => {
  options.list.forEach((item: any) => {
    if (item.key === 'paramRoute') {
      item.form.paramRoute.splice(index, 1)
    }
  })
}

const getParamRoute = async () => {
  const { pathId: serviceName, group, version } = route.params
  const params = {
    serviceName,
    group,
    version
  }
  const res = await getParamRouteAPI(params)
  if (res.code === 200) {
    options.list.forEach((item: any) => {
      if (item.key === 'paramRoute') {
        item.form.paramRoute = res.data?.routes
      }
    })
  }
}

// get timeout
const getServiceTimeout = async () => {
  const { pathId: serviceName, group, version } = route.params
  const params = {
    serviceName,
    group: group || '',
    version: version || ''
  }
  const res = await getServiceTimeoutAPI(params)
  options.list.forEach((item: any) => {
    if (item.key === 'timeout') {
      item.form.timeout = res.data.timeout
    }
  })
}

// update timeout
const updateServiceTimeout = async (timeout: number) => {
  const { pathId: serviceName, group, version } = route.params
  const data = {
    serviceName,
    group: group || '',
    version: version || '',
    timeout
  }
  await updateServiceTimeoutAPI(data)
  await getServiceTimeout()
}

// get service retry
const getServiceRetry = async () => {
  const { pathId: serviceName, group, version } = route.params
  const params = {
    serviceName,
    group: group || '',
    version: version || ''
  }
  const res = await getServiceRetryAPI(params)
  options.list.forEach((item: any) => {
    if (item.key === 'retryNum') {
      item.form.retryNum = res.data.retryTimes
    }
  })
}

// update service retry
const updateServiceRetry = async (retryTimes: number) => {
  const { pathId: serviceName, group, version } = route.params
  const data = {
    serviceName,
    group: group || '',
    version: version || '',
    retryTimes
  }
  await updateServiceRetryAPI(data)
  await getServiceRetry()
}

const getServiceIntraRegionPriority = async () => {
  const { pathId: serviceName, group, version } = route.params
  const params = {
    serviceName,
    group: group || '',
    version: version || ''
  }
  const res: any = await getServiceIntraRegionPriorityAPI(params)
  options.list.forEach((item: any) => {
    if (item.key === 'sameAreaFirst') {
      item.form.sameAreaFirst = res.data?.enabled
    }
  })
}

const updateServiceIntraRegionPriority = async (enabled: boolean) => {
  const { pathId: serviceName, group, version } = route.params
  const data = {
    serviceName,
    group: group || '',
    version: version || '',
    enabled
  }
  await updateServiceIntraRegionPriorityAPI(data)
  await getServiceIntraRegionPriority()
}

onMounted(async () => {
  await getServiceTimeout()
  await getServiceRetry()
  await getServiceIntraRegionPriority()
  await getParamRoute()
})
</script>

<style lang="less" scoped>
.container-services-tabs-scene-config {
  .item-content {
    margin-right: 20px;
  }

  .item-input {
    width: 200px;
  }

  .item-icon {
    margin-left: 15px;
    font-size: 18px;
  }

  .param-route {
    // margin-bottom: 20px;
    height: 320px;
  }
}

.item-content {
  margin-right: 10px;
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}

.item-icon {
  color: #1890ff;
  margin-left: 10px;
  cursor: pointer;
  transition: color 0.3s;
}

.item-icon:hover {
  color: #40a9ff;
}

.item-input {
  width: 120px;
}

.scene-config-pane {
  padding: 16px;
  background-color: #f5f5f5;
  border-radius: 6px;
  margin-bottom: 16px;
  border: 1px solid #e8e8e8;
}

.pane-content {
  background-color: white;
  padding: 16px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}
</style>
