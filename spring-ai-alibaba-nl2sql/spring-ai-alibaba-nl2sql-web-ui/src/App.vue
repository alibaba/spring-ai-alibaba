<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* 全局样式 */
:root {
  --primary-color: #1890ff;
  --secondary-color: #52c41a;
  --background-color: #f5f8fa;
  --card-bg: #ffffff;
  --text-color: #333333;
  --border-color: #e8e8e8;
  --code-bg: #282c34;
  --code-color: #abb2bf;
  --shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  --radius: 8px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: var(--background-color);
  color: var(--text-color);
  line-height: 1.6;
  padding: 0;
  margin: 0;
  min-height: 100vh;
}

#app {
  min-height: 100vh;
}
</style>
