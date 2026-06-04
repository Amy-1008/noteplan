<script setup>
import {computed, ref, onMounted, watch} from 'vue'
import { useRouter } from 'vue-router'
import { useNoteStore } from '@/store/note'
import axios from 'axios'

const store = useNoteStore()
const router = useRouter()
const notesWithTags = ref([])
const todaySchedules = ref([])

// 获取所有笔记的标签
const loadTagsForNotes = async (notes) => {
  if (!notes || notes.length === 0) {
    notesWithTags.value = []
    return
  }

  const promises = notes.map(async (n) => {
    let tagName = '未分类'
    try {
      const tagRes = await axios.get('http://localhost:8080/api/tags/target', {
        params: { targetId: n.id, targetType: 'NOTE' }
      })
      if (tagRes.data.code === 200 && tagRes.data.data) {
        tagName = tagRes.data.data.name
      }
    } catch (e) {
      console.error(`获取笔记 ${n.id} 标签失败`, e)
    }
    return { ...n, tagName }
  })

  notesWithTags.value = await Promise.all(promises)
}

// 获取今日日程
const fetchTodaySchedules = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/schedule/list')
    const allSchedules = response.data.data || []

    // 筛选今天的日程
    const today = new Date()
    today.setHours(0, 0, 0, 0)

    todaySchedules.value = allSchedules.filter(schedule => {
      const scheduleDate = new Date(schedule.endTime)
      scheduleDate.setHours(0, 0, 0, 0)
      return scheduleDate.getTime() === today.getTime()
    })
  } catch (error) {
    console.error('获取今日日程失败', error)
  }
}

// 标签统计（使用 notesWithTags）
const tags = computed(() => {
  const map = {}
  const notes = notesWithTags.value

  notes.forEach(n => {
    const tagName = n.tagName || '未分类'
    if (!map[tagName]) map[tagName] = 0
    map[tagName]++
  })

  return map
})

const selectTag = (tag) => {
  store.setActiveTag(tag)
  if (router.currentRoute.value.path !== '/') {
    router.push('/')
  }
}

const totalEntries = computed(() => notesWithTags.value.length)
const totalWords = computed(() => {
  return notesWithTags.value.reduce((sum, n) => sum + (n.content?.length || 0), 0)
})

// 待办日程数量（未完成的）
const todoCount = computed(() => {
  return todaySchedules.value.filter(s => !s.completed).length
})

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
watch(() => store.sidebarRefreshTrigger, async () => {
  await refreshSidebar()
})
const refreshSidebar = async () => {
  await store.fetchNotes()  // 重新获取笔记
  await loadTagsForNotes(store.notes)  // 重新加载标签
  await fetchTodaySchedules()  // 重新获取今日日程
}
onMounted(async () => {
  if (!store.notes.length) await store.fetchNotes()
  await loadTagsForNotes(store.notes)
  await fetchTodaySchedules()
})
</script>

<template>
  <div class="sidebar">
    <!-- 统计卡片：Insights -->
    <div class="insights-card">
      <div class="insights-header">Insights</div>
      <div class="insights-stats">
        <div class="stat-item">
          <span class="stat-number">{{ totalEntries }}</span>
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
        <button class="add-tag-btn">+</button>
      </div>

      <div class="tag-item" :class="{ active: store.activeTag === '全部' }" @click="selectTag('全部')">
        <span class="tag-icon">🏠</span>
        <span class="tag-name">All Entries</span>
        <span class="tag-count">{{ totalEntries }}</span>
      </div>

      <div v-for="(count, tag) in tags" :key="tag" class="tag-item" :class="{ active: store.activeTag === tag }" @click="selectTag(tag)">
        <span class="tag-dot" :style="{ backgroundColor: getTagColor(tag) }"></span>
        <span class="tag-name">{{ tag }}</span>
        <span class="tag-count">{{ count }}</span>
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
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
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

/* 今日待办卡片 - 调整高度 */
.todo-card {
  background: #e0f2fe;
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 20px;
  height: auto;
  min-height: 100px;
  overflow: hidden;
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
  color: #6b7280;
  margin-bottom: 12px;
}

.add-tag-btn {
  background: #e5e7eb;
  border: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  color: #6b7280;
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
  background: #f3f4f6;
}

.tag-item.active {
  background: #f3f4f6;
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
  color: #1f2937;
}

.tag-count {
  font-size: 12px;
  color: #9ca3af;
}
</style>