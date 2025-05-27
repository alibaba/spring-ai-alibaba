import { computed, ref } from 'vue'

export const PRIMARY_COLOR_DEFAULT = '#17b392'

export const LOCAL_STORAGE_LOCALE = 'LOCAL_STORAGE_LOCALE'
export const LOCAL_STORAGE_THEME = 'LOCAL_STORAGE_THEME'

const item = localStorage.getItem(LOCAL_STORAGE_THEME)

export const PRIMARY_COLOR = ref(item || PRIMARY_COLOR_DEFAULT)
export const PRIMARY_COLOR_T = (percent: string) => computed(() => PRIMARY_COLOR.value + percent)

export const INSTANCE_REGISTER_COLOR: { [key: string]: string } = {
  HEALTHY: 'green',
  REGISTED: 'green',
}
