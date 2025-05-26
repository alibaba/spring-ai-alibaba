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
  <div class="__container_common_config">
    <a-row :gutter="20">
      <a-col :span="6">
        <a-card class="__opt">
          <template #title>
            <div class="title">
              <div class="bg"></div>
              <Icon class="title-icon" icon="icon-park-twotone:application-one"></Icon>
              <a-tooltip placement="topLeft">
                <template #title>{{ route.params?.pathId }}</template>
                <span class="truncate-text">{{ route.params?.pathId }}</span>
              </a-tooltip>
            </div>
          </template>
          <a-menu v-model:selectedKeys="options.current">
            <a-menu-item v-for="(item, i) in options.list" :key="i">
              <Icon
                v-if="i === options.current[0]"
                style="margin-bottom: -5px; font-size: 20px"
                icon="material-symbols:settings-b-roll-rounded"
              ></Icon>
              <Icon
                v-else
                style="margin-bottom: -5px; font-size: 20px; color: grey"
                icon="material-symbols:settings-b-roll-outline-rounded"
              ></Icon>
              {{ $t(item.title) }}
            </a-menu-item>
          </a-menu>
        </a-card>
      </a-col>

      <a-col :span="18">
        <a-card>
          <template #title>
            {{ $t(currentOption.title) }}
            <div v-if="currentOption?.ext" style="float: right">
              <a-button
                type="primary"
                @click="currentOption?.ext?.fun(currentOption?.form?.rules)"
                >{{ currentOption?.ext?.title }}</a-button
              >
            </div>
          </template>
          <a-spin :spinning="waitResponse">
            <a-form
              style="overflow: auto; max-height: calc(100vh - 450px)"
              ref="__config_form"
              :key="options.current"
              :wrapper-col="{ span: 14 }"
              :model="currentOption.form"
              :label-col="{ style: { width: '100px' } }"
              layout="horizontal"
            >
              <slot :name="'form_' + currentOption.key" :current="currentOption"></slot>
            </a-form>
            <a-form-item
              v-if="currentOption?.submit || currentOption?.reset"
              style="margin: 20px 0 0 100px"
            >
              <a-button v-if="currentOption?.submit" type="primary" @click="submit">{{
                $t('submit')
              }}</a-button>
              <a-button v-if="currentOption?.reset" style="margin-left: 10px" @click="reset">{{
                $t('reset')
              }}</a-button>
            </a-form-item>
          </a-spin>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useRoute } from 'vue-router'
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'

let props = defineProps<{
  options: {
    list: {
      key: String
      title: String
      form: {}
      submit: Function | any
      reset: Function | any
    }[]
    current: number[]
  }
}>()

let currentOption: any = computed(() => {
  return props.options.list[props.options.current[0]]
})

let __config_form: any = ref(null)

// wait the server's response, we need show a spin icon
let waitResponse = ref(false)
let route = useRoute()

async function submit() {
  waitResponse.value = true
  await __config_form.value.validate().catch((e: any) => {
    message.error('submit failed [form check]: ' + e)
    waitResponse.value = false
  })

  let cur = props.options.list[props.options.current[0]]
  await cur.submit(cur.form).catch((e: any) => {
    message.error('submit failed [server error]: ' + e)
  })
  waitResponse.value = false
  message.success('submit success')
}

function reset() {
  currentOption.value.reset(currentOption.value.form)
}
</script>
<style lang="less" scoped>
.__container_common_config {
  .truncate-text {
    max-width: 200px;
    /* 根据实际需求调整 */
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  :deep(.ant-segmented-group) {
    flex-flow: column;
  }

  .__opt {
    .title {
      padding: 0 20px;
      font-size: 30px;
      text-align: center;
      color: #605f5f;
      position: relative;
    }

    .title-icon {
      margin-bottom: -5px;
    }

    :deep(.ant-menu-item) {
      width: 80%;

      &:hover {
        //translate: 10px; //transition: all 0.4s;
      }
    }

    :deep(.ant-menu) {
      border: none;
    }

    :deep(.ant-card-head) {
      border: none;
      background: url('@/assets/nav_logo.svg') #fafafa;
      background-size: 100% auto;
      padding: 20px 0;
    }

    :deep(.ant-menu-item-selected) {
      transition: all 0.4s;
      translate: 10px;
      transform: scale(1.1);
    }
  }
}
</style>
