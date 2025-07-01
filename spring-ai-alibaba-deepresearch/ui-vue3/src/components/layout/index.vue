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
            :defaultActiveKey="currentConvKey"
            :items="conversationItems"
        />
      </Flex>
      <Flex class="right" flex="1" vertical>
        <Flex justify="space-between" flex="1" class="header" style="">
          <Flex>
            Model
          </Flex>
          <!--            <color-picker-->
          <!--                :pureColor="token.colorPrimary"-->
          <!--                @pureColorChange="changeTheme"-->
          <!--                format="hex6"-->
          <!--                shape="circle"-->
          <!--                useType="pure"-->
          <!--            ></color-picker>-->
          <Flex gap="middle">
            <div>
              <ASegmented v-model:value="locale" :options="i18nConfig.opts"/>
            </div>
            <AAvatar style="background: rebeccapurple">{{ username.substring(0, 1) }}</AAvatar>
          </Flex>
        </Flex>
        <Flex class="content" style="width: 100%">
          <RouterView/>
        </Flex>
      </Flex>
    </Flex>
  </div>

</template>

<script setup lang="tsx">
import {Flex, theme,} from 'ant-design-vue';
import {GithubOutlined,FormOutlined, MenuOutlined, QuestionCircleOutlined} from '@ant-design/icons-vue'
import {computed, h, inject, onMounted, provide, reactive, ref, watch} from 'vue';
import {LOCAL_STORAGE_THEME, PRIMARY_COLOR,} from '@/base/constants'
import {PROVIDE_INJECT_KEY} from '@/base/enums/ProvideInject'
import {changeLanguage, localeConfig} from '@/base/i18n'
import {Conversations} from "ant-design-x-vue";
import Gap from "@/components/tookit/Gap.vue";
import {useConversationStore} from "@/store/ConversationStore";
import {useAuthStore} from "@/store/AuthStore";

const username = useAuthStore().token
const {useToken} = theme;
const collapse = ref(false)

const leftWidth = computed(() => {
  return collapse.value ? '80px' : '224px'
})

let __null = PRIMARY_COLOR
const i18nConfig:any = inject(PROVIDE_INJECT_KEY.LOCALE)
let locale = ref(localeConfig.locale)
const conversationStore = useConversationStore();

const currentConvKey = conversationStore.curConvKey
const conversationItems = conversationStore.items
function createNewConversation() {
  conversationStore.newOne()
}

function changeTheme(val: string) {
  localStorage.setItem(LOCAL_STORAGE_THEME, val)
  PRIMARY_COLOR.value = val
}

onMounted(()=>{
  const mediaQuery = window.matchMedia('(max-width: 768px)');
  function checkMediaQuery(){
    if(mediaQuery.matches){
      // reactive
      collapse.value = true
    }else {
      collapse.value = false
    }
  }
  mediaQuery.addEventListener('change', checkMediaQuery);

})
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
