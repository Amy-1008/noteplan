<template>
  <div class="entry-card" :class="{ expanded: isExpanded }">
    <!-- 折叠状态 (点击触发展开) -->
    <div v-if="!isExpanded" class="card-collapsed" @click="expand">
      <div class="card-header">
        <h3 class="card-title">{{ note.title || '无标题' }}</h3>
        <div class="card-date">{{ formatDate(note.createTime) }}</div>
      </div>

      <div class="card-body">
        <p class="card-content">{{ note.content || '暂无内容' }}</p>
      </div>

      <div class="card-footer">
        <div class="card-tags">
          <span v-for="tag in tagList" :key="tag" class="card-tag">
            <span class="tag-dot" :style="{ backgroundColor: getTagColorFn(tag) }"></span>
            {{ tag }}
          </span>
        </div>
        <button class="star-btn">☆</button>
      </div>
    </div>

    <!-- 展开状态 (详情 + 操作按钮) -->
    <div v-else class="card-expanded">
      <div class="expanded-header">
        <button class="collapse-btn" @click="collapse">← 收起</button>
        <div class="expanded-actions">
          <button class="action-btn history" @click="viewHistory">📜 历史</button>
          <button class="action-btn edit" @click="editNote">编辑</button>
          <button class="action-btn delete" @click="handleDeleteNote">删除</button>
        </div>
      </div>

      <div class="expanded-body">
        <h2 class="expanded-title">{{ note.title || '无标题' }}</h2>
        <div class="expanded-content">{{ note.content || '暂无内容' }}</div>
      </div>

      <div class="expanded-footer">
        <div class="expanded-meta">
          <span class="expanded-date">{{ formatFullDate(note.createTime) }}</span>
          <span v-for="tag in tagList" :key="tag" class="expanded-tag">
            <span class="tag-dot" :style="{ backgroundColor: getTagColorFn(tag) }"></span>
            {{ tag }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useNoteStore } from '@/store/note'
import { deleteNote as deleteNoteApi } from '@/api/note'  // ✅ 重命名导入

const props = defineProps({
  note: Object
})
// 查看历史版本
// 查看历史版本
const viewHistory = () => {
  // 先设置当前笔记为 activeNote，这样 NoteEdit 可以读取
  store.setActiveNote(props.note)
  router.push(`/notes/edit/${props.note.id}`)
}
const router = useRouter()
const store = useNoteStore()
const isExpanded = ref(false)

const expand = () => { isExpanded.value = true }
const collapse = () => { isExpanded.value = false }

// 编辑笔记：跳转到编辑页面
const editNote = () => {
  store.setActiveNote(props.note)
  router.push(`/notes/edit/${props.note.id}`)
}

// 删除笔记 - ✅ 重命名为 handleDeleteNote
const emit = defineEmits(['deleted'])
const handleDeleteNote = async () => {
  if (!confirm('确定要删除这条笔记吗？')) return

  try {
    const res = await deleteNoteApi(props.note.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      await store.fetchNotes()
      collapse()
      emit('deleted')
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (err) {
    ElMessage.error('删除失败')
  }
}

const tagList = computed(() => {
  if (props.note.tagName) {
    return [props.note.tagName]
  }
  return ['未分类']
})

const formatDate = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN', {
    month: 'long',
    day: 'numeric'
  })
}

const formatFullDate = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

const getTagColorFn = (tag) => {
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
</script>

<style scoped>
.entry-card {
  background: white;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  overflow: hidden;
}

.entry-card:hover:not(.expanded) {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.entry-card.expanded {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  border-color: #e5e7eb;
}

.card-collapsed {
  padding: 20px;
  cursor: pointer;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.card-date {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
  margin-top: 2px;
}

.card-body {
  margin-bottom: 12px;
}

.card-content {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.card-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.card-tag {
  display: flex;
  align-items: center;
  gap: 4px;
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

.star-btn {
  background: none;
  border: none;
  font-size: 16px;
  color: #d1d5db;
  cursor: pointer;
  padding: 4px;
}

.star-btn:hover {
  color: #fbbf24;
}

.card-expanded {
  padding: 24px 28px;
  animation: expandIn 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

@keyframes expandIn {
  from {
    opacity: 0;
    transform: scaleY(0.8);
    max-height: 0;
  }
  to {
    opacity: 1;
    transform: scaleY(1);
    max-height: 1000px;
  }
}

.expanded-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.collapse-btn {
  background: none;
  border: none;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  transition: 0.2s;
}

.collapse-btn:hover {
  background: #f3f4f6;
  color: #1a1a1a;
}

.expanded-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 4px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: 0.2s;
  background: #f3f4f6;
  color: #6b7280;
}

.action-btn:hover {
  background: #e5e7eb;
}

.action-btn.edit {
  color: #4b5563;
}

.action-btn.delete {
  color: #ef4444;
}

.action-btn.delete:hover {
  background: #fecaca;
}

.expanded-body {
  margin-bottom: 0;
}

.expanded-title {
  margin: 0 0 12px 0;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
}

.expanded-content {
  font-size: 15px;
  line-height: 1.8;
  color: #374151;
  white-space: pre-wrap;
}

.expanded-footer {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.expanded-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #6b7280;
}

.expanded-date {
  color: #9ca3af;
}

.expanded-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f3f4f6;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: #4b5563;
}
</style>