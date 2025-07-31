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
  <div
    ref="containerRef"
    class="virtual-list-container"
    :style="{ height: containerHeight + 'px' }"
    @scroll="handleScroll"
  >
    <div
      class="virtual-list-phantom"
      :style="{ height: totalHeight + 'px' }"
    ></div>
    <div
      class="virtual-list-content"
      :style="{ transform: `translateY(${offsetY}px)` }"
    >
      <div
        v-for="item in visibleItems"
        :key="getItemKey(item.data, item.index)"
        class="virtual-list-item"
        :style="{ height: itemHeight + 'px' }"
      >
        <slot :item="item.data" :index="item.index">
          {{ item.data }}
        </slot>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useThrottle } from '../utils/debounce.js'

export default {
  name: 'VirtualList',
  props: {
    items: {
      type: Array,
      required: true
    },
    itemHeight: {
      type: Number,
      default: 50
    },
    containerHeight: {
      type: Number,
      default: 400
    },
    overscan: {
      type: Number,
      default: 5
    },
    itemKey: {
      type: [String, Function],
      default: 'id'
    }
  },
  setup(props) {
    const containerRef = ref(null)
    const scrollTop = ref(0)

    const totalHeight = computed(() => {
      return props.items.length * props.itemHeight
    })

    const visibleCount = computed(() => {
      return Math.ceil(props.containerHeight / props.itemHeight)
    })

    const startIndex = computed(() => {
      return Math.max(0, Math.floor(scrollTop.value / props.itemHeight) - props.overscan)
    })

    const endIndex = computed(() => {
      return Math.min(
        props.items.length - 1,
        startIndex.value + visibleCount.value + props.overscan * 2
      )
    })

    const visibleItems = computed(() => {
      const items = []
      for (let i = startIndex.value; i <= endIndex.value; i++) {
        if (props.items[i]) {
          items.push({
            data: props.items[i],
            index: i
          })
        }
      }
      return items
    })

    const offsetY = computed(() => {
      return startIndex.value * props.itemHeight
    })

    const getItemKey = (item, index) => {
      if (typeof props.itemKey === 'function') {
        return props.itemKey(item, index)
      }
      return item[props.itemKey] || index
    }

    const handleScroll = useThrottle((event) => {
      scrollTop.value = event.target.scrollTop
    }, 16) // 60fps

    const scrollToIndex = (index) => {
      if (containerRef.value) {
        const targetScrollTop = index * props.itemHeight
        containerRef.value.scrollTop = targetScrollTop
        scrollTop.value = targetScrollTop
      }
    }

    const scrollToTop = () => {
      scrollToIndex(0)
    }

    const scrollToBottom = () => {
      scrollToIndex(props.items.length - 1)
    }

    // 监听数据变化，重置滚动位置
    watch(() => props.items.length, (newLength, oldLength) => {
      if (newLength !== oldLength) {
        scrollTop.value = 0
        if (containerRef.value) {
          containerRef.value.scrollTop = 0
        }
      }
    })

    return {
      containerRef,
      totalHeight,
      offsetY,
      visibleItems,
      getItemKey,
      handleScroll,
      scrollToIndex,
      scrollToTop,
      scrollToBottom
    }
  }
}
</script>

<style scoped>
.virtual-list-container {
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;
}

.virtual-list-phantom {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: -1;
}

.virtual-list-content {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  will-change: transform;
}

.virtual-list-item {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  transition: background-color var(--transition-fast);
}

.virtual-list-item:hover {
  background-color: var(--bg-hover);
}

.virtual-list-item:last-child {
  border-bottom: none;
}

/* 自定义滚动条 */
.virtual-list-container::-webkit-scrollbar {
  width: 6px;
}

.virtual-list-container::-webkit-scrollbar-track {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.virtual-list-container::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: var(--radius-sm);
  transition: background var(--transition-base);
}

.virtual-list-container::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}
</style>
