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
  <div class="__container_app_detail">
    <a-flex>
      <a-card-grid>
        <a-row :gutter="10">
          <a-col :span="12">
            <a-card class="_detail">
              <a-descriptions class="description-column" :column="1">
                <a-descriptions-item
                  v-for="(v, key, idx) in detailMap.left"
                  v-show="!!v"
                  :labelStyle="{ fontWeight: 'bold', width: '100px' }"
                  :label="$t('applicationDomain.' + key)"
                >
                  {{ typeof v === 'object' ? v[0] : v }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card class="_detail">
              <a-descriptions class="description-column" :column="1">
                <a-descriptions-item
                  v-for="(v, key, idx) in detailMap.right"
                  v-show="!!v"
                  :labelStyle="{ fontWeight: 'bold', width: '100px' }"
                  :label="$t('applicationDomain.' + key)"
                >
                  {{ v[0] }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>
        </a-row>

        <a-card style="margin-top: 10px" class="_detail">
          <a-descriptions class="description-column" :column="1">
            <a-descriptions-item
              v-for="(v, key, idx) in detailMap.bottom"
              v-show="!!v"
              :labelStyle="{ fontWeight: 'bold' }"
              :label="$t('applicationDomain.' + key)"
            >
              <template v-if="v?.length < 3">
                <p v-for="item in v" class="description-item-content no-card">
                  {{ item }}
                </p>
              </template>

              <a-card class="description-item-card" v-else>
                <p
                  v-for="item in v"
                  @click="copyIt(item.toString())"
                  class="description-item-content with-card"
                >
                  {{ item }}
                  <CopyOutlined />
                </p>
              </a-card>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-card-grid>
    </a-flex>
  </div>
</template>

<script setup lang="ts">
import { PRIMARY_COLOR, PRIMARY_COLOR_T } from '@/base/constants'
import { getApplicationDetail } from '@/api/service/app'
import { computed, onMounted, reactive, getCurrentInstance } from 'vue'
import { CopyOutlined } from '@ant-design/icons-vue'
import useClipboard from 'vue-clipboard3'
import { message } from 'ant-design-vue'
import type { ComponentInternalInstance } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const apiData: any = reactive({})

const {
  appContext: {
    config: { globalProperties }
  }
} = <ComponentInternalInstance>getCurrentInstance()

let __ = PRIMARY_COLOR
let PRIMARY_COLOR_20 = PRIMARY_COLOR_T('20')

let detailMap = reactive({
  left: {},
  right: {},
  bottom: {}
})

onMounted(async () => {
  let appNameParam: any = route.params?.pathId
  apiData.detail = await getApplicationDetail({ appName: appNameParam })
  console.log(apiData.detail)
  let {
    appName,
    rpcProtocols,
    dubboVersions,
    dubboPorts,
    serialProtocols,
    appTypes,
    images,
    workloads,
    deployClusters,
    registerClusters,
    registerModes
  } = apiData.detail.data
  detailMap.left = {
    appName,
    appTypes,
    serialProtocols
  }
  detailMap.right = {
    rpcProtocols,
    dubboPorts,
    dubboVersions
  }
  detailMap.bottom = {
    images,
    workloads,
    deployClusters,
    registerClusters,
    registerModes
  }
  console.log(appName)
})
const toClipboard = useClipboard().toClipboard

function copyIt(v: string) {
  message.success(globalProperties.$t('messageDomain.success.copy'))
  toClipboard(v)
}
</script>
<style lang="less" scoped>
.__container_app_detail {
}
</style>
