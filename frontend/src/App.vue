<script setup>
import { RouterView, useRouter } from 'vue-router'
import { onMounted } from 'vue'
import '@/styles/theme.css'
import Sidebar from '@/components/Sidebar.vue'
import { useNoteStore } from '@/store/note'

const store = useNoteStore()
const router = useRouter()

onMounted(async () => {
  await store.fetchNotes()
  await store.fetchTags()
})

const goToNotePage = () => {
  router.push('/notes/edit')
}
</script>

<template>
  <div class="app">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-inner">
        <h1 class="logo">NotePlan</h1>
        <nav class="nav">
          <RouterLink to="/">首页</RouterLink>
          <RouterLink to="/notes/edit">笔记</RouterLink>
          <RouterLink to="/schedules">日程</RouterLink>
          <RouterLink to="/calendar">日历</RouterLink>
          <RouterLink to="/tags">标签</RouterLink>
        </nav>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main">
      <div class="main-inner">
        <!-- 左侧 Sidebar -->
        <Sidebar class="sidebar-wrapper" />

        <!-- 右侧路由内容 -->
        <div class="content-wrapper">
          <RouterView :key="$route.fullPath" />
        </div>
      </div>
    </main>

    <!-- 全局 FAB 按钮 -->
    <div class="fab" @click="goToNotePage">＋</div>
  </div>
</template>

<style>
:root {
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
  color: #1a1a1a;
  background: #fafafa;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
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
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
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
}

.nav {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.nav a {
  color: #4b5563;
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 6px;
  transition: 0.2s;
}

.nav a:hover {
  background: #f3f4f6;
}

.nav a.router-link-active {
  color: #2563eb;
  font-weight: 600;
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