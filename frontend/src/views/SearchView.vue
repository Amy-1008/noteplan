<template>
  <div class="search-page">
    <h2>搜索</h2>
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="请输入关键词（搜索笔记标题/正文、日程标题/备注）"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch" :loading="loading">搜索</el-button>
    </div>

    <div class="result-block">
      <h3>笔记结果（{{ noteResults.length }}）</h3>
      <el-empty v-if="!loading && noteResults.length === 0" description="暂无匹配笔记" />
      <div v-for="item in noteResults" :key="`note-${item.id}`" class="result-item">
        <div class="title">
          <template v-for="(segment, idx) in highlightSegments(item.title || '无标题')" :key="`note-title-${item.id}-${idx}`">
            <mark v-if="segment.match">{{ segment.text }}</mark>
            <span v-else>{{ segment.text }}</span>
          </template>
        </div>
        <div class="content">
          <template v-for="(segment, idx) in highlightSegments(item.content || '')" :key="`note-content-${item.id}-${idx}`">
            <mark v-if="segment.match">{{ segment.text }}</mark>
            <span v-else>{{ segment.text }}</span>
          </template>
        </div>
      </div>
    </div>

    <div class="result-block">
      <h3>日程结果（{{ scheduleResults.length }}）</h3>
      <el-empty v-if="!loading && scheduleResults.length === 0" description="暂无匹配日程" />
      <div v-for="item in scheduleResults" :key="`schedule-${item.id}`" class="result-item">
        <div class="title">
          <template v-for="(segment, idx) in highlightSegments(item.title || '无标题')" :key="`schedule-title-${item.id}-${idx}`">
            <mark v-if="segment.match">{{ segment.text }}</mark>
            <span v-else>{{ segment.text }}</span>
          </template>
        </div>
        <div class="content">
          <template v-for="(segment, idx) in highlightSegments(item.remark || '')" :key="`schedule-content-${item.id}-${idx}`">
            <mark v-if="segment.match">{{ segment.text }}</mark>
            <span v-else>{{ segment.text }}</span>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { searchNotes } from '@/api/note'
import { searchSchedules } from '@/api/schedule'

const keyword = ref('')
const loading = ref(false)
const noteResults = ref([])
const scheduleResults = ref([])

const escapeRegExp = (str) => str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const highlightSegments = (text) => {
  const source = String(text || '')
  const trimmedKeyword = keyword.value.trim()
  if (!trimmedKeyword || !source) {
    return [{ text: source, match: false }]
  }

  const pattern = new RegExp(escapeRegExp(trimmedKeyword), 'gi')
  const segments = []
  let lastIndex = 0
  let match = pattern.exec(source)

  while (match) {
    const start = match.index
    const end = pattern.lastIndex

    if (start > lastIndex) {
      segments.push({ text: source.slice(lastIndex, start), match: false })
    }
    segments.push({ text: source.slice(start, end), match: true })
    lastIndex = end
    match = pattern.exec(source)
  }

  if (lastIndex < source.length) {
    segments.push({ text: source.slice(lastIndex), match: false })
  }

  return segments.length ? segments : [{ text: source, match: false }]
}

const handleSearch = async () => {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入关键词')
    return
  }
  loading.value = true
  try {
    const [noteRes, scheduleRes] = await Promise.all([
      searchNotes(keyword.value.trim()),
      searchSchedules(keyword.value.trim())
    ])
    noteResults.value = noteRes.data?.data || []
    scheduleResults.value = scheduleRes.data?.data || []
  } catch (e) {
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.search-page { max-width: 960px; margin: 0 auto; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.result-block { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.result-item { padding: 10px 0; border-bottom: 1px solid #f1f5f9; }
.result-item:last-child { border-bottom: none; }
.title { font-weight: 600; margin-bottom: 6px; }
.content { color: #4b5563; white-space: pre-wrap; }
:deep(mark) { background: #fde68a; padding: 0 2px; border-radius: 2px; }
</style>
