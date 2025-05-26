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
  <div class="__container_resources_application_index">
    <search-table :search-domain="searchDomain">
      <template #customOperation>
        <a-button type="primary">新增路由规则</a-button>
      </template>
      <template #bodyCell="{ text, column, record }">
        <template v-if="column.dataIndex === 'ruleName'">
          <a-button type="link" @click="router.replace(`formview/${record[column.key]}`)">{{
            text
          }}</a-button>
        </template>
        <template v-if="column.dataIndex === 'operation'">
          <a-button type="link">查看</a-button>
          <a-button type="link">修改</a-button>
          <a-popconfirm
            title="确认删除该动态配置？"
            ok-text="Yes"
            cancel-text="No"
            @confirm="confirm"
          >
            <a-button type="link">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </search-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, provide, reactive } from 'vue'
import { searchVirtualService } from '@/api/service/traffic'
import SearchTable from '@/components/SearchTable.vue'
import { SearchDomain, sortString } from '@/utils/SearchUtil'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import router from '@/router'

let columns = [
  {
    title: 'ruleName',
    key: 'ruleName',
    dataIndex: 'ruleName',
    sorter: (a: any, b: any) => sortString(a.appName, b.appName),
    width: 140
  },
  {
    title: 'createTime',
    key: 'createTime',
    dataIndex: 'createTime',
    width: 120,
    sorter: (a: any, b: any) => sortString(a.instanceNum, b.instanceNum)
  },
  {
    title: 'lastModifiedTime',
    key: 'lastModifiedTime',
    dataIndex: 'lastModifiedTime',
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
    searchVirtualService,
    columns
  )
)

onMounted(() => {
  searchDomain.onSearch()
})

const confirm = () => {}

provide(PROVIDE_INJECT_KEY.SEARCH_DOMAIN, searchDomain)
</script>
<style lang="less" scoped>
.search-table-container {
  min-height: 60vh;
  //max-height: 70vh; //overflow: auto;
}
</style>
