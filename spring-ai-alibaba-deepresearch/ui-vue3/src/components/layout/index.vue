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
  <div class="__container_layout_index">
    <Flex justify="space-between" gap="middle" class="body">
      <Flex vertical class="left" :style="{width: leftWidth, background: '#F1F4F8'}">
        <Flex class="top-button-group" justify="space-between">
          <a-button type="text"
                    @click="collapse = !collapse"
                    class="circle-button">
            <MenuOutlined/>
          </a-button>
        </Flex>
        <a-menu :selectable="false">
          <a-menu-item
              @click="createNewConversation"
              key="create_new_conversation">
            <FormOutlined/>
            <Gap width="2px"/>
            {{ $t('create_new_conversation') }}
          </a-menu-item>
          <a-menu-item key="help">
            <QuestionCircleOutlined/>
            <Gap width="2px"/>
            {{ $t('help') }}
          </a-menu-item>
        </a-menu>
        <a-divider/>
        <Conversations
            style="width: 100%"
            :onActiveChange="changeConv"
            :defaultActiveKey="currentConvKey"
            :items="conversationItems"
        />
      </Flex>
      <Flex class="right" flex="1" vertical>
        <Flex justify="space-between" flex="1" class="header" style="">
          <Flex>
            Model
          </Flex>
          <Flex gap="middle">
            <ASegmented v-model:value="locale" :options="i18nConfig.opts"/>
            <a-button
                @click="openConfigView"
                type="primary">
              <SettingOutlined/>
            </a-button>
            <AAvatar style="background: rebeccapurple">{{ username?.substring(0, 1) }}</AAvatar>
          </Flex>
        </Flex>
        <Flex class="content" style="width: 100%">
          <RouterView :key="route.fullPath"/>
        </Flex>
      </Flex>
    </Flex>


  </div>

</template>

<script setup lang="tsx">
import {Flex, theme,} from 'ant-design-vue';
import {
  FormOutlined,
  MenuOutlined,
  MessageOutlined,
  QuestionCircleOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'
import {computed, inject, onMounted, ref, watch} from 'vue';
import {PRIMARY_COLOR,} from '@/base/constants'
import {PROVIDE_INJECT_KEY} from '@/base/enums/ProvideInject'
import {changeLanguage, localeConfig} from '@/base/i18n'
import {Conversations} from "ant-design-x-vue";
import Gap from "@/components/tookit/Gap.vue";
import Config from "@/components/layout/config/index.vue";
import {useConversationStore} from "@/store/ConversationStore";
import {useAuthStore} from "@/store/AuthStore";
import {useRoute, useRouter} from "vue-router";

const router = useRouter();
const route = useRoute();
const username = useAuthStore().token
const {useToken} = theme;
const collapse = ref(false)
const showConfigView = ref(false)

const leftWidth = computed(() => {
  return collapse.value ? '80px' : '224px'
})

const {convId} = route.params
let __null = PRIMARY_COLOR
const i18nConfig: any = inject(PROVIDE_INJECT_KEY.LOCALE)
let locale = ref(localeConfig.locale)
const conversationStore = useConversationStore();
conversationStore.active(convId)
const currentConvKey = conversationStore.curConvKey
const conversationItems = computed(() => {
  return conversationStore.conversations.map((x: any) => {
    x.icon = <MessageOutlined/>
    return x
  })
})

function createNewConversation() {
  const {key} = conversationStore.newOne()
  changeConv(key)
}

onMounted(() => {
  const mediaQuery = window.matchMedia('(max-width: 768px)');

  function checkMediaQuery() {
    if (mediaQuery.matches) {
      // reactive
      collapse.value = true
    } else {
      collapse.value = false
    }
  }

  mediaQuery.addEventListener('change', checkMediaQuery);

})

function changeConv(id: any) {
  router.push(`/chat/${id}`)
}

function openConfigView() {
  router.push("/config")
}

watch(locale, (value) => {
  changeLanguage(value)
})

</script>
<style lang="less" scoped>
.top-button-group {
  padding: 10px 25px;
  font-size: 20px;

  .circle-button {
    border-radius: 15px;
    width: 30px;
    height: 30px;
    text-align: center;
    padding: 0;
  }

}

.body {
  height: 100vh;
  width: 100vw;
  overflow: hidden;

  :deep(.ant-menu-light) {
    background: none;
    padding: 12px;
  }

  .right {

  }

  .header {
    width: 100%;
    padding: 10px;
  }

  .content {
    width: 100%;
    min-height: calc(100vh - 50px);
  }
}
</style>
