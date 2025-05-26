<template>
  <div class="__container_menu">
    <a-menu
      mode="inline"
      :selectedKeys="selectedKeys"
      :open-keys="openKeys"
      @select="selectMenu"
      :items="items"
      @click="handleClick"
    >
    </a-menu>
  </div>
</template>

<script setup lang="ts">
import type { RouteRecordType } from '@/router/defaultRoutes'
import { routes as defaultRoutes } from '@/router/defaultRoutes'
import type { ItemType, MenuProps } from 'ant-design-vue'
import type { ComponentInternalInstance } from 'vue'
import { computed, getCurrentInstance, h, reactive, ref } from 'vue'
import { Icon } from '@iconify/vue'
import { useRoute, useRouter } from 'vue-router'
import type { RouterMeta } from '@/router/RouterMeta'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

const {
  appContext: {
    config: { globalProperties }
  }
} = getCurrentInstance() as ComponentInternalInstance

const routesForMenu = defaultRoutes
const nowRoute = useRoute() as RouteLocationNormalizedLoaded & { meta: RouterMeta }
const router = useRouter()

// load active menu
const activeKey = ref('')
const selectedKeys = computed(() => [activeKey.value || getLoadSelectedKeys(nowRoute.meta)])
const openKeys = reactive<string[]>([])

function getLoadSelectedKeys(meta: RouterMeta | undefined): string {
  if (!meta) return ''
  if (meta.tab || meta.hidden) {
    return meta.parent?.meta ? getLoadSelectedKeys(meta.parent.meta) : ''
  }
  return meta._router_key || ''
}

function loadOpenedKeys() {
  let p = nowRoute.meta?.parent
  while (p?.meta) {
    if (p.meta._router_key) {
      openKeys.push(p.meta._router_key)
    }
    p = p.meta.parent
  }
}

loadOpenedKeys()

function selectMenu(e: { key: string }) {
  activeKey.value = e.key
}

function getItem(
  label: string,
  title: string,
  key?: string,
  icon?: string,
  children?: ItemType[],
  type?: 'group'
): ItemType {
  return {
    key,
    title,
    icon: icon ? h(Icon, { icon }) : undefined,
    children,
    label: computed(() => globalProperties.$t(label)),
    type
  } as ItemType
}

const items = reactive<ItemType[]>([])

function prepareRoutes(
  arr: readonly RouteRecordType[] | undefined,
  arr2: ItemType[],
  parent = 'root'
): void {
  if (!arr || arr.length === 0) return

  for (const r of arr) {
    if (r.meta?.skip) {
      prepareRoutes(r.children, arr2, r.name)
      continue
    }

    if (!r.meta?.hidden) {
      if (!r.children || r.children.length === 0 || r.meta?.tab_parent) {
        arr2.push(getItem(r.name, r.path, r.meta?._router_key, r.meta?.icon))
      } else if (r.children.length === 1) {
        arr2.push(
          getItem(r.children[0].name, r.path, r.meta?._router_key, r.children[0].meta?.icon)
        )
      } else {
        const tmp: ItemType[] = reactive([])
        prepareRoutes(r.children, tmp, r.name)
        arr2.push(getItem(r.name, r.path, r.meta?._router_key, r.meta?.icon, tmp))
      }
    }
  }
}

prepareRoutes(routesForMenu, items)

const handleClick: MenuProps['onClick'] = (e) => {
  if (e.item?.title) {
    router.push(e.item.title as string)
  }
}
</script>

<style lang="less" scoped>
.__container_menu {
  .icon-wrapper {
    .icon {
      font-size: 20px;
      margin-right: 5px;
      margin-bottom: -4px;
      font-weight: 700;
    }
  }
}
</style>
