<template>
  <div class="page-container">
    <div class="page-inner">
      <!-- 头部 -->
      <div class="page-header">
        <h2 class="page-title">笔记管理</h2>
        <button class="create-btn" @click="handleCreate">+ 新建笔记</button>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar card">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input v-model="queryParams.title" placeholder="搜索笔记标题..." class="search-input" />
        </div>
        <div class="filter-group">
          <select v-model="queryParams.tagId" class="filter-select">
            <option :value="null">全部标签</option>
            <option v-for="tag in tagList" :key="tag.id" :value="tag.id">
              {{ tag.name }}
            </option>
          </select>
          <button class="filter-btn" @click="handleQuery">查询</button>
          <button class="reset-btn" @click="resetQuery">重置</button>
        </div>
      </div>

      <!-- 笔记列表（卡片式表格） -->
      <div class="note-list card">
        <div v-if="loading" class="loading-state">加载中...</div>
        <div v-else-if="paginatedNotes.length === 0" class="empty-state">暂无笔记</div>

        <div v-else v-for="note in paginatedNotes" :key="note.id" class="note-row">
          <div class="row-left">
            <div class="row-title">{{ note.title || '无标题' }}</div>
            <div class="row-meta">
              <span class="row-date">{{ formatDate(note.updateTime) }}</span>
              <span v-if="note.tagName" class="tag-display">
                <span class="tag-dot" :style="{ backgroundColor: getTagColor(note.tagName) }"></span>
                {{ note.tagName }}
              </span>
              <span v-else class="muted">未分类</span>
            </div>
          </div>
          <div class="row-actions">
            <button class="action-btn view" @click="handleView(note)">查看</button>
            <button class="action-btn edit" @click="handleEdit(note)">修改</button>
            <button class="action-btn delete" @click="handleDelete(note)">删除</button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination">
        <button class="page-btn" @click="changePage(currentPage - 1)" :disabled="currentPage === 1">
          ← 上一页
        </button>
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button class="page-btn" @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages">
          下一页 →
        </button>
      </div>
    </div>

    <!-- ========== 自定义新建/编辑笔记弹窗 ========== -->
    <div v-if="dialogVisible" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">{{ dialogTitle }}</h3>
          <button class="modal-close" @click="closeModal">✕</button>
        </div>

        <div class="modal-body">
          <el-form :model="noteForm" label-width="80px" ref="formRef" :rules="formRules">
            <el-form-item label="标题" prop="title">
              <el-input v-model="noteForm.title" placeholder="标题（可选）" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item label="内容" prop="content">
              <el-input
                  v-model="noteForm.content"
                  type="textarea"
                  :rows="12"
                  placeholder="请输入笔记内容"
                  maxlength="5000"
                  show-word-limit
              />
            </el-form-item>
            <el-form-item label="标签">
              <TagSelector v-model="noteForm.tagId" @tag-created="handleTagCreated" />
            </el-form-item>
          </el-form>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="closeModal">取消</button>
          <button class="btn-confirm" @click="saveNote" :disabled="saving">
            {{ saving ? '保存中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 自定义查看正文弹窗 ========== -->
    <div v-if="viewDialogVisible" class="modal-overlay" @click.self="closeViewModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3 class="modal-title">{{ viewNote.title || '笔记正文' }}</h3>
          <button class="modal-close" @click="closeViewModal">✕</button>
        </div>

        <div class="modal-body">
          <div class="view-meta">
            <span>更新于：{{ formatDate(viewNote.updateTime) }}</span>
            <span v-if="viewNote.tagName" class="tag-display">
              <span class="tag-dot" :style="{ backgroundColor: getTagColor(viewNote.tagName) }"></span>
              {{ viewNote.tagName }}
            </span>
          </div>
          <el-divider />
          <div class="view-body">{{ viewNote.content }}</div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="closeViewModal">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getNoteList, deleteNote, addNote, updateNote } from '@/api/note'
import TagSelector from '@/components/TagSelector.vue'
import axios from 'axios'

// 查询参数
const queryParams = reactive({
  title: '',
  tagId: null
})

// 原始笔记列表
const allNotes = ref([])
const tagList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

// 获取所有标签
const fetchTags = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/tags')
    if (res.data.code === 200) {
      tagList.value = res.data.data || []
    }
  } catch (err) {
    console.error('获取标签列表失败', err)
  }
}

// 获取笔记列表并补充标签名
const fetchNotes = async () => {
  loading.value = true
  try {
    const res = await getNoteList()
    if (res.data.code === 200) {
      const notes = res.data.data || []
      for (const note of notes) {
        const tagRes = await axios.get('http://localhost:8080/api/tags/target', {
          params: { targetId: note.id, targetType: 'NOTE' }
        })
        if (tagRes.data.code === 200 && tagRes.data.data) {
          note.tagName = tagRes.data.data.name
          note.tagId = tagRes.data.data.id
        } else {
          note.tagName = null
          note.tagId = null
        }
      }
      allNotes.value = notes
    } else {
      ElMessage.error(res.data.message || '加载笔记失败')
    }
  } catch (err) {
    ElMessage.error('加载笔记失败，请检查网络')
  } finally {
    loading.value = false
  }
}

// 筛选后的笔记
const filteredNotes = computed(() => {
  let result = [...allNotes.value]
  if (queryParams.title) {
    const keyword = queryParams.title.toLowerCase()
    result = result.filter(n => n.title?.toLowerCase().includes(keyword))
  }
  if (queryParams.tagId) {
    result = result.filter(n => n.tagId === queryParams.tagId)
  }
  return result
})

// 分页后数据
const paginatedNotes = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredNotes.value.slice(start, start + pageSize.value)
})

const totalPages = computed(() => {
  return Math.ceil(filteredNotes.value.length / pageSize.value)
})

