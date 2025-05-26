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
  <div class="__container_services_index">
    <search-table :search-domain="searchDomain">
      <template #bodyCell="{ column, record, text, index: tableRowIndex }">
        <template v-if="column.dataIndex === 'serviceName'">
          {{ record.versionGroup }}
          <span class="service-link" @click="viewDistribution(text, tableRowIndex)">
            <b>
              <Icon style="margin-bottom: -2px" icon="material-symbols:attach-file-rounded"></Icon>
              {{ text }}
            </b>
          </span>
        </template>

        <template v-else-if="column.dataIndex === 'versionGroupSelect'">
          <a-select :value="text?.versionGroupValue" :bordered="false" style="width: 80%">
            <a-select-option
              v-for="(item, index) in text?.versionGroupArr"
              :value="item"
              @click="selectedVersionAndGroup(tableRowIndex, index, item)"
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
import { useRoute, useRouter } from 'vue-router'
import { nextTick, provide, reactive, ref, watch } from 'vue'
import { searchService } from '@/api/service/service'
import { SearchDomain } from '@/utils/SearchUtil'
import SearchTable from '@/components/SearchTable.vue'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { PRIMARY_COLOR } from '@/base/constants'
import { Icon } from '@iconify/vue'
import { queryMetrics } from '@/base/http/promQuery'
import { promQueryList } from '@/utils/PromQueryUtil'

let __null = PRIMARY_COLOR
const router = useRouter()
const route = useRoute()
let query = route.query['query']
const columns = [
  {
    title: 'service',
    key: 'service',
    dataIndex: 'serviceName',
    sorter: true,
    width: '30%',
    ellipsis: true
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

const tempServiceList = ref([])

const handleResult = (result: any) => {
  return result.map((service: any) => {
    service.versionGroupSelect = {}
    service.versionGroupSelect.versionGroupArr = service.versionGroups.map((item: any) => {
      return (item.versionGroup =
        (item.version ? 'version: ' + item.version + ', ' : '') +
          (item.group ? 'group: ' + item.group : '') || '无')
    })
    service.versionGroupSelect.versionGroupValue = service.versionGroupSelect.versionGroupArr[0]
    return service
  })
}

function serviceInfo(params: any, table: any) {
  return searchService(params).then(async (res) => {
    tempServiceList.value = res.data?.list
    tempServiceList.value.forEach((service: any) => {
      service.selectedIndex = -1
    })

    console.log(tempServiceList.value)
    return promQueryList(res, ['avgQPS', 'avgRT', 'requestTotal'], async (service: any) => {
      service.avgQPS = await queryMetrics(
        `sum (dubbo_provider_qps_total{interface='${service.serviceName}'}) by (interface)`
      )
      service.avgRT = await queryMetrics(
        `avg(dubbo_consumer_rt_avg_milliseconds_aggregate{interface="${service.serviceName}",method=~"$method"}>0)`
      )
      service.requestTotal = await queryMetrics(
        `sum (increase(dubbo_provider_requests_total{interface="${service.serviceName}"}[1m]))`
      )
    })
  })
}

const searchDomain = reactive(
  new SearchDomain(
    [
      {
        label: 'serviceName',
        param: 'keywords',
        placeholder: 'typeAppName',
        defaultValue: query,
        style: {
          width: '200px'
        }
      }
    ],
    serviceInfo,
    columns,
    undefined,
    undefined,
    handleResult
  )
)

searchDomain.onSearch(handleResult)
searchDomain.tableStyle = {
  scrollX: '100',
  scrollY: '367px'
}

const selectedVersionAndGroup = (
  tableRowIndex: number,
  versionAndGroupIndex: number,
  versionAndGroupText: string
) => {
  if (versionAndGroupText === '无') {
    tempServiceList.value[tableRowIndex].selectedIndex = -1
  } else {
    tempServiceList.value[tableRowIndex].selectedIndex = versionAndGroupIndex
  }
}

const viewDistribution = (serviceName: string, tableRowIndex: number) => {
  const selectedIndex = tempServiceList.value[tableRowIndex]?.selectedIndex
  const group = tempServiceList.value[tableRowIndex].versionGroups[selectedIndex]?.group || ''
  const version = tempServiceList.value[tableRowIndex].versionGroups[selectedIndex]?.version || ''
  router.push({ name: 'distribution', params: { pathId: serviceName, group, version } })
}

provide(PROVIDE_INJECT_KEY.SEARCH_DOMAIN, searchDomain)
watch(route, (a, b) => {
  searchDomain.queryForm['keywords'] = a.query['query']
  searchDomain.onSearch()
  console.log(a)
})
</script>
<style lang="less" scoped>
.__container_services_index {
  .service-link {
    padding: 4px 10px 4px 4px;
    border-radius: 4px;
    color: v-bind('PRIMARY_COLOR');

    &:hover {
      cursor: pointer;
      background: rgba(133, 131, 131, 0.13);
    }
  }
}
</style>
