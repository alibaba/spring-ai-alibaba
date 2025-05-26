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
  <div class="__container_search_table">
    <div class="search-query-container">
      <a-row>
        <a-col :span="18">
          <a-form @keyup.enter="searchDomain.onSearch()">
            <a-flex wrap="wrap" gap="large">
              <template v-for="q in searchDomain.params">
                <a-form-item :label="$t(q.label)">
                  <template v-if="q.dict && q.dict.length > 0">
                    <a-radio-group
                      button-style="solid"
                      v-model:value="searchDomain.queryForm[q.param]"
                      v-if="q.dictType === 'BUTTON'"
                    >
                      <a-radio-button v-for="item in q.dict" :value="item.value">
                        {{ $t(item.label) }}
                      </a-radio-button>
                    </a-radio-group>
                    <a-select
                      v-else
                      class="select-type"
                      :style="q.style"
                      v-model:value="searchDomain.queryForm[q.param]"
                    >
                      <a-select-option
                        :value="item.value"
                        v-for="item in [...q.dict, { label: 'none', value: '' }]"
                      >
                        {{ $t(item.label) }}
                      </a-select-option>
                    </a-select>
                  </template>

                  <a-input
                    v-else
                    :style="q.style"
                    :placeholder="$t('placeholder.' + (q.placeholder || `typeDefault`))"
                    v-model:value="searchDomain.queryForm[q.param]"
                  ></a-input>
                </a-form-item>
              </template>
              <a-form-item :label="''">
                <a-button type="primary" @click="searchDomain.onSearch()">
                  <Icon
                    style="margin-bottom: -2px; font-size: 1.3rem"
                    icon="ic:outline-manage-search"
                  ></Icon>
                </a-button>
              </a-form-item>
            </a-flex>
          </a-form>
        </a-col>
        <a-col :span="6">
          <a-flex style="justify-content: flex-end">
            <slot name="customOperation"></slot>
            <div class="common-tool" @click="commonTool.customColumns = !commonTool.customColumns">
              <div class="custom-column button">
                <Icon icon="material-symbols-light:format-list-bulleted-rounded"></Icon>
              </div>
              <div class="dropdown" v-show="commonTool.customColumns">
                <a-card style="max-width: 300px" title="Custom Column">
                  <div class="body">
                    <div
                      class="item"
                      @click.stop="hideColumn(item)"
                      v-for="(item, i) in searchDomain?.table.columns"
                    >
                      <Icon
                        style="margin-bottom: -4px; font-size: 1rem; margin-right: 2px"
                        :icon="item.__hide ? 'zondicons:view-hide' : 'zondicons:view-show'"
                      ></Icon>
                      {{ item.title }}
                    </div>
                  </div>
                </a-card>
              </div>
            </div>
          </a-flex>
        </a-col>
      </a-row>
    </div>
    <div class="search-table-container">
      <a-table
        :loading="searchDomain.table.loading"
        :pagination="pagination"
        :scroll="{
          scrollToFirstRowOnChange: true,
          y: searchDomain.tableStyle?.scrollY || '',
          x: searchDomain.tableStyle?.scrollX || ''
        }"
        :columns="searchDomain?.table.columns.filter((x: any) => !x.__hide)"
        :data-source="searchDomain?.result"
        @change="handleTableChange"
      >
        <template #bodyCell="{ text, record, index, column }">
          <span v-if="column.key === 'idx'">{{ index + 1 }}</span>
          <span v-if="text === 'skeleton-loading'">
            <a-skeleton-button active size="small"></a-skeleton-button>
          </span>
          <slot
            name="bodyCell"
            :text="text"
            :record="record"
            :index="index"
            :column="column"
            v-else
          >
          </slot>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ComponentInternalInstance } from 'vue'
import { computed, getCurrentInstance, inject, reactive } from 'vue'

import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import type { SearchDomain } from '@/utils/SearchUtil'
import { Icon } from '@iconify/vue'
import { PRIMARY_COLOR } from '@/base/constants'
import { message } from 'ant-design-vue'

const commonTool = reactive({
  customColumns: false
})
let __ = PRIMARY_COLOR

const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()

const searchDomain: SearchDomain | any = inject(PROVIDE_INJECT_KEY.SEARCH_DOMAIN)

searchDomain.table.columns.forEach((column: any) => {
  if (column.title) {
    const tmp = column.title
    column.title = computed(() => globalProperties.$t(tmp))
  }
})
const pagination: any = computed(() => {
  if (searchDomain.noPaged) {
    return false
  }
  return {
    pageSize: searchDomain.paged.pageSize,
    current: searchDomain.paged.curPage,
    total: searchDomain.paged.total,
    showTotal: (v: any) =>
      globalProperties.$t('searchDomain.total') +
      ': ' +
      v +
      ' ' +
      globalProperties.$t('searchDomain.unit')
  }
})

const handleTableChange = (
  pag: { pageSize: number; current: number },
  filters: any,
  sorter: any
) => {
  searchDomain.paged.pageSize = pag.pageSize
  searchDomain.paged.curPage = pag.current

  searchDomain.onSearch()

  return
}

function hideColumn(item: any) {
  let filter = searchDomain?.table.columns.filter((x: any) => !x.__hide)
  if (!item.__hide && filter.length <= 1) {
    message.warn('must show at least one column')
    return
  }
  item.__hide = !item.__hide
}
</script>
<style lang="less" scoped>
.__container_search_table {
  .search-query-container {
    border-radius: 10px 10px 0 0;
    border-bottom: 1px solid rgba(220, 219, 219, 0.29);
    background: #fafafa;
    padding: 20px 20px 0 20px;
    //margin-bottom: 20px;
  }

  .select-type {
    width: 200px;
  }

  :deep(.ant-pagination) {
    padding: 0 10px 20px 0;
  }

  :deep(.ant-spin-container) {
    background: #fafafa;
  }

  .common-tool {
    margin-top: 5px;
    width: 100px;
    cursor: pointer;
    position: relative;

    .button {
      vertical-align: center;
      line-height: 24px;
      font-size: 24px;
      float: right;

      &:hover {
        color: v-bind('PRIMARY_COLOR');
      }

      svg {
        margin-left: 10px;
      }
    }

    .dropdown {
      top: 40px;
      right: -40px;
      position: absolute;
      height: auto;
      z-index: 1000;

      .body {
        max-height: 200px;
        overflow: auto;
      }

      .item {
        line-height: 30px;

        &:hover {
          color: v-bind('PRIMARY_COLOR');
        }
      }
    }
  }
}
</style>
<style lang="less"></style>
