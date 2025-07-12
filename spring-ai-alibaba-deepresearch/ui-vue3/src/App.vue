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

<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router'
import enUS from 'ant-design-vue/es/locale/en_US'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { provide, reactive, watch } from 'vue'
import dayjs from 'dayjs'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { PRIMARY_COLOR } from '@/base/constants'
import { i18n, localeConfig } from '@/base/i18n'
import { XProvider } from 'ant-design-x-vue'
dayjs.locale('en')
const i18nConfig = reactive(localeConfig)
watch(i18nConfig, val => {
  dayjs.locale(val.locale)
})

provide(PROVIDE_INJECT_KEY.LOCALE, i18nConfig)

/**
 * this function is showing some tips about our Q&A
 * TODO
 */
function globalQuestion() {}

const localeGlobal = reactive(i18n.global.locale)

const router = useRouter()
</script>

<template>
  <x-provider
    :locale="localeGlobal === 'en' ? enUS : zhCN"
    :theme="{
      token: {
        colorPrimary: PRIMARY_COLOR,
      },
    }"
  >
    <RouterView />
  </x-provider>
</template>

<style lang="less">
.__global_float_button_question {
  right: 24px;
}

#nprogress .bar {
  background: #000000 !important;
}

//If you want to show multiple cards, I think you maybe need this style to beautify
._detail {
  box-shadow: 8px 8px 4px rgba(162, 162, 162, 0.19);
}

//Display a sub-card in a card to display the data
.description-item-card {
  :deep(.ant-card-body) {
    padding: 10px;
  }

  width: 80%;
  margin-left: 20px;
  border: 1px dashed rgba(162, 162, 162, 0.19);
}

//Display description fields or interactive text in a card
.description-item-content {
  &.no-card {
    padding-left: 20px;
  }

  &.with-card:hover {
    color: v-bind('PRIMARY_COLOR');
  }
}

//The monitoring tab styles are highly uniform
.__container_tabDemo3 {
  .option {
    padding-left: 16px;

    .btn {
      margin-right: 10px;
    }
  }

  :deep(.spin) {
    margin-top: 30px;
  }
}
</style>