const handleQuery = () => { currentPage.value = 1 }
const resetQuery = () => {
  queryParams.title = ''
  queryParams.tagId = null
  handleQuery()
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
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

// 新建/编辑弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新建笔记')
const dialogType = ref('create')
const router = useRouter()
const formRef = ref()
const saving = ref(false)
const noteForm = reactive({
  id: null,
  title: '',
  content: '',
  tagId: null
})

const formRules = {
  content: [{ required: true, message: '内容不能为空', trigger: 'blur' }]
}

const handleCreate = () => {
  dialogType.value = 'create'
  dialogTitle.value = '新建笔记'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  router.push(`/notes/edit/${row.id}`)
}

const handleTagCreated = (newTag) => {
  fetchTags()
}

const saveNote = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      let res
      if (dialogType.value === 'create') {
        res = await addNote({
          title: noteForm.title,
          content: noteForm.content
        })
      } else {
        res = await updateNote({
          id: noteForm.id,
          title: noteForm.title,
          content: noteForm.content
        })
      }
      if (res.data.code === 200) {
        const savedNote = res.data.data
        if (noteForm.tagId) {
          await axios.post('http://localhost:8080/api/tags/bind', null, {
            params: {
              targetId: savedNote.id,
              targetType: 'NOTE',
              tagId: noteForm.tagId
            }
          })
        } else if (dialogType.value === 'edit' && noteForm.tagId === null) {
          await axios.delete('http://localhost:8080/api/tags/clear', {
            params: { targetId: savedNote.id, targetType: 'NOTE' }
          })
        }
        ElMessage.success(dialogType.value === 'create' ? '新建成功' : '更新成功')
        dialogVisible.value = false
        fetchNotes()
      } else {
        ElMessage.error(res.data.message || '操作失败')
      }
    } catch (err) {
      console.error('保存笔记失败', err)
      ElMessage.error('保存失败，请重试')
    } finally {
      saving.value = false
    }
  })
}

const resetForm = () => {
  noteForm.id = null
  noteForm.title = ''
  noteForm.content = ''
  noteForm.tagId = null
  formRef.value?.clearValidate()
}

// 删除笔记
const handleDelete = async (row) => {
  try {
    const res = await deleteNote(row.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      if (paginatedNotes.value.length === 1 && currentPage.value > 1) {
        currentPage.value -= 1
      }
      fetchNotes()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (err) {
    ElMessage.error('删除失败')
  }
}

// 查看正文
const viewDialogVisible = ref(false)
const viewNote = ref({})
const handleView = (row) => {
  viewNote.value = row
  viewDialogVisible.value = true
}

const closeModal = () => {
  dialogVisible.value = false
  resetForm()
}

const closeViewModal = () => {
  viewDialogVisible.value = false
}

onMounted(() => {
  fetchTags()
  fetchNotes()
})
</script>

<style scoped>
/* ================= 通用页面样式 ================= */

.page-container {
  padding: 24px 32px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.page-inner {
  width: 100%;
  max-width: 680px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0;
  width: 100%;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
}

.create-btn {
  background: #fbbf24;
  color: #1a1a1a;
  border: none;
  border-radius: 8px;
  padding: 8px 20px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
}

.create-btn:hover {
  background: #f59e0b;
}

/* ================= 搜索栏 ================= */

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  margin-bottom: 20px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.search-icon {
  color: #9ca3af;
  font-size: 16px;
}

.search-input {
  border: none;
  outline: none;
  font-size: 14px;
  padding: 6px 0;
  width: 100%;
  background: transparent;
}

.search-input::placeholder {
  color: #9ca3af;
}

.filter-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.filter-select {
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  color: #1a1a1a;
  outline: none;
}

.filter-btn {
  padding: 6px 16px;
  background: #4f8cff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.filter-btn:hover {
  background: #3b7adf;
}

.reset-btn {
  padding: 6px 16px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.reset-btn:hover {
  background: #e5e7eb;
}

/* ================= 笔记列表 ================= */

.note-list {
  padding: 0;
  overflow: hidden;
}

.note-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.note-row:last-child {
  border-bottom: none;
}

.note-row:hover {
  background: #fafafa;
}

.row-left {
  flex: 1;
}

.row-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.row-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #6b7280;
}

.row-date {
  color: #9ca3af;
}

.muted {
  color: #9ca3af;
}

.row-actions {
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
}

.action-btn.view {
  background: #f3f4f6;
  color: #6b7280;
}

.action-btn.view:hover {
  background: #e5e7eb;
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

/* ================= 分页 ================= */

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.page-btn {
  padding: 6px 16px;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #1a1a1a;
  transition: 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: #f3f4f6;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #6b7280;
}

.loading-state {
  text-align: center;
  padding: 40px 20px;
  color: #9ca3af;
}

/* ================= 通用卡片样式 ================= */

.card {
  background: white;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
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

/* ================= 自定义弹窗样式 ================= */

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
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 14px;
  width: 600px;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
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
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.modal-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.modal-close {
  background: none;
  border: none;
  font-size: 20px;
  color: #9ca3af;
  cursor: pointer;
  padding: 4px;
  transition: 0.2s;
}

.modal-close:hover {
  color: #1a1a1a;
}

.modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px 20px 24px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.btn-cancel {
  padding: 8px 20px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: 0.2s;
}

.btn-cancel:hover {
  background: #e5e7eb;
}

.btn-confirm {
  padding: 8px 20px;
  background: #4f8cff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: 0.2s;
}

.btn-confirm:hover {
  background: #3b7adf;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 查看正文弹窗内的样式 */
.view-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 16px;
}

.view-body {
  white-space: pre-wrap;
  line-height: 1.8;
  color: #374151;
}
</style>