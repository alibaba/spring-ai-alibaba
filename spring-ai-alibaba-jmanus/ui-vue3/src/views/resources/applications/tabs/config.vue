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
  <div class="__container_app_config">
    <config-page :options="options">
      <template v-slot:form_log="{ current }">
        <a-form-item :label="$t('applicationDomain.operatorLog')" name="logFlag">
          <a-switch v-model:checked="current.form.logFlag"></a-switch>
        </a-form-item>
      </template>
      <template v-slot:form_flow="{ current }">
        <a-space direction="vertical" size="middle" class="flowWeight-box">
          <a-card :id="'flowWeight' + i" v-for="(item, i) in current.form.rules">
            <template #title>
              {{ $t('applicationDomain.flowWeight') }} {{ i + 1 }}
              <div style="float: right">
                <a-space>
                  <a-button danger type="dashed" @click="deleteFlowWeight(i)">
                    <Icon style="font-size: 20px" icon="fluent:delete-12-filled"></Icon>
                  </a-button>
                </a-space>
              </div>
            </template>

            <a-form-item :name="'rules[' + i + '].weight'" label="权重">
              <a-input-number min="1" v-model:value="item.weight"></a-input-number>
            </a-form-item>

            <a-form-item label="作用范围">
              <a-button type="primary" @click="addScopeItem(i)"> 添加</a-button>
              <a-table
                style="width: 40vw"
                :pagination="false"
                :columns="scopeColumns"
                :data-source="item.scope"
              >
                <template #bodyCell="{ column, record, index: scopeItemIndex }">
                  <template v-if="column.key === 'key'">
                    <a-form-item :name="'rules[' + i + '].scope.key'">
                      <a-input v-model:value="record.key"></a-input>
                    </a-form-item>
                  </template>
                  <template v-if="column.key === 'condition'">
                    <a-form-item :name="'rules[' + i + '].scope.condition'">
                      <a-input v-model:value="record.condition"></a-input>
                    </a-form-item>
                  </template>
                  <template v-if="column.key === 'value'">
                    <a-form-item :name="'rules[' + i + '].scope.value'">
                      <a-input v-model:value="record.value"></a-input>
                    </a-form-item>
                  </template>

                  <template v-if="column.key === 'operation'">
                    <a-form-item :name="'rules[' + i + '].scope.operation'">
                      <a-button type="link" @click="deleteScopeItem(i, scopeItemIndex)">
                        删除</a-button
                      >
                    </a-form-item>
                  </template>
                </template>
              </a-table>
            </a-form-item>
          </a-card>
        </a-space>
      </template>
      <template v-slot:form_gray="{ current }">
        <a-space direction="vertical" size="middle">
          <a-card v-for="(item, i) in current.form.rules">
            <template #title>
              {{ $t('applicationDomain.gray') }} {{ i + 1 }}
              <div style="float: right">
                <a-space>
                  <a-button danger type="dashed" @click="deleteGrayIsolation(i)">
                    <Icon style="font-size: 20px" icon="fluent:delete-12-filled"></Icon>
                  </a-button>
                </a-space>
              </div>
            </template>

            <a-form-item :name="'rules[' + i + '].name'" label="环境名称">
              <a-input v-model:value="item.name"></a-input>
            </a-form-item>
            <a-form-item label="作用范围">
              <a-space direction="vertical" size="middle">
                <a-button type="primary" @click="addGrayScopeItem(i)"> 添加</a-button>
                <a-table
                  style="width: 40vw"
                  :pagination="false"
                  :columns="grayTableColumns"
                  :data-source="item.scope"
                >
                  <template #bodyCell="{ column, record, index }">
                    <template v-if="column.key === 'label'">
                      <a-form-item :name="'rules[' + i + '].scope.key'">
                        <a-input v-model:value="record.label"></a-input>
                      </a-form-item>
                    </template>
                    <template v-if="column.key === 'condition'">
                      <a-form-item :name="'rules[' + i + '].scope.condition'">
                        <a-input v-model:value="record.condition"></a-input>
                      </a-form-item>
                    </template>
                    <template v-if="column.key === 'value'">
                      <a-form-item :name="'rules[' + i + '].scope.value'">
                        <a-input v-model:value="record.value"></a-input>
                      </a-form-item>
                    </template>
                    <template v-if="column.key === 'operation'">
                      <a-form-item :name="'rules[' + i + '].scope.operation'">
                        <a-button type="link" @click="deleteGrayScopeItem(i, index)">
                          删除</a-button
                        >
                      </a-form-item>
                    </template>
                  </template>
                </a-table>
              </a-space>
            </a-form-item>
          </a-card>
        </a-space>
      </template>
    </config-page>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import ConfigPage from '@/components/ConfigPage.vue'
