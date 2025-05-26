<template>
  <div class="__container_router_tab_index">
    <div :key="key">
      <div v-if="tabRoute.meta?.tab" class="header">
        <a-row>
          <a-col :span="1">
            <span @click="handleBack" style="float: left">
              <Icon icon="material-symbols:keyboard-backspace-rounded" class="back" />
            </span>
          </a-col>
          <a-col :span="18">
            <TAB_HEADER_TITLE :route="tabRoute" />
          </a-col>
        </a-row>
        <a-tabs @change="handleTabChange" v-model:activeKey="activeKey">
          <a-tab-pane v-for="tab in visibleTabs" :key="String(tab.name)">
            <template #tab>
              <span>
                <Icon
                  v-if="tab.meta?.icon"
                  style="margin-bottom: -2px"
                  :icon="tab.meta.icon"
                ></Icon>
                {{ tab.name ? $t(String(tab.name)) : '' }}
              </span>
            </template>
          </a-tab-pane>
        </a-tabs>
      </div>

      <a-spin class="tab-spin" :spinning="transitionFlag">
        <div id="layout-tab-body" class="tab-content">
          <router-view :key="routerKey" v-if="!transitionFlag" />
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'LayoutTab'
})
import { computed, provide, reactive, ref } from 'vue'
import { Icon } from '@iconify/vue'
import { useRoute, useRouter } from 'vue-router'
import _ from 'lodash'
import { PRIMARY_COLOR, TAB_HEADER_TITLE } from '@/base/constants'
import type { RouteLocationNormalizedLoaded, RouteRecordName, RouteRecordRaw } from 'vue-router'
import type { RouterMeta } from '@/router/RouterMeta'

type TabMeta = RouterMeta & {
  icon?: string
  tab?: boolean
  hidden?: boolean
  back?: string
}

type TabRouteRecord = RouteRecordRaw & {
  meta?: TabMeta
  name: string | symbol
}

const TAB_STATE = reactive({})
provide('TAB_LAYOUT_STATE', TAB_STATE)

const router = useRouter()
const tabRoute = useRoute() as RouteLocationNormalizedLoaded & { meta: TabMeta }
const __ = PRIMARY_COLOR

const key = ref(_.uniqueId('__tab_page'))
const activeKey = ref(String(tabRoute.name || ''))
const transitionFlag = ref(false)

const routerKey = computed(() => {
  return `${String(tabRoute.name)}_${_.uniqueId()}`
})

const visibleTabs = computed(() => {
  const children = (tabRoute.meta?.parent?.children || []) as TabRouteRecord[]
  return children.filter((route) => route.meta?.tab && !route.meta?.hidden)
})

function handleBack() {
  router.replace(tabRoute.meta?.back || '../')
}

function handleTabChange(newKey: string) {
  router.push({ name: newKey as RouteRecordName })
}

router.beforeEach((to, from, next) => {
  key.value = _.uniqueId('__tab_page')
  transitionFlag.value = true
  activeKey.value = String(to.name || '')
  next()
  setTimeout(() => {
    transitionFlag.value = false
  }, 500)
})
</script>

<style lang="less" scoped>
.__container_router_tab_index {
  :deep(.tab-spin) {
    margin-top: 20vh;
  }

  :deep(.ant-tabs-nav) {
    margin: 0;
  }

  .header {
    background: #fafafa;
    padding: 20px 20px 0 20px;
    border-radius: 10px;
    margin-bottom: 20px;
  }

  .back {
    font-size: 24px;
    margin-bottom: -2px;
    color: v-bind('PRIMARY_COLOR');
  }

  .tab-content {
    transition: scroll-top 0.5s ease;
    overflow: auto;
    height: calc(100vh - 300px);
    padding-bottom: 20px;
  }
}
</style>
