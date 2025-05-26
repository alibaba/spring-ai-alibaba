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
  <div class="__container_app_instance">
    <a-flex wrap="wrap" gap="small" :vertical="false" justify="space-around" align="left">
      <a-card class="statistic-card" v-for="(v, k) in statisticsInfo.report">
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
        <template v-if="column.dataIndex === 'name'">
          <a-button type="link" @click="viewDetail(text)">{{ text }}</a-button>
        </template>
        <template v-if="column.dataIndex === 'deployState'">
          <a-tag :color="INSTANCE_DEPLOY_COLOR[text.toUpperCase()]">{{ text }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'deployClusters'">
          <a-tag>{{ text }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'registerState'">
          <a-tag :color="INSTANCE_REGISTER_COLOR[text.toUpperCase()]">{{ text }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'registerCluster'">
          <a-tag>{{ text }}</a-tag>
        </template>
        <template v-if="column.dataIndex === 'labels'">
          <a-tag :color="PRIMARY_COLOR" v-for="(value, key) in text">{{ key }} : {{ value }}</a-tag>
        </template>

        <template v-if="column.dataIndex === 'registerTime'">
          {{ formattedDate(text) }}
        </template>
      </template>
    </search-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, provide, reactive } from 'vue'
import { INSTANCE_DEPLOY_COLOR, INSTANCE_REGISTER_COLOR, PRIMARY_COLOR } from '@/base/constants'
import { Icon } from '@iconify/vue'
import SearchTable from '@/components/SearchTable.vue'
import { SearchDomain } from '@/utils/SearchUtil'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { useRoute, useRouter } from 'vue-router'
import { getApplicationInstanceInfo } from '@/api/service/app'
import { formattedDate } from '@/utils/DateUtil'
import { queryMetrics } from '@/base/http/promQuery'
import { isNumber } from 'lodash'
import { bytesToHuman } from '@/utils/ByteUtil'
import { promQueryList } from '@/utils/PromQueryUtil'

const route = useRoute()
const router = useRouter()

let __null = PRIMARY_COLOR
let statisticsInfo = reactive({
  info: <{ [key: string]: string }>{},
  report: <{ [key: string]: { value: string; icon: string } }>{}
})
let appNameParam: any = route.params?.pathId

onMounted(async () => {
  // let statistics = (await getApplicationInstanceStatistics({})).data
  // statisticsInfo.info = <{ [key: string]: string }>statistics
  // statisticsInfo.report = {
  //   providers: {
  //     icon: 'carbon:branch',
  //     value: statisticsInfo.info.instanceTotal
  //   },
  //   consumers: {
  //     icon: 'mdi:merge',
  //     value: statisticsInfo.info.versionTotal
  //   },
  //   cpu: {
  //     icon: 'carbon:branch',
  //     value: statisticsInfo.info.cpuTotal
  //   },
  //   memory: {
  //     icon: 'mdi:merge',
  //     value: statisticsInfo.info.memoryTotal
  //   }
  // }
})

const columns = [
  {
    title: 'instanceDomain.ip',
    dataIndex: 'ip',
    key: 'ip',
    sorter: true,
    width: 150,
    fixed: 'left'
  },
  {
    title: 'instanceDomain.name',
    dataIndex: 'name',
    key: 'name',
    sorter: true,
    width: 180
  },
  {
    title: 'instanceDomain.deployState',
    dataIndex: 'deployState',
    key: 'deployState',
    sorter: true,
    width: 150
  },
  {
    title: 'instanceDomain.deployCluster',
    dataIndex: 'deployClusters',
    key: 'deployClusters',
    sorter: true,
    width: 180
  },
  {
    title: 'instanceDomain.registerState',
    dataIndex: 'registerState',
    key: 'registerState',
    sorter: true,
    width: 150
  },
  {
    title: 'instanceDomain.registerClusters',
    dataIndex: 'registerCluster',
    key: 'registerCluster',
    sorter: true,
    width: 200
  },
  {
    title: 'instanceDomain.cpu',
    dataIndex: 'cpu',
    key: 'cpu',
    sorter: true,
    width: 120
  },
  {
    title: 'instanceDomain.memory',
    dataIndex: 'memory',
    key: 'memory',
    sorter: true,
    width: 120
  },
  {
    title: 'instanceDomain.startTime',
    dataIndex: 'startTime',
    key: 'startTime',
    sorter: true,
    width: 150
  }
  // {
  //   title: 'instanceDomain.registerTime',
  //   dataIndex: 'registerTime',
  //   key: 'registerTime',
  //   sorter: true,
  //   width: 150
  // },
  // {
  //   title: 'instanceDomain.labels',
  //   dataIndex: 'labels',
  //   key: 'labels',
  //   sorter: true,
  //   fixed: 'left',
  //   width: 800
  // }
]

function instanceInfo(params: any) {
  return getApplicationInstanceInfo(params).then(async (res) => {
    return promQueryList(res, ['cpu', 'memory'], async (instance: any) => {
      let ip = instance.ip.split(':')[0]
      let cpu =
        await queryMetrics(`sum(node_namespace_pod_container:container_cpu_usage_seconds_total:sum_irate{container!=""}) by (pod) * on (pod) group_left(pod_ip)
        kube_pod_info{pod_ip="${ip}"}`)
      let mem = await queryMetrics(`sum(container_memory_working_set_bytes{container!=""}) by (pod)
* on (pod) group_left(pod_ip)
kube_pod_info{pod_ip="${ip}"}`)
      instance.cpu = isNumber(cpu) ? cpu.toFixed(3) + 'u' : cpu
      instance.memory = bytesToHuman(mem)
    })
  })
}

const searchDomain = reactive(
  new SearchDomain(
    [
      {
        label: '',
        param: 'type',
        defaultValue: 1,
        dict: [
          { label: 'ip', value: 1 },
          { label: 'name', value: 2 },
          { label: 'label', value: 3 }
        ],
        style: {
          width: '100px'
        }
      },
      {
        label: '',
        param: 'search',
        style: {
          width: '300px'
        }
      },
      {
        label: '',
        param: 'appName',
        defaultValue: appNameParam,
        dict: [],
        dictType: 'APPLICATION_NAME'
      }
    ],
    instanceInfo,
    columns,
    { pageSize: 10 },
    true
  )
)

onMounted(() => {
  searchDomain.tableStyle = {
    scrollX: '100',
    scrollY: 'calc(100vh - 400px)'
  }
  searchDomain.onSearch()
})

const viewDetail = (serviceName: string) => {
  router.replace(`/resources/instances/detail/${serviceName.split(':')[0]}/${serviceName}`)
}

provide(PROVIDE_INJECT_KEY.SEARCH_DOMAIN, searchDomain)
</script>
<style lang="less" scoped>
.__container_app_instance {
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
