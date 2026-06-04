<script setup>
import { RouterView, useRoute } from 'vue-router'
import { ref, onMounted, computed } from 'vue'
import '@/styles/theme.css'
import Sidebar from '@/components/Sidebar.vue'
import { useNoteStore } from '@/store/note'

const store = useNoteStore()
const route = useRoute()

// 深色模式状态
const isDarkMode = ref(false)

// 切换深色模式
const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  if (isDarkMode.value) {
    document.documentElement.setAttribute('data-theme', 'dark')
    localStorage.setItem('theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
    localStorage.setItem('theme', 'light')
  }
}

// 初始化主题
const initTheme = () => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDarkMode.value = true
    document.documentElement.setAttribute('data-theme', 'dark')
  }
}

// 判断是否显示 Sidebar
const showSidebar = computed(() => {
  const hiddenRoutes = ['/calendar', '/search']
  return !hiddenRoutes.includes(route.path)
})

onMounted(async () => {
  await store.fetchNotes()
  await store.fetchTags()
  initTheme()
})
</script>

<template>
  <div class="app">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-inner">
        <h1 class="logo">NotePlan</h1>
        <nav class="nav">
          <RouterLink to="/">首页</RouterLink>
          <RouterLink to="/notes/edit">新建笔记</RouterLink>
          <RouterLink to="/schedules">日程</RouterLink>
          <RouterLink to="/calendar">日历</RouterLink>
          <RouterLink to="/tags">标签</RouterLink>
        </nav>
        <!-- ✅ 主题切换按钮 -->
        <button class="theme-toggle" @click="toggleTheme" :title="isDarkMode ? '切换到浅色模式' : '切换到深色模式'">
          <span class="icon">{{ isDarkMode ? '☀️' : '💡' }}</span>
        </button>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main">
      <div class="main-inner">
        <Sidebar v-if="showSidebar" class="sidebar-wrapper" />
        <div class="content-wrapper">
          <RouterView :key="$route.fullPath" />
        </div>
      </div>
    </main>

    <!-- 全局 FAB 按钮 -->
    <div class="fab" @click="store.createNote()">＋</div>
  </div>
</template>

<style>
* {
  box-sizing: border-box;
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.header {
  display: flex;
  justify-content: center;
  background: var(--header-bg);
  border-bottom: 1px solid var(--header-border);
  flex-shrink: 0;
  transition: background 0.3s, border-color 0.3s;
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding: 0.75rem 1.5rem;
  width: 100%;
  max-width: 1200px;
}

.logo {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
}

.nav {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.nav a {
  color: var(--header-text);
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 6px;
  transition: 0.2s;
}

.nav a:hover {
  background: var(--bg-hover);
}

.nav a.router-link-active {
  color: var(--header-active);
}

/* 主题切换按钮 */
.theme-toggle {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  transition: 0.2s;
  color: var(--text-primary);
}

.theme-toggle:hover {
  background: var(--bg-hover);
  transform: scale(1.1);
}

.icon {
  display: block;
}

/* 主内容区 */
.main {
  display: flex;
  justify-content: center;
  flex: 1;
  height: calc(100vh - 64px);
  overflow: hidden;
}

.main-inner {
  display: flex;
  width: 100%;
  max-width: 1200px;
  height: 100%;
}

.sidebar-wrapper {
  flex-shrink: 0;
}

.content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  background: var(--bg-primary);
  transition: background 0.3s;
}

/* 全局 FAB 按钮 */
.fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  background: #fbbf24;
  color: #1a1a1a;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 300;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.4);
  transition: 0.2s;
  z-index: 100;
}

.fab:hover {
  transform: scale(1.05);
}
</style>