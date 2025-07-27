<template>
  <div class="header">
    <div class="container">
      <div class="title">
        <i :class="icon"></i> {{ title }}
      </div>
      <div class="subtitle">{{ subtitle }}</div>
      <div class="nav-links">
        <router-link 
          to="/" 
          class="nav-link" 
          :class="{ active: $route.name === 'Home' || $route.name === 'NL2SQL' }"
        >
          <i class="bi bi-house"></i> 首页
        </router-link>
        <router-link 
          to="/business-knowledge" 
          class="nav-link"
          :class="{ active: $route.name === 'BusinessKnowledge' }"
        >
          <i class="bi bi-book"></i> 业务知识管理
        </router-link>
        <router-link 
          to="/semantic-model" 
          class="nav-link"
          :class="{ active: $route.name === 'SemanticModel' }"
        >
          <i class="bi bi-diagram-3"></i> 语义模型配置
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HeaderComponent',
  props: {
    title: {
      type: String,
      required: true
    },
    subtitle: {
      type: String,
      required: true
    },
    icon: {
      type: String,
      required: true
    }
  }
}
</script>

<style scoped>
.header {
  background-color: var(--card-bg);
  padding: 1rem;
  box-shadow: var(--shadow);
  position: sticky;
  top: 0;
  z-index: 100;
}

.container {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 2rem;
}

.title {
  font-size: 1.6rem;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 0.3rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.subtitle {
  font-size: 0.95rem;
  color: #666;
  margin-bottom: 0.8rem;
}

.nav-links {
  display: flex;
  gap: 0.8rem;
  margin-top: 0.5rem;
}

.nav-link {
  padding: 0.4rem 0.8rem;
  text-decoration: none;
  color: #666;
  border-radius: var(--radius);
  transition: all 0.3s;
  font-size: 0.95rem;
}

.nav-link:hover {
  background-color: #f0f5ff;
  color: var(--primary-color);
}

.nav-link.active {
  background-color: var(--primary-color);
  color: white;
}

@media (max-width: 768px) {
  .container {
    padding: 0 1rem;
  }
}
</style>
