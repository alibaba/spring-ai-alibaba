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
  <div class="__container_traffic_config_index">
    <search-table :search-domain="searchDomain">
      <template #customOperation>
        <a-button type="primary" @click="addDynamicConfig">新增动态配置</a-button>
      </template>
      <template #bodyCell="{ text, column, record }">
        <template v-if="column.dataIndex === 'ruleName'">
          <span
            class="config-link"
            @click="router.push(`/traffic/dynamicConfig/formview/${record.ruleName}/0`)"
          >
            <b>
              <Icon style="margin-bottom: -2px" icon="material-symbols:attach-file-rounded"></Icon>
              {{ text }}
            </b>
          </span>
        </template>
        <template v-if="column.dataIndex === 'ruleGranularity'">
          {{ text ? '服务' : '应用' }}
        </template>
        <template v-if="column.dataIndex === 'enabled'">
          {{ text ? '启用' : '禁用' }}
        </template>
        <template v-if="column.dataIndex === 'operation'">
          <a-button
            type="link"
            @click="router.push(`/traffic/dynamicConfig/formview/${record.ruleName}/0`)"
            >查看</a-button
          >
          <a-button
            type="link"
            @click="router.push(`/traffic/dynamicConfig/formview/${record.ruleName}/1`)"
          >
            修改
          </a-button>
          <a-popconfirm
            title="确认删除该动态配置？"
            ok-text="Yes"
            cancel-text="No"
            @confirm="delDynamicConfig(record)"
          >
            <a-button type="link">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </search-table>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, provide, reactive } from 'vue'
import { delConfiguratorDetail, searchDynamicConfig } from '@/api/service/traffic'
import SearchTable from '@/components/SearchTable.vue'
import { SearchDomain, sortString } from '@/utils/SearchUtil'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { useRouter } from 'vue-router'
import { PRIMARY_COLOR } from '@/base/constants'
import { Icon } from '@iconify/vue'

const router = useRouter()

let __null = PRIMARY_COLOR
const TAB_STATE = inject(PROVIDE_INJECT_KEY.PROVIDE_INJECT_KEY)
TAB_STATE.dynamicConfigForm = reactive({})
let columns = [
  {
    title: 'ruleName',
    key: 'ruleName',
    dataIndex: 'ruleName',
    sorter: (a: any, b: any) => sortString(a.appName, b.appName),
    width: 200,
    ellipsis: true
  },
  {
    title: 'ruleGranularity',
    key: 'ruleGranularity',
    dataIndex: 'ruleGranularity',
    render: (text, record) => (record.isService ? '服务' : '应用'),
    width: 100,
    sorter: (a: any, b: any) => sortString(a.instanceNum, b.instanceNum)
  },
  {
    title: 'createTime',
    key: 'createTime',
    dataIndex: 'createTime',
    width: 200,
    sorter: (a: any, b: any) => sortString(a.instanceNum, b.instanceNum)
  },
  {
    title: 'enabled',
    key: 'enabled',
    dataIndex: 'enabled',
    render: (text, record) => (record.enabled ? '是' : '否'),
    width: 120,
    sorter: (a: any, b: any) => sortString(a.instanceNum, b.instanceNum)
  },
  {
    title: 'operation',
    key: 'operation',
    dataIndex: 'operation',
    width: 200
  }
]
const searchDomain = reactive(
  new SearchDomain(
    [
      {
        label: 'serviceGovernance',
        param: 'serviceGovernance',
        placeholder: 'typeRoutingRules',
        style: {
          width: '200px'
        }
      }
    ],
    searchDynamicConfig,
    columns
  )
)
const addDynamicConfig = () => {
  router.push(`/traffic/dynamicConfig/formview/_tmp/1`)
}

onMounted(async () => {
  await searchDomain.onSearch()
})

const delDynamicConfig = async (record: any) => {
  await delConfiguratorDetail({ name: record.ruleName })
  await searchDomain.onSearch()
}

provide(PROVIDE_INJECT_KEY.SEARCH_DOMAIN, searchDomain)
</script>
<style lang="less" scoped>
.__container_traffic_config_index {
  min-height: 60vh;
  .config-link {
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
