<template>
  <div class="note-container">
    <div class="note-header">
      <h2>我的笔记</h2>
      <el-button type="primary" @click="goToCreate">+ 新建笔记</el-button>
    </div>

    <!-- 标签侧边栏筛选（复用现有组件） -->
    <div class="note-sidebar">
      <TagSidebar v-model="currentTag" @change="onTagChange" />
    </div>

    <div class="note-list">
      <el-empty v-if="filteredNotes.length === 0" description="暂无笔记，点击右上角新建" />
      <div v-else class="note-cards">
        <el-card v-for="note in paginatedNotes" :key="note.id" class="note-card" shadow="hover">
          <template #header>
            <div class="note-card-header">
              <span class="note-title">{{ note.title || '无标题' }}</span>
              <div class="note-actions">
                <el-button text type="primary" @click="editNote(note.id)">编辑</el-button>
                <el-button text type="danger" @click="confirmDelete(note)">删除</el-button>
              </div>
            </div>
          </template>
          <div class="note-content-preview">{{ note.content?.slice(0, 100) }}...</div>
          <div class="note-footer">
            <span class="note-time">更新于 {{ formatDate(note.updateTime) }}</span>
            <el-tag v-if="noteTagMap[note.id]" size="small">{{ noteTagMap[note.id] }}</el-tag>
          </div>
        </el-card>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-if="filteredNotes.length > pageSize"
          background
          layout="prev, pager, next"
          :total="filteredNotes.length"
          :page-size="pageSize"
          v-model:current-page="currentPage"
        />
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <el-dialog v-model="deleteDialogVisible" title="确认删除" width="30%">
      <span>确定要删除笔记「{{ deleteTarget?.title }}」吗？删除后可在日历中解除关联。</span>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="doDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import TagSidebar from '@/components/TagSidebar.vue'
import { getNoteList, deleteNote } from '@/api/note'
import axios from 'axios'

const router = useRouter()
const noteList = ref([])
const currentTag = ref('all')   // 'all', 'uncategorized', 或具体 tagId
const tagMap = ref({})           // tagId -> tagName
const deleteDialogVisible = ref(false)
const deleteTarget = ref(null)
const currentPage = ref(1)
const pageSize = 10

// 获取所有标签（用于显示标签名）
const fetchTags = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/tags')
    if (res.data.code === 200) {
      const tags = res.data.data || []
      tagMap.value = tags.reduce((map, tag) => { map[tag.id] = tag.name; return map }, {})
    }
  } catch (err) {
    console.error('获取标签失败', err)
  }
}

// 获取笔记列表，同时获取每个笔记的标签
const fetchNotes = async () => {
  try {
    const res = await getNoteList()
    if (res.data.code === 200) {
      noteList.value = res.data.data || []
      // 为每个笔记获取其绑定的标签
      await fetchNoteTags()
    } else {
      ElMessage.error(res.data.message || '加载笔记失败')
    }
  } catch (err) {
    ElMessage.error('加载笔记失败，请检查网络')
  }
}

// 获取每个笔记的标签
const noteTagMap = ref({})   // noteId -> tagName
const fetchNoteTags = async () => {
  const promises = noteList.value.map(async (note) => {
    try {
      const res = await axios.get('http://localhost:8080/api/tags/target', {
        params: { targetId: note.id, targetType: 'NOTE' }
      })
      if (res.data.code === 200 && res.data.data) {
        noteTagMap.value[note.id] = res.data.data.name
      } else {
        noteTagMap.value[note.id] = null
      }
    } catch (err) {
      noteTagMap.value[note.id] = null
    }
  })
  await Promise.all(promises)
}

// 根据标签筛选
const filteredNotes = computed(() => {
  if (currentTag.value === 'all') return noteList.value
  if (currentTag.value === 'uncategorized') {
    return noteList.value.filter(n => !noteTagMap.value[n.id])
  }
  // 具体标签ID
  return noteList.value.filter(n => noteTagMap.value[n.id] === tagMap.value[currentTag.value])
})

// 分页后的笔记
const paginatedNotes = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredNotes.value.slice(start, start + pageSize)
})

// 标签切换时重置页码
const onTagChange = () => {
  currentPage.value = 1
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}`
}

const goToCreate = () => {
  router.push('/notes/create')
}

const editNote = (id) => {
  router.push(`/notes/edit/${id}`)
}

const confirmDelete = (note) => {
  deleteTarget.value = note
  deleteDialogVisible.value = true
}

const doDelete = async () => {
  if (!deleteTarget.value) return
  try {
    const res = await deleteNote(deleteTarget.value.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      fetchNotes()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (err) {
    ElMessage.error('删除失败')
  } finally {
    deleteDialogVisible.value = false
    deleteTarget.value = null
  }
}

onMounted(() => {
  fetchTags()
  fetchNotes()
})
</script>

<style scoped>
.note-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}
.note-header {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  width: 100%;
}
.note-sidebar {
  width: 220px;
  flex-shrink: 0;
}
.note-list {
  flex: 1;
  min-width: 0;
}
.note-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.note-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.note-title {
  font-weight: bold;
  font-size: 1.1rem;
}
.note-content-preview {
  color: #666;
  margin-bottom: 12px;
}
.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
  color: #999;
}
.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>