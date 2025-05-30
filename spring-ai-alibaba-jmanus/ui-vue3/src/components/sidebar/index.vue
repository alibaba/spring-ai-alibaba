<template>
  <div class="sidebar-wrapper" :class="{ 'sidebar-wrapper-collapsed': isCollapsed }">
    <div class="sidebar-switch" @click="toggleSidebar">
      <div class="tb-line-wrapper" :class="{ 'tb-line-wrapper-expanded': !isCollapsed }">
        <div class="tb-line" :class="{ 'tb-line-expanded': !isCollapsed }"></div>
        <div class="bt-line" :class="{ 'bt-line-expanded': !isCollapsed }"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import BlurCard from '@/components/blurCard/index.vue'
import { ref } from 'vue'

const isCollapsed = ref(false)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped>
.sidebar-wrapper {
  position: relative;
  width: 100px;
  height: 100vh;
  background: rgba(255, 255, 255, 0.05);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  transition: width 0.3s ease-in-out;
  cursor: pointer;

  .sidebar-switch {
    position: absolute;
    top: 50%;
    right: -40px;
    transform: translateY(-50%);
    height: 80px;
    z-index: 10;

    .tb-line-wrapper {
      display: flex;
      flex-direction: column;
      gap: 6px;
      transition: all 0.3s ease-in-out;

      .tb-line,
      .bt-line {
        background-color: #667eea;
        width: 22px;
        height: 6px;
        border-radius: 10px;
        transform: rotate(-45deg);
        transition: all 0.3s ease-in-out;
      }
      .tb-line {
        background-color: #6646a2;
        transition: all 0.5s ease-in-out;
        transform: rotate(45deg);
      }

      .tb-line-expanded,
      .bt-line-expanded {
        width: 36px;
      }

      .tb-line-expanded {
        transform: rotate(45deg) translateY(9px);
      }

      .bt-line-expanded {
        transform: rotate(-45deg) translateY(-9px);
      }
    }

    .tb-line-wrapper-expanded {
      transform: rotate(180deg);
    }
  }
}
.sidebar-wrapper-collapsed {
  width: 1px !important;
}
</style>