import { Icon } from '@iconify/vue'
import {
  getAppGrayIsolation,
  getAppLogSwitch,
  getAppTrafficWeight,
  updateAppGrayIsolation,
  updateAppLogSwitch,
  updateAppTrafficWeight
} from '@/api/service/app'
import { useRoute } from 'vue-router'
import { scrollIntoView } from '@/utils/UIUtil'

const route = useRoute()

const scopeColumns = [
  { key: 'key', title: 'label' },
  { key: 'condition', title: 'condition' },
  { key: 'value', title: 'value' },
  { key: 'operation', title: '操作' }
]

let options: any = reactive({
  list: [
    {
      title: 'applicationDomain.operatorLog',
      key: 'log',
      form: {
        logFlag: false
      },
      submit: (form: any) => {
        return new Promise((resolve) => {
          resolve(updateLogFlag(form?.logFlag))
        })
      },
      reset(form: any) {
        form.logFlag = false
      }
    },
    {
      title: 'applicationDomain.flowWeight',
      key: 'flow',
      ext: {
        title: '添加权重配置',
        fun(rules: any) {
          addFlowWeight()
          // 使用 nextTick 确保 DOM 已更新
          nextTick(() => {
            const index =
              options.list.find((item: any) => item.key === 'flow')?.form.rules.length - 1
            if (index >= 0) {
              scrollIntoView('flowWeight' + index)
            }
          })
        }
      },
      form: {
        rules: [
          {
            weight: 10,
            scope: []
          }
        ]
      },
      submit(form: {}) {
        return new Promise((resolve) => {
          resolve(updateFlowWeight())
        })
      },
      reset() {
        getFlowWeight()
      }
    },

    {
      title: 'applicationDomain.gray',
      key: 'gray',
      ext: {
        title: '添加灰度环境',
        fun() {
          addGrayIsolation()
        }
      },
      form: {
        rules: [
          {
            name: 'env-nam',
            scope: {
              key: 'env',
              value: {
                exact: 'gray'
              }
            }
          }
        ]
      },
      submit(form: {}) {
        return new Promise((resolve) => {
          resolve(updateGrayIsolation())
        })
      },
      reset() {
        getGrayIsolation()
      }
    }
  ],
  current: [0]
})

// Is execution log acquisition enabled?
const getLogFlag = async () => {
  const res = await getAppLogSwitch(<string>route.params?.pathId)
  console.log(res)
  if (res?.code == 200) {
    options.list.forEach((item: any) => {
      if (item.key === 'log') {
        item.form.logFlag = res.data.operatorLog
        return
      }
    })
  }
}

// Modify the execution log switch
const updateLogFlag = async (operatorLog: boolean) => {
  const res = await updateAppLogSwitch(<string>route.params?.pathId, operatorLog)
  console.log(res)
  if (res?.code == 200) {
    await getLogFlag()
  }
}

// Obtain flow weight
const getFlowWeight = async () => {
  const res = await getAppTrafficWeight(<string>route.params?.pathId)
  if (res?.code == 200) {
    options.list.forEach((item: any) => {
      if (item.key === 'flow') {
        item.form.rules = JSON.parse(JSON.stringify(res.data.flowWeightSets))
        //Separate the traffic weight data sent by the backend.
        item.form.rules.forEach((weight: any) => {
          weight.scope.forEach((scopeItem: any) => {
            scopeItem.label = scopeItem.key
            scopeItem.condition = scopeItem.value ? Object.keys(scopeItem.value)[0] : ''
            scopeItem.value = scopeItem.value ? Object.values(scopeItem.value)[0] : ''
          })
        })
      }
    })
  }
}

// Modify flow weight
const updateFlowWeight = async () => {
  let flowWeightSets: any = []
  options.list.forEach((item: any) => {
    if (item.key === 'flow') {
      item.form.rules.forEach((weight: any) => {
        let weightItemTem = {
          weight: weight.weight,
          scope: []
        }
        weight.scope.forEach((scopeItem: any) => {
          const { key, value, condition } = scopeItem
          let scopeItemTem = {
            key: scopeItem.label || key,
            value: {}
          }
          if (condition) {
            scopeItemTem.value[condition] = value
          }
          weightItemTem.scope.push(scopeItemTem)
        })
        flowWeightSets.push(weightItemTem)
      })
    }
  })
  const res = await updateAppTrafficWeight(<string>route.params?.pathId, flowWeightSets)
  if (res.code === 200) {
    await getFlowWeight()
  }
}

