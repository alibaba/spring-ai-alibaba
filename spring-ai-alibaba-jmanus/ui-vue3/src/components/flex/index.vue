<!-- 
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
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
