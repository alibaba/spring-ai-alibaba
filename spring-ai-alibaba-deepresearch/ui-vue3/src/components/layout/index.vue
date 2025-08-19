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
    <!-- 顶部导航栏 -->
    <div class="top-navbar">
      <div class="navbar-content">
        <!-- 左侧：Logo和标题 -->
        <div class="navbar-left">
          <img src="/logo.svg" alt="DeepResearch" class="logo" />
          <span class="title">DeepResearch</span>
        </div>
        
        <!-- 中间：模式切换标签页 -->
        <div class="navbar-center">
          <a-tabs v-model:activeKey="currentMode" @change="switchMode" class="mode-tabs">
            <a-tab-pane key="chat" :tab="tabLabel('chat', 'MessageOutlined')" />
            <a-tab-pane key="knowledge" :tab="tabLabel('knowledge_base', 'BookOutlined')" />
            <a-tab-pane key="config" :tab="tabLabel('system_config', 'ControlOutlined')" />
          </a-tabs>
        </div>
        
        <!-- 右侧：语言切换和用户下拉框 -->
        <div class="navbar-right">
          <ASegmented v-model:value="locale" :options="i18nConfig.opts" />
          <a-dropdown>
            <a-button type="text" class="user-dropdown">
              <AAvatar style="background: rebeccapurple">{{ username?.substring(0, 1) }}</AAvatar>
              <span class="username">{{ username }}</span>
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile">
                  <UserOutlined />
                  用户信息
                </a-menu-item>
                <a-menu-item key="logout" @click="logout">
                  <LogoutOutlined />
                  退出
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </div>
    
    <!-- 下部分：页面内容 -->
    <div class="main-content">
      <Flex justify="space-between" gap="middle" class="body">
        <Flex vertical class="left" :style="{ width: leftWidth, background: '#F1F4F8' }">
          <Flex class="top-button-group" justify="space-between">
            <a-button type="text" @click="collapse = !collapse" class="circle-button">
              <MenuOutlined />
            </a-button>
          </Flex>
          
          <a-divider />
        
        <!-- 聊天模式侧边栏 -->
        <div v-if="currentMode === 'chat'" class="sidebar-content">
          <a-menu :selectable="false">
            <a-menu-item @click="createNewConversation" key="create_new_conversation">
              <FormOutlined />
              <Gap width="2px" />
              <span v-if="!collapse">{{ $t('create_new_conversation') }}</span>
            </a-menu-item>
            <a-menu-item key="clear_all" @click="clearAllConversations">
              <DeleteOutlined />
              <Gap width="2px" />
              <span v-if="!collapse">清空所有会话</span>
            </a-menu-item>
          </a-menu>
          <a-divider />
          <Conversations
            v-if="!collapse"
            style="width: 100%"
            :onActiveChange="changeConv"
            :defaultActiveKey="currentConvKey"
            :items="conversationItems"
            :menu="menuConfig"
          >
          </Conversations>
        </div>
        
        <!-- 知识库模式侧边栏 -->
        <div v-else-if="currentMode === 'knowledge'" class="sidebar-content">
          <a-menu :selectable="false">
            <a-menu-item key="knowledge_management" @click="goToKnowledgeManagement">
              <FileTextOutlined />
              <Gap width="2px" />
              <span v-if="!collapse">{{ $t('knowledge_management') }}</span>
            </a-menu-item>
            <!-- <a-menu-item key="document_upload">
              <UploadOutlined />
              <Gap width="2px" />
              <span v-if="!collapse">{{ $t('document_upload') }}</span>
            </a-menu-item>
            <a-menu-item key="knowledge_search">
              <SearchOutlined />
              <Gap width="2px" />
              <span v-if="!collapse">{{ $t('knowledge_search') }}</span>
            </a-menu-item> -->
          </a-menu>
        </div>
        
        <!-- 系统配置模式侧边栏 -->
        <div v-else-if="currentMode === 'config'" class="sidebar-content">
          <a-menu :selectable="false">
            <a-menu-item key="system_settings">
              <SettingOutlined />
              <Gap width="2px" />
              <span v-if="!collapse">系统设置</span>
            </a-menu-item>
          </a-menu>
        </div>
      </Flex>
        <Flex class="right" flex="1" vertical>
          <Flex class="content" style="width: 100%">
            <RouterView :key="route.fullPath" />
          </Flex>
        </Flex>
      </Flex>
    </div>
  </div>
