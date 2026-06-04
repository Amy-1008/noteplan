<script setup>
import { onMounted, computed, ref } from 'vue'
import EntryCard from '@/components/EntryCard.vue'
import { useNoteStore } from '@/store/note'
import axios from 'axios'

const store = useNoteStore()
const keyword = ref('')
const notesWithTags = ref([])

// 页面标题
const pageTitle = computed(() => {
  if (store.activeTag === '全部') return 'All Entries'
  return `${store.activeTag} Entries`
})

// 异步获取标签
const normalizeNote = async (n) => {
  let tagName = '未分类'
  
  try {
    const tagRes = await axios.get('http://localhost:8080/api/tags/target', {
      params: { 
        targetId: n.id, 
        targetType: 'NOTE' 
      }
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
  
  const promises = notes.map(async (n) => {
    return await normalizeNote(n)
  })
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
  new Date(d).toLocaleDateString('zh-CN', {
    month: 'long',
    day: 'numeric'
  })

onMounted(async () => {
  await store.fetchNotes()
  await store.fetchTags()
  await loadTagsForNotes(store.notes)
})
</script>

<template>
  <div class="home">
    <!-- 头部：标题 + 搜索 -->
    <div class="main-header">
      <h2 class="page-title">{{ pageTitle }}</h2>
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input v-model="keyword" placeholder="Search entries..." class="search-input" />
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <div v-if="filteredNotes.length === 0" class="empty-state">
        暂无匹配笔记
      </div>

      <div v-for="(group, date) in groupedNotes" :key="date" class="date-group">
        <div class="date-label">{{ formatDate(date) }}</div>
        <div class="card-grid">
          <EntryCard v-for="n in group" :key="n.id" :note="n" />
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
  color: #1a1a1a;
}

.search-box {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 24px;
  padding: 6px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #e5e7eb;
}

.search-icon {
  color: #9ca3af;
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
}

.search-input::placeholder {
  color: #9ca3af;
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
  color: #6b7280;
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
  color: #9ca3af;
}
</style>