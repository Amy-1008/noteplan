<script setup>
import { onMounted, computed, ref } from 'vue'
import EntryCard from '@/components/EntryCard.vue'
import { useNoteStore } from '@/store/note'
import axios from 'axios'

const store = useNoteStore()
const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const notesWithTags = ref([])

// 页面标题
const pageTitle = computed(() => {
  if (store.activeTag === '全部') return 'All Entries'
  return `${store.activeTag}`
})

// 异步获取标签
const normalizeNote = async (n) => {
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
}

// 加载所有笔记的标签
const loadTagsForNotes = async (notes) => {
  if (!notes || notes.length === 0) {
    notesWithTags.value = []
    return
  }
  const promises = notes.map(async (n) => await normalizeNote(n))
  notesWithTags.value = await Promise.all(promises)
}

// 过滤笔记
const filteredNotes = computed(() => {
  let list = notesWithTags.value
  if (store.activeTag !== '全部') {
    list = list.filter(n => n.tagName === store.activeTag)
  }
  if (keyword.value.trim()) {
    list = list.filter(n =>
        (n.title || '').includes(keyword.value) ||
        (n.content || '').includes(keyword.value)
    )
  }
  if (startDate.value && endDate.value) {
    list = list.filter(n => {
      if (!n.createTime) return false
      const noteDate = n.createTime.split('T')[0]
      return noteDate >= startDate.value && noteDate <= endDate.value
    })
  }
  return list
})

// 按日期分组
const groupedNotes = computed(() => {
  const groups = {}
  filteredNotes.value.forEach(note => {
    if (!note.createTime) return
    const date = note.createTime.split('T')[0]
    if (!groups[date]) groups[date] = []
    groups[date].push(note)
  })
  return groups
})

const formatDate = d =>
    new Date(d).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
const refreshNotes = async () => {
  await loadTagsForNotes(store.notes)
}
onMounted(async () => {
  await store.fetchNotes()
  await store.fetchTags()
  await loadTagsForNotes(store.notes)
})
</script>

<template>
  <div class="home">
    <div class="main-header">
      <h2 class="page-title">{{ pageTitle }}</h2>
      <div class="search-area">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input v-model="keyword" placeholder="Search entries..." class="search-input" />
        </div>
        <div class="date-filter">
          <input type="date" v-model="startDate" class="date-input" />
          <span class="date-separator">至</span>
          <input type="date" v-model="endDate" class="date-input" />
        </div>
      </div>
    </div>

    <div class="content-area">
      <div v-if="filteredNotes.length === 0" class="empty-state">暂无匹配笔记</div>
      <div v-for="(group, date) in groupedNotes" :key="date" class="date-group">
        <div class="date-label">{{ formatDate(date) }}</div>
        <div class="card-grid">
          <EntryCard v-for="n in group" :key="n.id" :note="n" @deleted="refreshNotes"/>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  padding: 24px 32px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-shrink: 0;
  width: 100%;
  max-width: 680px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.search-box {
  display: flex;
  align-items: center;
  background: var(--card-bg);
  border-radius: 24px;
  padding: 6px 16px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.search-icon {
  color: var(--text-secondary);
  margin-right: 8px;
  font-size: 14px;
}

.search-input {
  border: none;
  outline: none;
  font-size: 14px;
  padding: 6px 0;
  width: 200px;
  background: transparent;
  color: var(--text-primary);
}

.search-input::placeholder {
  color: var(--text-secondary);
}

.date-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-input {
  padding: 6px 12px;
  border: 1px solid var(--card-border);
  border-radius: 24px;
  font-size: 13px;
  background: var(--card-bg);
  outline: none;
  box-shadow: var(--card-shadow);
  color: var(--text-primary);
}

.date-input:focus {
  border-color: var(--accent);
}

.date-separator {
  color: var(--text-secondary);
  font-size: 13px;
}

.content-area {
  flex: 1;
  overflow-y: auto;
  width: 100%;
  max-width: 680px;
}

.date-group {
  margin-bottom: 24px;
}

.date-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 12px;
  padding-left: 4px;
}

.card-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--empty-text);
}
</style>