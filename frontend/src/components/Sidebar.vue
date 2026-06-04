<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useNoteStore } from '@/store/note'
import axios from 'axios'

const store = useNoteStore()
const router = useRouter()
const route = useRoute()
const allTags = ref([])
let refreshTimer = null

// 获取所有标签
const fetchAllTags = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/tags')
    if (response.data.code === 200) {
      allTags.value = (response.data.data || []).sort((a, b) => b.rank - a.rank)
    }
  } catch (error) {
    console.error('获取标签失败', error)
  }
}

const goToTagManagement = () => {
  router.push('/tags')
}

const toggleTagRank = async (tag) => {
  const newRank = tag.rank === 1 ? 0 : 1
  try {
    const response = await axios.put(`http://localhost:8080/api/tags/${tag.id}`, {
      name: tag.name,
      rank: newRank
    })
    if (response.data.code === 200) {
      tag.rank = newRank
      allTags.value = [...allTags.value].sort((a, b) => b.rank - a.rank)
      const storeTag = store.tags.find(t => t.id === tag.id)
      if (storeTag) {
        storeTag.rank = newRank
      }
    }
  } catch (error) {
    console.error('切换置顶状态失败:', error)
  }
}

const totalWords = computed(() => {
  const notes = store.notes || []
  return notes.reduce((sum, note) => sum + (note.content?.length || 0), 0)
})

const todoCount = computed(() => {
  const schedules = store.scheduleList || []
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return schedules.filter(schedule => {
    if (schedule.completed) return false
    const scheduleDate = new Date(schedule.endTime)
    scheduleDate.setHours(0, 0, 0, 0)
    return scheduleDate.getTime() === today.getTime()
  }).length
})

const selectTag = (tag) => {
  const path = route.path
  if (path === '/calendar' || path === '/tags') return
  store.setActiveTag(tag)
  if (path === '/' || path.startsWith('/notes')) {
    if (path !== '/') router.push('/')
    return
  }
  if (path === '/schedules' || path.startsWith('/schedule')) {
    if (path !== '/schedules') router.push('/schedules')
    filterSchedulesByTag(tag)
    return
  }
}

const filterSchedulesByTag = (tag) => {
  const allSchedules = store.fullScheduleList || store.scheduleList || []
  let filtered = []
  if (tag === '全部') {
    filtered = allSchedules
  } else {
    filtered = allSchedules.filter(schedule => {
      if (schedule.tagId) {
        const tagObj = store.tags.find(t => t.id === schedule.tagId)
        return tagObj && tagObj.name === tag
      }
      return false
    })
  }
  store.scheduleList = filtered
  if (route.path === '/schedules') {
    window.dispatchEvent(new CustomEvent('schedule-filtered', { detail: filtered }))
  }
}

const getTagColor = (tag) => {
  const colors = {
    '未分类': '#9ca3af',
    '代码': '#4f8cff',
    '梦想': '#f472b6',
    '歌词': '#fbbf24',
    '食谱': '#34d399',
    '艺术': '#60a5fa',
    '生活': '#a78bfa',
    '工作': '#f59e0b',
    '学习': '#6366f1',
    '作业': '#8b5cf6'
  }
  return colors[tag] || '#9ca3af'
}

const startRefreshTimer = () => {
  refreshTimer = setInterval(() => {
    fetchAllTags()
  }, 2000)
}

onMounted(() => {
  fetchAllTags()
  startRefreshTimer()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<template>
  <div class="sidebar">
    <!-- 统计卡片：Insights -->
    <div class="insights-card">
      <div class="insights-header">Insights</div>
      <div class="insights-stats">
        <div class="stat-item">
          <span class="stat-number">{{ store.notes.length }}</span>
          <span class="stat-label">Entries<br />This Year</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">{{ totalWords }}</span>
          <span class="stat-label">Total<br />Words</span>
        </div>
      </div>
    </div>

    <!-- 统计卡片：今日待办 -->
    <div class="todo-card">
      <div class="todo-header">
        <span class="todo-icon">📋</span>
        <span class="todo-title">今日待办</span>
      </div>
      <div class="todo-count">{{ todoCount }}</div>
      <div class="todo-sub">待完成</div>
    </div>

    <!-- 标签列表 -->
    <div class="tags-section">
      <div class="tags-header">
        <span>Tags</span>
        <button class="add-tag-btn" @click="goToTagManagement">+</button>
      </div>

      <div class="tag-item" :class="{ active: store.activeTag === '全部' }" @click="selectTag('全部')">
        <span class="tag-icon">🏠</span>
        <span class="tag-name">All Entries</span>
      </div>

      <div v-for="tag in allTags" :key="tag.id" class="tag-item" :class="{ active: store.activeTag === tag.name }" @click="selectTag(tag.name)">
        <span class="tag-dot" :style="{ backgroundColor: getTagColor(tag.name) }"></span>
        <span class="tag-name">{{ tag.name }}</span>
        <button class="star-btn" @click.stop="toggleTagRank(tag)" :title="tag.rank === 1 ? '取消置顶' : '置顶'">
          <span v-if="tag.rank === 1" class="star filled">★</span>
          <span v-else class="star empty">☆</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  width: 240px;
  padding: 20px 16px;
  height: 100%;
  overflow-y: auto;
  flex-shrink: 0;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  transition: background 0.3s, border-color 0.3s;
}

/* Insights 卡片 */
.insights-card {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
  border-radius: 16px;
  padding: 20px;
  color: white;
  margin-bottom: 16px;
}

.insights-header {
  font-size: 12px;
  font-weight: 600;
  opacity: 0.8;
  margin-bottom: 12px;
}

.insights-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  font-size: 11px;
  opacity: 0.8;
  line-height: 1.3;
}

/* 今日待办卡片 */
.todo-card {
  background: #e0f2fe;
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 20px;
  height: auto;
  min-height: 100px;
}

.todo-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0369a1;
  margin-bottom: 8px;
}

.todo-icon {
  font-size: 18px;
}

.todo-title {
  font-size: 14px;
  font-weight: 600;
  color: #0369a1;
}

.todo-count {
  font-size: 32px;
  font-weight: 700;
  color: #0369a1;
  margin-bottom: 4px;
}

.todo-sub {
  font-size: 12px;
  color: #0369a1;
  opacity: 0.8;
}

/* Tags 部分 */
.tags-section {
  margin-top: 8px;
}

.tags-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.add-tag-btn {
  background: var(--bg-hover);
  border: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  color: var(--text-secondary);
}

.tag-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: 0.15s;
  margin-bottom: 2px;
}

.tag-item:hover {
  background: var(--bg-hover);
}

.tag-item.active {
  background: var(--bg-hover);
}

.tag-icon {
  font-size: 16px;
  margin-right: 10px;
}

.tag-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 10px;
  flex-shrink: 0;
}

.tag-name {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
}

.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.1s;
  margin-left: 8px;
}

.star-btn:hover {
  transform: scale(1.1);
}

.star {
  font-size: 18px;
  line-height: 1;
}

.star.filled {
  color: #fbbf24;
}

.star.empty {
  color: #d1d5db;
}

.star.empty:hover {
  color: #fbbf24;
}
</style>