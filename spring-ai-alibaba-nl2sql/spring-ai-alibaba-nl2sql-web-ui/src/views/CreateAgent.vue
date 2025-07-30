<template>
  <div class="create-agent-page">
    <!-- 共享导航组件 -->
    <HeaderComponent title="智能体管理" subtitle="创建新智能体" icon="bi bi-robot" />

    <div class="container">
      <h1>创建智能体</h1>
      <p>简化页面用于测试导航显示</p>
    </div>
  </div>
</template>

<script>
import HeaderComponent from '@/components/HeaderComponent.vue';

export default {
  name: 'CreateAgent',
  components: {
    HeaderComponent
  },
  setup() {
    return {}
  }
}
</script>

<style>
/* 基础样式确保页面结构 */
.create-agent-page {
  min-height: 100vh;
  background-color: white;
}
.container {
  padding: 20px;
}
</style>
