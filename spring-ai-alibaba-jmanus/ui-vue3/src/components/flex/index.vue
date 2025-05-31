<template>
  <div :style="mergedStyle" :class="props?.class">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps, type CSSProperties } from 'vue'

interface FlexProps {
  style?: CSSProperties
  class?: string
  direction?: 'row' | 'row-reverse' | 'column' | 'column-reverse'
  justify?: 'flex-start' | 'flex-end' | 'center' | 'space-between' | 'space-around' | 'space-evenly'
  align?: 'stretch' | 'flex-start' | 'flex-end' | 'center' | 'baseline'
  wrap?: 'nowrap' | 'wrap' | 'wrap-reverse'
  gap?: string
}

const props = defineProps<FlexProps>()

const mergedStyle = computed(() => ({
  display: 'flex',
  flexDirection: props.direction ?? 'row',
  justifyContent: props.justify ?? 'flex-start',
  alignItems: props.align ?? 'center',
  flexWrap: props.wrap ?? 'nowrap',
  gap: props.gap ?? '0px',
  ...props.style,
}))
</script>

<style scoped lang="scss"></style>