// add weight
const addFlowWeight = () => {
  options.list.forEach((item: any) => {
    if (item.key === 'flow') {
      item.form.rules.push({
        weight: 10,
        scope: [
          {
            key: '',
            condition: '',
            value: ''
          }
        ]
      })
    }
  })
}

// delete weightItem
const deleteFlowWeight = (index: number) => {
  options.list.forEach((item: any) => {
    if (item.key === 'flow') {
      item.form.rules.splice(index, 1)
    }
  })
}

// add scopeItem
const addScopeItem = (index: number) => {
  options.list.forEach((item: any) => {
    if (item.key === 'flow') {
      let newData = {
        key: '',
        condition: '',
        value: ''
      }
      // console.log("lhg",item.form.rules)
      item.form.rules[index].scope.push(newData)
      return
    }
  })
}

const deleteScopeItem = (weightIndex: number, scopeItemIndex: number) => {
  options.list.forEach((item: any) => {
    if (item.key === 'flow') {
      item.form.rules[weightIndex].scope.splice(scopeItemIndex, 1)
    }
  })
}

const grayTableColumns = [
  { key: 'label', title: 'label', dataIndex: 'label' },
  { key: 'condition', title: 'condition', dataIndex: 'condition' },
  { key: 'value', title: 'value', dataIndex: 'value' },
  { key: 'operation', title: 'operation', dataIndex: 'operation' }
]

// Obtain GrayIsolation
const getGrayIsolation = async () => {
  const res = await getAppGrayIsolation(<string>route.params?.pathId)
  if (res?.code == 200) {
    options.list.forEach((item: any) => {
      if (item.key === 'gray') {
        const graySets = res.data.graySets
        if (graySets.length > 0) {
          graySets.forEach((grayItem: any) => {
            grayItem.scope.forEach((scopeItem: any) => {
              scopeItem.label = scopeItem.key
              scopeItem.condition = scopeItem.value ? Object.keys(scopeItem.value)[0] : ''
              scopeItem.value = scopeItem.value ? Object.values(scopeItem.value)[0] : ''
            })
          })
        }
        item.form.rules = graySets
      }
    })
  }
}

// Modify GrayIsolation
const updateGrayIsolation = async () => {
  let graySets: any = []
  options.list.forEach((item: any) => {
    if (item.key === 'gray') {
      item.form.rules.forEach((grayItem: any) => {
        let grayItemTem = {
          name: grayItem.name,
          scope: []
        }
        grayItem.scope.forEach((scopeItem: any) => {
          const { key, value, condition } = scopeItem
          let scopeItemTem = {
            key: scopeItem.label,
            value: {}
          }
          if (condition) {
            scopeItemTem.value[condition] = value
          }
          grayItemTem.scope.push(scopeItemTem)
        })
        graySets.push(grayItemTem)
      })
    }
  })
  const res = await updateAppGrayIsolation(<string>route.params?.pathId, graySets)
  if (res.code === 200) {
    await getGrayIsolation()
  }
}

// add GrayIsolation
const addGrayIsolation = () => {
  options.list.forEach((item: any) => {
    if (item.key === 'gray') {
      item.form.rules.push({
        name: '',
        scope: [
          {
            key: '',
            condition: '',
            value: ''
          }
        ]
      })
    }
  })
}

const addGrayScopeItem = (index: number) => {
  options.list.forEach((item: any) => {
    if (item.key === 'gray') {
      let newData = {
        key: '',
        condition: '',
        value: ''
      }
      // console.log("lhg",item.form.rules)
      item.form.rules[index].scope.push(newData)
      return
    }
  })
}
// delete GrayIsolationItem
const deleteGrayScopeItem = (weightIndex: number, scopeItemIndex: number) => {
  options.list.forEach((item: any) => {
    if (item.key === 'gray') {
      item.form.rules[weightIndex].scope.splice(scopeItemIndex, 1)
    }
  })
}

// delete GrayIsolation
const deleteGrayIsolation = (index: number) => {
  options.list.forEach((item: any) => {
    if (item.key === 'gray') {
      item.form.rules.splice(index, 1)
    }
  })
}

onMounted(() => {
  getLogFlag()
  getFlowWeight()
  getGrayIsolation()
})
</script>
<style lang="less" scoped>
.__container_app_config {
  .flowWeight-box {
    width: 100%;
    height: 100%;
    //overflow: scroll;
  }
}
</style>
