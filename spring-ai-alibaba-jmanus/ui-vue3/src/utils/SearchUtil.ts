/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { TableColumnsType } from 'ant-design-vue'
import { reactive } from 'vue'

export type DICT_TYPE = 'SELECT' | 'BUTTON' | 'RADIO'

export class SearchDomain {
  // form of search
  noPaged?: boolean
  queryForm: any
  params: [
    {
      label: string
      param: string
      defaultValue: string
      style?: any
      dict: [
        {
          label: string
          value: string
        }
      ]
      dictType: DICT_TYPE
    }
  ]
  searchApi: Function
  result: any
  handleResult?: Function
  tableStyle: any
  table: {
    loading?: boolean
    columns: (TableColumnsType & { __hide: boolean }) | any
  } = { columns: [] }
  paged = {
    curPage: 1,
    pageOffset: '0',
    total: 0,
    pageSize: 10
  }

  constructor(
    query: any,
    searchApi: any,
    columns: TableColumnsType | any,
    paged?: any | undefined,
    noPaged?: boolean,
    handleResult?: Function
  ) {
    this.params = query
    this.noPaged = noPaged
    this.queryForm = reactive({})
    this.table.columns = columns
    query.forEach((c: any) => {
      if (c.defaultValue) {
        this.queryForm[c.param] = c.defaultValue
      }
    })
    if (paged) {
      this.paged = { ...this.paged, ...paged }
    }
    this.searchApi = searchApi
    this.handleResult = handleResult
  }

  async onSearch(handleResult?: Function) {
    if (handleResult) {
      this.handleResult = handleResult
    }
    this.table.loading = true
    setTimeout(() => {
      this.table.loading = false
    }, 10000)
    const queryParams = {
      ...this.queryForm,
      ...(this.noPaged
        ? {}
        : {
            pageSize: this.paged.pageSize,
            pageOffset: (this.paged.curPage - 1) * this.paged.pageSize
          })
    }

    this.searchApi(queryParams)
      .then((res: any) => {
        const {
          data: { list, pageInfo }
        } = res
        this.result = handleResult ? handleResult(list) : list

        if (!this.noPaged) {
          this.paged.total = pageInfo?.Total || 0
        }
      })
      .catch((error: any) => {
        console.error('Error fetching data:', error)
      })
      .finally(() => {
        this.table.loading = false
      })
  }
}

export function sortString(a: any, b: any) {
  if (!isNaN(a - b)) {
    return a - b
  }
  return a.localeCompare(b)
}