</template>

<script setup lang="tsx">
import {
  Button,
  Dropdown,
  Flex,
  Input,
  Menu,
  MenuItem,
  type MenuProps,
  message,
  Modal,
  theme,
} from 'ant-design-vue'
import {
  EllipsisOutlined,
  FormOutlined,
  MenuOutlined,
  QuestionCircleOutlined,
  SettingOutlined,
  SmileOutlined,
  EditOutlined,
  StopOutlined,
  DeleteOutlined,
  MessageOutlined,
  BookOutlined,
  ControlOutlined,
  FileTextOutlined,
  UploadOutlined,
  SearchOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons-vue'
import { computed, inject, nextTick, onMounted, ref, watch } from 'vue'
import { PRIMARY_COLOR } from '@/base/constants'
import { PROVIDE_INJECT_KEY } from '@/base/enums/ProvideInject'
import { changeLanguage, localeConfig } from '@/base/i18n'
import { Conversations, type ConversationsProps } from 'ant-design-x-vue'
import Gap from '@/components/toolkit/Gap.vue'
import { useConversationStore } from '@/store/ConversationStore'
import { useAuthStore } from '@/store/AuthStore'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useMessageStore } from '@/store/MessageStore'

const router = useRouter()
const route = useRoute()
const username = useAuthStore().token
// add navigation handler for knowledge management
const goToKnowledgeManagement = () => router.push('/knowledge/management')
const { useToken } = theme
const collapse = ref(false)
const showConfigView = ref(false)

// 当前模式：chat, knowledge, config
const currentMode = ref('chat')

// 监听路由变化更新当前模式
watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/chat')) currentMode.value = 'chat'
    else if (path.startsWith('/knowledge')) currentMode.value = 'knowledge'
    else if (path.startsWith('/config')) currentMode.value = 'config'
    else currentMode.value = 'chat'
  },
  { immediate: true }
)

// 生成带图标的标签
const tabLabel = (textKey: string, iconName: string) => {
  const iconMap: Record<string, any> = {
    MessageOutlined,
    BookOutlined,
    ControlOutlined,
  }
  const IconComponent = iconMap[iconName]
  return (
    <span style="display: flex; align-items: center; gap: 6px;">
      <IconComponent />
      <span>{textKey === 'chat' ? '聊天' : textKey === 'knowledge_base' ? '知识库' : '系统配置'}</span>
    </span>
  )
}

const leftWidth = computed(() => {
  return collapse.value ? '80px' : '224px'
})

const { convId } = route.params
let __null = PRIMARY_COLOR
const i18nConfig: any = inject(PROVIDE_INJECT_KEY.LOCALE)
let locale = ref(localeConfig.locale)
const conversationStore = useConversationStore()
const { conversations } = storeToRefs(conversationStore)
conversationStore.active(convId)
const currentConvKey = conversationStore.curConvKey
const EditableItem = (props: { index: number; disabled?: boolean }) => {
  return (
    <Flex justify="space-between" align="center" style={{ width: '100%' }}>
      {conversationStore.editKey === conversations.value[props.index].key ? (
        <Input
          v-model:value={conversations.value[props.index].title}
          onBlur={() => (conversations.value[props.index].editing = false)}
          onPressEnter={() => (conversationStore.editKey = null)}
        />
      ) : (
        <>
          <span>{conversations.value[props.index].title}</span>
        </>
      )}
    </Flex>
  )
}
const conversationItems = computed(() => {
  return conversations.value.map((x: any, index: number) => {
    return {
      // icon:  <MessageOutlined/>,
      ...x,
      label: <EditableItem index={index} />,
    }
  })
})
const menuConfig: ConversationsProps['menu'] = conversation => ({
  items: [
    {
      label: 'edit',
      key: 'edit',
      icon: <EditOutlined />,
    },
    {
      label: 'delete',
      key: 'delete',
      icon: <DeleteOutlined />,
      danger: true,
    },
  ],
  onClick: menuInfo => {
    switch (menuInfo.key) {
      case 'edit':
        conversationStore.editKey = conversation.key
        break
      case 'delete':
        conversationStore.delete(conversation.key)
        if (route.params.convId === conversation.key) {
          const messageStore = useMessageStore()
          delete messageStore.history[conversation.key]
          delete messageStore.currentState[conversation.key]
          router.push('/chat')
        }
        break
      default:
        break
    }
    menuInfo.domEvent.stopPropagation()
  },
})

