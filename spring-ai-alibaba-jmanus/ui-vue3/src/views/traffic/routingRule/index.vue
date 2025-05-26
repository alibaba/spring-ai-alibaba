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
  <div class="routing-rule-container">
    <search-table :search-domain="searchDomain">
      <template #customOperation>
        <a-button type="primary" @click="router.push(`/traffic/addRoutingRule/addByFormView`)"
          >新增条件路由规则
        </a-button>
      </template>
      <template #bodyCell="{ text, column, record }">
        <template v-if="column.dataIndex === 'ruleName'">
          <span class="rule-link" @click="router.push(`formview/${record[column.key]}`)">
            <b>
              <Icon style="margin-bottom: -2px" icon="material-symbols:attach-file-rounded"></Icon>
              {{ text }}
            </b>
          </span>
        </template>
        <template v-if="column.dataIndex === 'ruleGranularity'">
          {{ record.scope === 'service' ? '服务' : '应用' }}
        </template>
        <template v-if="column.dataIndex === 'enabled'">
          {{ text ? '启用' : '禁用' }}
        </template>
        <!-- 时间 -->
        <template v-if="column.dataIndex === 'createTime'">
          {{ formattedDate(record.createTime) }}
        </template>
        <template v-if="column.dataIndex === 'operation'">
          <a-button type="link" @click="router.push(`formview/${record.ruleName}`)">
            查看
          </a-button>
          <a-button
            type="link"
            @click="router.push(`/traffic/updateRoutingRule/updateByFormView/${record.ruleName}`)"
          >
            修改
          </a-button>
          <a-popconfirm
            title="确认删除该条件路由规则？"
            ok-text="Yes"
            cancel-text="No"
            @confirm="confirm(record.ruleName)"
          >
            <a-button type="link"> 删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </search-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, provide, reactive, inject } from 'vue'
import { deleteConditionRuleAPI, searchRoutingRule } from '@/api/service/traffic'
import SearchTable from '@/components/SearchTable.vue'
import { SearchDomain, sortString } from '@/utils/SearchUtil'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import router from '@/router'
import { Icon } from '@iconify/vue'
import { PRIMARY_COLOR } from '@/base/constants'
import { formattedDate } from '@/utils/DateUtil'
const TAB_STATE = inject(PROVIDE_INJECT_KEY.PROVIDE_INJECT_KEY)
let columns = [
  {
    title: 'ruleName',
    key: 'ruleName',
    dataIndex: 'ruleName',
    sorter: (a: any, b: any) => sortString(a.appName, b.appName),
    width: 140
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
    width: 120,
    sorter: (a: any, b: any) => sortString(a.instanceNum, b.instanceNum)
  },
  {
    title: 'enabled',
    key: 'enabled',
    dataIndex: 'enabled',
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
    searchRoutingRule,
    columns
  )
)

//Delete conditional routing
const deleteRule = async (ruleName: string) => {
  const res = await deleteConditionRuleAPI(ruleName)
  if (res.code === 200) {
    await searchDomain.onSearch()
  }
}

onMounted(() => {
  TAB_STATE.conditionRule = null
  TAB_STATE.addConditionRuleSate = null
  searchDomain.onSearch()
  searchDomain.tableStyle = {
    scrollX: '100',
    scrollY: '367px'
  }
})

const confirm = (ruleName) => {
  deleteRule(ruleName)
}

provide(PROVIDE_INJECT_KEY.SEARCH_DOMAIN, searchDomain)
</script>
<style lang="less" scoped>
.routing-rule-container {
  height: 100%;
  .search-table-container {
    height: 100%;
    //min-height: 60vh;
    //max-height: 70vh; //overflow: auto;

    .rule-link {
      padding: 4px 10px 4px 4px;
      border-radius: 4px;
      color: v-bind('PRIMARY_COLOR');

      &:hover {
        cursor: pointer;
        background: rgba(133, 131, 131, 0.13);
      }
    }
  }
}
</style>
