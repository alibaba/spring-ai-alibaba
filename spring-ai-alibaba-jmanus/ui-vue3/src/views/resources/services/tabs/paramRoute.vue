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
  <div class="__container_services_tabs_param_route">
    <a-card :bordered="false" style="width: 1000px">
      <template #title>
        <a-flex justify="space-between">
          <a-flex align="center" :gap="8">
            <span>路由{{ props?.index + 1 }}</span>
            <DeleteOutlined @click="emit('deleteParamRoute', props.index)" class="edit-icon" />
          </a-flex>
        </a-flex>
      </template>
      <a-form :labelCol="{ span: 3 }">
        <a-form-item label="选择方法">
          <a-select v-model:value="props.paramRouteForm.method" style="width: 200px">
            <a-select-option v-for="(item, index) in selectArr" :value="item.value" :key="index">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="指定方法参数">
          <a-space direction="vertical">
            <a-button type="primary" @click="addFunctionParams"> 添加参数 </a-button>
            <a-table
              :columns="functionParamsColumn"
              :data-source="props.paramRouteForm.conditions"
              :pagination="false"
            >
              <template #bodyCell="{ column, index: idx }">
                <template v-if="column.dataIndex === 'index'">
                  <a-input v-model:value="props.paramRouteForm.conditions[idx].index" />
                </template>
                <template v-if="column.dataIndex === 'relation'">
                  <a-input v-model:value="props.paramRouteForm.conditions[idx].relation" />
                </template>
                <template v-if="column.dataIndex === 'value'">
                  <a-input v-model:value="props.paramRouteForm.conditions[idx].value" />
                </template>
                <template v-if="column.dataIndex === 'handle'">
                  <a-flex justify="space-between">
                    <MinusOutlined
                      class="edit-icon"
                      :class="{ 'disabled-icon': props.paramRouteForm.conditions.length === 1 }"
                      @click="
                        props.paramRouteForm.conditions.length !== 1 && deleteFunctionParams(idx)
                      "
                    />
                  </a-flex>
                </template>
              </template>
            </a-table>
          </a-space>
        </a-form-item>
        <a-form-item label="路由目的地">
          <a-space direction="vertical">
            <a-button type="primary" @click="addDestination"> 添加目的地 </a-button>
            <a-card
              v-for="(destinationItem, index) in props.paramRouteForm.destinations"
              :key="index"
              :bordered="false"
            >
              <template #title>
                <a-space>
                  目的地{{ index + 1 }}
                  <a-button type="primary" @click="addDestinationCondition(index)">
                    添加条件
                  </a-button>
                </a-space>
              </template>
              <a-table
                :columns="destinationColumn"
                :data-source="destinationItem.conditions"
                :pagination="false"
              >
                <template #bodyCell="{ column, index: idx, record }">
                  <template v-if="column.dataIndex === 'tag'">
                    <a-input v-model:value="record.tag" />
                  </template>
                  <template v-if="column.dataIndex === 'relation'">
                    <a-input v-model:value="record.relation" />
                  </template>
                  <template v-if="column.dataIndex === 'value'">
                    <a-input v-model:value="record.value" />
                  </template>
                  <template v-if="column.dataIndex === 'handle'">
                    <a-flex justify="space-between">
                      <MinusOutlined
                        :class="{
                          'disabled-icon':
                            props.paramRouteForm.destinations[index].conditions.length === 1 &&
                            props.paramRouteForm.destinations.length === 1
                        }"
                        class="edit-icon"
                        @click="deleteDestinationCondition(index, idx)"
                      />
                    </a-flex>
                  </template>
                </template>
              </a-table>
            </a-card>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { DeleteOutlined, MinusOutlined } from '@ant-design/icons-vue'

const emit = defineEmits(['deleteParamRoute'])
const props = defineProps({
  paramRouteForm: {
    type: Object,
    default: () => {}
  },
  index: {
    type: Number
  }
})

const selectArr = [
  {
    label: 'getUserInfo',
    value: 'getUserInfo'
  },
  {
    label: 'register',
    value: 'register'
  },
  {
    label: 'login',
    value: 'login'
  }
]

const functionParamsColumn = [
  {
    title: '参数索引',
    key: 'index',
    dataIndex: 'index',
    width: '30%'
  },
  {
    title: '关系',
    key: 'relation',
    dataIndex: 'relation',
    width: '30%'
  },
  {
    title: '值',
    key: 'value',
    dataIndex: 'value',
    width: '30%'
  },
  {
    title: '操作',
    key: 'handle',
    dataIndex: 'handle',
    width: '10%'
  }
]

const destinationColumn = [
  {
    title: '标签',
    key: 'tag',
    dataIndex: 'tag',
    width: '25%'
  },
  {
    title: '关系',
    key: 'relation',
    dataIndex: 'relation',
    width: '25%'
  },
  {
    title: '值',
    key: 'value',
    dataIndex: 'value',
    width: '25%'
  },
  {
    title: '操作',
    key: 'handle',
    dataIndex: 'handle',
    width: '10%'
  }
]

const addFunctionParams = () => {
  props.paramRouteForm.conditions.push({
    index: '',
    relation: '',
    value: ''
  })
}

const deleteFunctionParams = (idx: number) => {
  props.paramRouteForm.conditions.splice(idx, 1)
}

const addDestination = () => {
  props.paramRouteForm.destinations.push({
    conditions: [
      {
        tag: '',
        relation: '',
        value: ''
      }
    ],
    weight: 0
  })
}

const addDestinationCondition = (idx: number) => {
  props.paramRouteForm.destinations[idx].conditions.push({
    tag: '',
    relation: '',
    value: ''
  })
}

const deleteDestinationCondition = (idx: number, idy: number) => {
  const { destinations } = props.paramRouteForm
  const currentDest = destinations[idx]

  // If the current destination has only one condition and is not the last destination, delete the entire destination
  if (currentDest.conditions.length === 1 && destinations.length > 1) {
    destinations.splice(idx, 1)
    return
  }

  // Otherwise just delete the condition (ensure at least one condition remains)
  if (currentDest.conditions.length > 1) {
    currentDest.conditions.splice(idy, 1)
  }
}
</script>

<style lang="less" scoped>
.__container_services_tabs_param_route {
  .handle-form {
    width: 50px;
    justify-content: space-between;
  }

  .edit-icon {
    font-size: 18px;
  }

  .disabled-icon {
    color: #71777d;
    cursor: not-allowed;
  }
}
</style>