function createNewConversation() {
  const { key } = conversationStore.newOne('Unnamed conversation')
  changeConv(key)
}

function switchMode(mode: string) {
  switch (mode) {
    case 'chat':
      router.push('/chat')
      break
    case 'knowledge':
      router.push('/knowledge')
      break
    case 'config':
      router.push('/config')
      break
    default:
      router.push('/chat')
  }
}

// 退出登录
const logout = () => {
  const authStore = useAuthStore()
  authStore.token = null
  router.push('/login')
}

// 清空所有会话
const clearAllConversations = () => {
  Modal.confirm({
    title: '确认清空所有会话',
    content: '此操作将永久删除所有会话记录，无法恢复。确定要继续吗？',
    okText: '确定',
    cancelText: '取消',
    onOk() {
      // 清空会话存储
      conversationStore.clearAll()
      
      // 清空消息历史
      const messageStore = useMessageStore()
      messageStore.history = {}
      messageStore.currentState = {}
      
      // 跳转到默认聊天页面
      router.push('/chat')
      
      // 显示成功提示
      message.success('所有会话已清空')
    }
  })
}

onMounted(() => {
  const mediaQuery = window.matchMedia('(max-width: 768px)')

  function checkMediaQuery() {
    if (mediaQuery.matches) {
      // reactive
      collapse.value = true
    } else {
      collapse.value = false
    }
  }

  mediaQuery.addEventListener('change', checkMediaQuery)
})

function changeConv(id: any) {
  router.push(`/chat/${id}`)
}

function openConfigView() {
  router.push('/config')
}

watch(locale, value => {
  changeLanguage(value)
})
</script>
<style lang="less" scoped>
.__container_layout_index {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏样式 */
.top-navbar {
  height: 64px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 1000;
}

.navbar-content {
  height: 100%;
  // max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  position: relative;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.logo {
  height: 32px;
  width: 32px;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #1890ff;
}

.navbar-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  max-width: 400px;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
  margin-left: auto;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  height: auto;
}

.username {
  font-size: 14px;
  color: #666;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  overflow: hidden;
}

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
  height: 100%;
  width: 100%;
  overflow: hidden;

  :deep(.ant-menu-light) {
    background: none;
    padding: 12px;
  }

  .left {
    transition: width 0.3s ease;
    
    .sidebar-content {
      flex: 1;
      overflow-y: auto;
      
      :deep(.ant-menu-light) {
        background: none;
        padding: 0 12px;
      }
    }
  }

  .right {
  }

  .content {
    width: 100%;
    height: 100%;
  }
}

/* 顶部导航栏中的模式切换标签页样式 */
.navbar-center .mode-tabs {
  width: 100%;
  
  :deep(.ant-tabs-nav) {
    margin: 0;
    
    .ant-tabs-tab {
      padding: 6px 16px;
      margin: 0 2px;
      border-radius: 8px;
      transition: all 0.3s ease;
      position: relative;
      background: transparent;
      
      &:hover {
        background: rgba(24, 144, 255, 0.08);
      }
      
      &.ant-tabs-tab-active {
        background: transparent;
        color: #1890ff;
        font-weight: 500;
      }
    }
    
    .ant-tabs-ink-bar {
      background: #1890ff;
      height: 2px;
      border-radius: 1px;
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .navbar-content {
    padding: 0 16px;
  }
  
  .navbar-center {
    margin: 0 20px;
  }
  
  .title {
    display: none;
  }
  
  .username {
    display: none;
  }
  
  .body {
    .left {
      position: fixed;
      top: 0;
      left: 0;
      z-index: 1000;
      height: 100%;
      box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
      
      &.collapsed {
        transform: translateX(-100%);
      }
    }
    
    .right {
      margin-left: 0;
    }
  }
}
</style>
