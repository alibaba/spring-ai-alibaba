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
  <div class="__container_app_service">
    <a-flex v-if="false" wrap="wrap" gap="small" :vertical="false" justify="start" align="left">
      <a-card class="statistic-card" v-for="(v, k) in clusterInfo.report">
        <a-flex gap="middle" :vertical="false" justify="space-between" align="center">
          <a-statistic :value="v.value" class="statistic">
            <template #prefix>
              <Icon class="statistic-icon" icon="solar:target-line-duotone"></Icon>
            </template>
            <template #title> {{ $t(k.toString()) }}</template>
          </a-statistic>
          <div class="statistic-icon-big">
            <Icon :icon="v.icon"></Icon>
          </div>
        </a-flex>
      </a-card>
    </a-flex>
    <search-table :search-domain="searchDomain">
      <template #bodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'serviceName'">
          <a-button type="link" @click="viewDetail(text)">{{ text }}</a-button>
        </template>
        <template v-else-if="column.dataIndex === 'versionGroupSelect'">
          <a-select :value="text?.versionGroupValue" :bordered="false" style="width: 80%">
            <a-select-option
              v-for="(item, index) in text?.versionGroupArr"
              :value="item"
              :key="index"
            >
              {{ item }}
            </a-select-option>
          </a-select>
        </template>
      </template>
    </search-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { getClusterInfo } from '@/api/service/clusterInfo'
import { getMetricsMetadata } from '@/api/service/serverInfo'
import { PRIMARY_COLOR } from '@/base/constants'
import { Icon } from '@iconify/vue'
import SearchTable from '@/components/SearchTable.vue'
import { SearchDomain } from '@/utils/SearchUtil'
import { provide } from 'vue'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { useRoute, useRouter } from 'vue-router'
import { getApplicationServiceForm } from '@/api/service/app'
import { searchService } from '@/api/service/service'
import { queryMetrics } from '@/base/http/promQuery'
import { promQueryList } from '@/utils/PromQueryUtil'
import { isNumber } from 'lodash'
import { bytesToHuman } from '@/utils/ByteUtil'

const route = useRoute()
const router = useRouter()

let __null = PRIMARY_COLOR
let clusterInfo = reactive({
  info: <{ [key: string]: string }>{},
  report: <{ [key: string]: { value: string; icon: string } }>{}
})

let metricsMetadata = reactive({
  info: <{ [key: string]: string }>{}
})

onMounted(async () => {
  searchDomain.tableStyle = {
    scrollX: '100',
    scrollY: 'calc(100vh - 600px)'
  }

  let clusterData = (await getClusterInfo({})).data
  metricsMetadata.info = <{ [key: string]: string }>(await getMetricsMetadata({})).data
  clusterInfo.info = <{ [key: string]: string }>clusterData
  clusterInfo.report = {
    providers: {
      icon: 'carbon:branch',
      value: clusterInfo.info.providers
    },
    consumers: {
      icon: 'mdi:merge',
      value: clusterInfo.info.consumers
    }
  }
})

const columns = [
  {
    title: 'provideServiceName',
    key: 'service',
    dataIndex: 'serviceName',
    sorter: true,
    width: '30%'
  },
  {
    title: 'versionGroup',
    key: 'versionGroup',
    dataIndex: 'versionGroupSelect',
    width: '25%'
  },
  {
    title: 'avgQPS',
    key: 'avgQPS',
    dataIndex: 'avgQPS',
    sorter: true,
    width: '15%'
  },
  {
    title: 'avgRT',
    key: 'avgRT',
    dataIndex: 'avgRT',
    sorter: true,
    width: '15%'
  },
  {
    title: 'requestTotal',
    key: 'requestTotal',
    dataIndex: 'requestTotal',
    sorter: true,
    width: '15%'
  }
]

const appName = computed(() => {
  return route.params?.pathId
})

function serviceInfo(params: any) {
  return getApplicationServiceForm(params).then(async (res) => {
    return promQueryList(res, ['qps', 'rt', 'request'], async (service: any) => {
      service.versionGroupSelect = {}
      service.versionGroupSelect.versionGroupArr = service.versionGroups.map((item: any) => {
        return (item.versionGroup =
          (item.version ? 'version: ' + item.version + ', ' : '') +
            (item.group ? 'group: ' + item.group : '') || '无')
      })
      service.versionGroupSelect.versionGroupValue = service.versionGroupSelect.versionGroupArr[0]
      let qps = await queryMetrics(
        `sum (dubbo_provider_qps_total{interface='${service.serviceName}'}) by (interface)`
      )
      let rt = await queryMetrics(
        `avg(dubbo_consumer_rt_avg_milliseconds_aggregate{interface="${service.serviceName}",method=~"$method"}>0)`
      )
      let request = await queryMetrics(
        `sum (increase(dubbo_provider_requests_total{interface="${service.serviceName}"}[1m]))`
      )

      service.avgQPS = qps
      service.avgRT = rt
      service.requestTotal = request
    })
  })
}
const searchDomain = reactive(
  new SearchDomain(
    [
      // {
      //   label: '',
      //   param: 'side',
      //   defaultValue: 'provider',
      //   dict: [
      //     { label: 'providers', value: 'provider' },
      //     { label: 'consumers', value: 'consumer' }
      //   ],
      //   dictType: 'BUTTON'
      // },
      {
        label: 'serviceName',
        param: 'serviceName'
      },
      {
        label: '',
        param: 'appName',
        defaultValue: appName
      }
    ],
    serviceInfo,
    columns,
    { pageSize: 4 },
    true
  )
)
searchDomain.onSearch()

const viewDetail = (serviceName: string) => {
  router.push('/resources/services/distribution/' + serviceName)
}

provide(PROVIDE_INJECT_KEY.SEARCH_DOMAIN, searchDomain)
</script>
<style lang="less" scoped>
.__container_app_service {
  .statistic {
    width: 8vw;
  }
  :deep(.ant-card-body) {
    padding: 12px;
  }

  .statistic-card {
    border: 1px solid v-bind("(PRIMARY_COLOR) + '22'");
    margin-bottom: 30px;
  }

  .statistic-icon {
    color: v-bind(PRIMARY_COLOR);
    margin-bottom: -3px;
    font-weight: bold;
  }

  .statistic-icon-big {
    width: 38px;
    height: 38px;
    background: v-bind('PRIMARY_COLOR');
    line-height: 38px;
    vertical-align: middle;
    text-align: center;
    border-radius: 5px;
    font-size: 20px;
    padding-top: 2px;
    color: white;
  }
}
</style>
