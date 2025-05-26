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
  <div class="__container_services_tabs_distribution">
    <a-flex vertical>
      <a-flex class="service-filter">
        <a-radio-group v-model:value="type" button-style="solid" @click="debounceSearch">
          <a-radio-button value="provider">生产者</a-radio-button>
          <a-radio-button value="consumer">消费者</a-radio-button>
        </a-radio-group>
        <a-input-search
          v-model:value="searchValue"
          placeholder="搜索应用，ip，支持前缀搜索"
          class="service-filter-input"
          @search="debounceSearch"
          enter-button
        />
      </a-flex>
      <a-table
        :columns="tableColumns"
        :data-source="tableData"
        :scroll="{ y: '45vh' }"
        :pagination="pagination"
        @change="onTablePageChange"
      >
        <template #bodyCell="{ column, text }">
          <template v-if="column.dataIndex === 'appName'">
            <span class="link" @click="router.push('/resources/applications/detail/' + text)">
              <b>
                <Icon
                  style="margin-bottom: -2px"
                  icon="material-symbols:attach-file-rounded"
                ></Icon>
                {{ text }}
              </b>
            </span>
          </template>

          <template v-if="column.dataIndex === 'instanceName'">
            <span class="link" @click="router.push('/resources/instances/detail/' + text)">
              <b>
                <Icon
                  style="margin-bottom: -2px"
                  icon="material-symbols:attach-file-rounded"
                ></Icon>
                {{ text }}
              </b>
            </span>
          </template>

          <template v-if="column.dataIndex === 'timeOut'">
            {{ formattedDate(text) }}
          </template>
          <template v-if="column.dataIndex === 'label'">
            <a-tag :color="PRIMARY_COLOR">{{ text }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-flex>
  </div>
</template>

<script setup lang="ts">
import type { ComponentInternalInstance } from 'vue'
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getServiceDistribution } from '@/api/service/service'
import { debounce } from 'lodash'
import { PRIMARY_COLOR } from '@/base/constants'
import { Icon } from '@iconify/vue'
import { formattedDate } from '@/utils/DateUtil'

let __null = PRIMARY_COLOR
const router = useRouter()
const route = useRoute()
const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()

const searchValue = ref('')
const versionAndGroupOptions = reactive([
  {
    label: '不指定',
    value: ''
  },
  {
    label: 'version=1.0.0',
    value: 'version=1.0.0'
  },
  {
    label: 'group=group1',
    value: 'group=group1'
  },
  {
    label: 'version=1.0.0,group=group1',
    value: 'version=1.0.0,group=group1'
  }
])
const versionAndGroup = ref(versionAndGroupOptions[0].value)
const type = ref('provider')

const tableColumns = [
  {
    title: '应用名',
    dataIndex: 'appName',
    width: '20%',
    customCell: (_, index) => {
      const currentAppName = tableData.value[index].appName
      if (index === 0 || tableData.value[index - 1].appName !== currentAppName) {
        const sameAppCount = tableData.value.filter(
          (item: any) => item.appName === currentAppName
        ).length
        return {
          rowSpan: sameAppCount
        }
      } else {
        return {
          rowSpan: 0
        }
      }
    }
  },
  {
    title: '实例数',
    dataIndex: 'instanceNum',
    width: '15%',
    customRender: ({ record }) => {
      const appName = record.appName
      const instanceNum = tableData.value.filter((item: any) => item.appName === appName).length
      return instanceNum ?? 0
    },
    customCell: (_, index) => {
      const currentAppName = tableData.value[index].appName
      if (index === 0 || tableData.value[index - 1].appName !== currentAppName) {
        const sameAppCount = tableData.value.filter(
          (item: any) => item.appName === currentAppName
        ).length
        return {
          rowSpan: sameAppCount
        }
      } else {
        return {
          rowSpan: 0
        }
      }
    }
  },
  {
    title: '实例名',
    dataIndex: 'instanceName',
    width: '25%',
    ellipsis: true
  },
  {
    title: 'RPC端口',
    dataIndex: 'rpcPort',
    width: '8%'
  },
  {
    title: '超时时间',
    dataIndex: 'timeOut',
    width: '10%'
  },
  {
    title: '重试次数',
    dataIndex: 'retryNum',
    width: '10%'
  }
  // {
  //   title: '标签',
  //   dataIndex: 'label',
  //   width: '15%'
  // }
]

const tableData = ref([])

const pagination = reactive({
  total: 0,
  pageSize: 10,
  current: 1,
  pageOffset: 0,
  showTotal: (v: any) =>
    globalProperties.$t('searchDomain.total') +
    ': ' +
    v +
    ' ' +
    globalProperties.$t('searchDomain.unit')
})

const onSearch = async () => {
  let params = {
    serviceName: route.params?.pathId,
    side: type.value,
    version: route.params?.version || '',
    group: route.params?.group || '',
    pageOffset: pagination.pageOffset,
    pageSize: pagination.pageSize
  }
  const {
    data: { list, pageInfo }
  } = await getServiceDistribution(params)
  tableData.value = list
  pagination.total = pageInfo.Total
}
onSearch()

const debounceSearch = debounce(onSearch, 300)

const onTablePageChange = (pageInfo: any) => {
  pagination.pageSize = pageInfo.pageSize || 10
  pagination.current = pageInfo.current || 1
  pagination.pageOffset = (pagination.current - 1) * pagination.pageSize
  debounceSearch()
}
</script>

<style lang="less" scoped>
.__container_services_tabs_distribution {
  .service-filter {
    margin-bottom: 20px;

    .service-filter-select {
      margin-left: 10px;
      width: 250px;
    }

    .service-filter-input {
      margin-left: 30px;
      width: 300px;
    }
  }

  .link {
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
