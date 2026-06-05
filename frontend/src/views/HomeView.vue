<script setup>
import { onMounted, computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useNoteStore } from '@/store/note'
import axios from 'axios'

const store = useNoteStore()
const router = useRouter()
const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const notesWithTags = ref([])

// 分页相关
const currentPage = ref(1)
const pageSize = ref(10)
const pageSizes = [5, 10, 20]
const total = ref(0)

// 查看正文弹窗
const viewDialogVisible = ref(false)
const viewingNote = ref(null)

// 页面标题
const pageTitle = computed(() => {
  if (store.activeTag === '全部') return 'All Entries'
  return `${store.activeTag}`
})

// 为每个笔记补充标签名
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

// 过滤笔记（基于 createTime 筛选）
const filteredNotes = computed(() => {
  let list = notesWithTags.value
  if (store.activeTag !== '全部') {
    list = list.filter(n => n.tagName === store.activeTag)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(n =>
      (n.title || '').toLowerCase().includes(kw) ||
      (n.content || '').toLowerCase().includes(kw)
    )
  }
  if (startDate.value && endDate.value) {
    list = list.filter(n => {
      if (!n.createTime) return false
      const noteDate = n.createTime.split('T')[0]
      return noteDate >= startDate.value && noteDate <= endDate.value
    })
  }
  // 按创建时间倒序排序
  return list.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
})

// 分页后的数据
const paginatedNotes = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredNotes.value.slice(start, start + pageSize.value)
})

// 更新总数
const updateTotal = () => {
  total.value = filteredNotes.value.length
}

// 监听过滤条件变化，重置页码并刷新总数
watch([filteredNotes, () => pageSize.value], () => {
  currentPage.value = 1
  updateTotal()
}, { immediate: true, deep: true })

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// 标签颜色
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

// 删除笔记
const handleDelete = async (row) => {
  if (!confirm('确认删除该笔记？')) return
  try {
    const res = await axios.delete(`http://localhost:8080/api/note/delete/${row.id}`)
    if (res.data.code === 200) {
      alert('删除成功')
      await refreshNotes()
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (err) {
    console.error(err)
    alert('删除失败')
  }
}

// 编辑跳转
const handleEdit = (row) => {
  router.push(`/notes/edit/${row.id}`)
}

// 查看正文（弹出模态框）
const handleView = (row) => {
  viewingNote.value = row
  viewDialogVisible.value = true
}

// 关闭模态框
const closeDialog = () => {
  viewDialogVisible.value = false
  viewingNote.value = null
}

// 卡片点击事件（弹出正文）
const handleCardClick = (row) => {
  handleView(row)
}

const refreshNotes = async () => {
  await store.fetchNotes()
  await loadTagsForNotes(store.notes)
  updateTotal()
}

onMounted(async () => {
  await store.fetchNotes()
  await store.fetchTags()
  await loadTagsForNotes(store.notes)
  updateTotal()
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
      <div v-else class="card-grid">
        <div v-for="note in paginatedNotes" :key="note.id" class="entry-card" @click="handleCardClick(note)">
          <div class="card-header">
            <h3 class="card-title">
              <span class="note-id">#{{ note.id }}</span>
              {{ note.title || '无标题' }}
            </h3>
          </div>
          <div class="card-actions-row" @click.stop>
            <button class="action-btn view" @click="handleView(note)">查看</button>
            <button class="action-btn edit" @click="handleEdit(note)">编辑</button>
            <button class="action-btn delete" @click="handleDelete(note)">删除</button>
          </div>
          <div class="card-meta">
            <span class="meta-date">更新于：{{ formatTime(note.updateTime || note.createTime) }}</span>
            <span class="create-date">创建于：{{ formatTime(note.createTime) }}</span>
            <span v-if="note.tagName" class="tag-display">
              <span class="tag-dot" :style="{ backgroundColor: getTagColor(note.tagName) }"></span>
              {{ note.tagName }}
            </span>
            <span v-else class="muted">未分类</span>
          </div>
          <div class="card-preview">{{ note.content?.slice(0, 100) }}...</div>
        </div>
      </div>

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :page-sizes="pageSizes"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 查看正文弹窗 -->
    <div v-if="viewDialogVisible" class="modal-overlay" @click.self="closeDialog">
      <div class="modal-card">
        <div class="modal-header">
          <h3 class="modal-title">{{ viewingNote?.title || '无标题' }}</h3>
          <button class="modal-close" @click="closeDialog">✕</button>
        </div>
        <div class="modal-meta">
          <span>更新于：{{ formatTime(viewingNote?.updateTime || viewingNote?.createTime) }}</span>
          <span>创建于：{{ formatTime(viewingNote?.createTime) }}</span>
          <span v-if="viewingNote?.tagName" class="tag-display">
            <span class="tag-dot" :style="{ backgroundColor: getTagColor(viewingNote.tagName) }"></span>
            {{ viewingNote.tagName }}
          </span>
          <span v-else class="muted">未分类</span>
        </div>
        <div class="modal-content">
          <div class="note-full-content">{{ viewingNote?.content || '暂无内容' }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-close" @click="closeDialog">关闭</button>
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
  max-width: 720px;
}
.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}
.search-area {
  display: flex;
  gap: 16px;
  align-items: center;
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
}
.content-area {
  flex: 1;
  overflow-y: auto;
  width: 100%;
  max-width: 720px;
}
.card-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.entry-card {
  background: var(--card-bg);
  border-radius: 14px;
  padding: 20px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
  cursor: pointer;
  transition: 0.2s;
}
.entry-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.card-header {
  margin-bottom: 8px;
}
.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.note-id {
  font-size: 13px;
  font-weight: normal;
  color: var(--text-secondary);
  background: var(--bg-hover);
  padding: 2px 8px;
  border-radius: 12px;
}
.card-actions-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.action-btn {
  padding: 4px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}
.action-btn.view {
  background: #e6f7ff;
  color: #1890ff;
}
.action-btn.view:hover {
  background: #bae7ff;
}
.action-btn.edit {
  background: #dbeafe;
  color: #2563eb;
}
.action-btn.edit:hover {
  background: #bfdbfe;
}
.action-btn.delete {
  background: #fee2e2;
  color: #ef4444;
}
.action-btn.delete:hover {
  background: #fecaca;
}
.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}
.meta-date, .create-date {
  color: #9ca3af;
}
.tag-display {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f3f4f6;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: #4b5563;
}
.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}
.muted {
  color: #9ca3af;
}
.card-preview {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-wrap;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px 0;
}
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--empty-text);
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(2px);
}
.modal-card {
  background: var(--card-bg, white);
  border-radius: 20px;
  width: 600px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 35px rgba(0, 0, 0, 0.2);
  animation: fadeSlideUp 0.2s ease;
}
@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--card-border, #eef2f6);
}
.modal-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}
.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-secondary);
  line-height: 1;
}
.modal-close:hover {
  color: var(--text-primary);
}
.modal-meta {
  padding: 12px 24px;
  background: var(--bg-hover, #f9fafb);
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--card-border, #eef2f6);
}
.modal-content {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}
.note-full-content {
  white-space: pre-wrap;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-primary);
}
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--card-border, #eef2f6);
  display: flex;
  justify-content: flex-end;
}
.btn-close {
  padding: 8px 20px;
  background: var(--bg-hover, #f3f4f6);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
}
.btn-close:hover {
  background: var(--border-color, #e5e7eb);
}
</style>