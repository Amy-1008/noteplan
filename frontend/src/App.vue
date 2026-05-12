<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="app">
    <header class="header">
      <h1 class="logo">NotePlan</h1>
      <nav class="nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/notes">笔记</RouterLink>
        <RouterLink to="/schedules">日程</RouterLink>
        <RouterLink to="/calendar">日历</RouterLink>
        <RouterLink to="/tags">标签</RouterLink>
        <RouterLink to="/search">搜索</RouterLink>
      </nav>
    </header>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<style>
:root {
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
  color: #1a1a1a;
  background: #f6f7f9;
}
* {
  box-sizing: border-box;
}
body {
  margin: 0;
}
.app {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding: 0.75rem 1.5rem;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
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
}
.nav a.router-link-active {
  color: #2563eb;
  font-weight: 600;
}
.main {
  padding: 1.5rem;
  max-width: 960px;
  margin: 0 auto;
}
.card {
  background: #fff;
  border-radius: 8px;
  padding: 1.25rem;
  border: 1px solid #e5e7eb;
}
.muted {
  color: #6b7280;
  font-size: 0.9rem;
}
</style>
