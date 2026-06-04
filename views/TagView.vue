<template>
  <div class="page-container">
    <div class="page-inner">
      <!-- 头部 -->
      <div class="page-header">
        <h2 class="page-title">标签管理</h2>
        <button class="create-btn" @click="addNewTag">+ 新增标签</button>
      </div>

      <!-- 新增标签的编辑行（显示在最上方） -->
      <div v-if="isAdding" class="tag-item add-mode">
        <div class="tag-info">
          <input
              ref="addInput"
              v-model="newTagName"
              type="text"
              class="tag-input"
              maxlength="20"
              placeholder="输入标签名（最多20字）"
              @keyup.enter="confirmAdd"
              @keyup.esc="cancelAdd"
          />
        </div>
        <div class="tag-actions">
          <button class="action-btn confirm-btn" @click="confirmAdd" title="确认">✓</button>
          <button class="action-btn cancel-btn" @click="cancelAdd" title="取消">✗</button>
        </div>
      </div>

      <!-- 标签列表 -->
      <div class="tag-list">
        <div v-if="loading" class="loading-state">加载中...</div>
        <div v-else-if="paginatedTags.length === 0 && !isAdding" class="empty-state">
          暂无标签，点击右上角"新增标签"开始添加
        </div>

        <div
            v-for="tag in paginatedTags"
            :key="tag.id"
            class="tag-item"
            :class="{ 'editing-mode': editingId === tag.id }"
        >
          <div class="tag-info">
            <button
                class="star-btn"
                @click="toggleRank(tag)"
                :title="tag.rank === 1 ? '取消置顶' : '置顶'"
            >
              <span v-if="tag.rank === 1" class="star filled">★</span>
              <span v-else class="star empty">☆</span>
            </button>

            <span v-if="editingId !== tag.id" class="tag-name">{{ tag.name }}</span>

            <input
                v-else
                ref="editInput"
                v-model="editName"
                type="text"
                class="tag-input"
                maxlength="20"
                placeholder="标签名（最多20字）"
                @keyup.enter="confirmEdit(tag.id)"
                @keyup.esc="cancelEdit"
            />
          </div>

          <div class="tag-actions">
            <template v-if="editingId === tag.id">
              <button class="action-btn confirm-btn" @click="confirmEdit(tag.id)" title="确认">✓</button>
              <button class="action-btn cancel-btn" @click="cancelEdit" title="取消">✗</button>
            </template>
            <template v-else>
              <button class="action-btn edit-btn" @click="startEdit(tag)" title="编辑">✎</button>
              <button class="action-btn delete-btn" @click="confirmDelete(tag)" title="删除">🗑</button>
            </template>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" @click="goToPage(currentPage - 1)" :disabled="currentPage === 1">← 上一页</button>
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button class="page-btn" @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages">下一页 →</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import axios from 'axios'

// API 基础路径
const API_BASE = 'http://localhost:8080/api'

// 数据
const tagList = ref([])
const loading = ref(false)

// 分页相关
const currentPage = ref(1)
const pageSize = 10

// 编辑相关
const editingId = ref(null)
const editName = ref('')
const originalName = ref('')

// 新增相关
const isAdding = ref(false)
const newTagName = ref('')

// 删除弹窗相关
const showDeleteModal = ref(false)
const deleteTarget = ref(null)

// 输入框引用
const addInput = ref(null)
const editInput = ref(null)

// 分页后的标签列表（按 rank 降序排序后分页）
const paginatedTags = computed(() => {
  const sorted = [...tagList.value].sort((a, b) => b.rank - a.rank)
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return sorted.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(tagList.value.length / pageSize)
})

// 获取所有标签
const fetchTags = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE}/tags`)
    if (response.data.code === 200) {
      tagList.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取标签失败:', error)
  } finally {
    loading.value = false
  }
}

// 切换 rank 状态
const toggleRank = async (tag) => {
  const newRank = tag.rank === 1 ? 0 : 1
  try {
    const response = await axios.put(`${API_BASE}/tags/${tag.id}`, {
      name: tag.name,
      rank: newRank
    })
    if (response.data.code === 200) {
      tag.rank = newRank
    }
  } catch (error) {
    console.error('切换置顶状态失败:', error)
  }
}

// 开始编辑
const startEdit = (tag) => {
  editingId.value = tag.id
  editName.value = tag.name
  originalName.value = tag.name
  nextTick(() => {
    editInput.value?.focus()
  })
}

// 确认编辑
const confirmEdit = async (id) => {
  const trimmedName = editName.value.trim()
  if (!trimmedName) return

  const targetTag = tagList.value.find(t => t.id === id)
  if (!targetTag) return

  try {
    const response = await axios.put(`${API_BASE}/tags/${id}`, {
      name: trimmedName,
      rank: targetTag.rank
    })
    if (response.data.code === 200) {
      targetTag.name = trimmedName
      cancelEdit()
    }
  } catch (error) {
    console.error('更新标签失败:', error)
  }
}

// 取消编辑
const cancelEdit = () => {
  editingId.value = null
  editName.value = ''
  originalName.value = ''
}

// 确认删除
const confirmDelete = (tag) => {
  deleteTarget.value = tag
  showDeleteModal.value = true
}

// 关闭删除弹窗
const closeDeleteModal = () => {
  showDeleteModal.value = false
  deleteTarget.value = null
}

// 执行删除
const deleteTag = async () => {
  if (!deleteTarget.value) return

  try {
    const response = await axios.delete(`${API_BASE}/tags/${deleteTarget.value.id}`)
    if (response.data.code === 200) {
      tagList.value = tagList.value.filter(t => t.id !== deleteTarget.value.id)
      if (paginatedTags.value.length === 0 && currentPage.value > 1) {
        currentPage.value--
      }
      closeDeleteModal()
    }
  } catch (error) {
    console.error('删除标签失败:', error)
    closeDeleteModal()
  }
}

// 新增标签
const addNewTag = () => {
  if (isAdding.value) return
  isAdding.value = true
  newTagName.value = ''
  nextTick(() => {
    addInput.value?.focus()
  })
}

// 确认新增
const confirmAdd = async () => {
  const trimmedName = newTagName.value.trim()
  if (!trimmedName) return

  try {
    const response = await axios.post(`${API_BASE}/tags`, {
      name: trimmedName,
      rank: 0
    })
    if (response.data.code === 200) {
      tagList.value.push(response.data.data)
      currentPage.value = totalPages.value
      cancelAdd()
    }
  } catch (error) {
    console.error('创建标签失败:', error)
  }
}

// 取消新增
const cancelAdd = () => {
  isAdding.value = false
  newTagName.value = ''
}

// 分页跳转
const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
/* ================= 页面容器 ================= */
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

/* ================= 头部 ================= */
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
  padding: 6px 16px;
  background: #fbbf24;
  color: #1a1a1a;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: 0.2s;
}

.create-btn:hover {
  background: #f59e0b;
}

/* ================= 标签列表 ================= */
.tag-list {
  background: white;
  border-radius: 14px;
  overflow: hidden;
}

.tag-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.tag-item:last-child {
  border-bottom: none;
}

.tag-item:hover {
  background: #fafafa;
}

.tag-item.editing-mode {
  background: #fefce8;
}

.tag-item.add-mode {
  background: #eff6ff;
}

.tag-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ================= 星星按钮 ================= */
.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.1s;
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

/* ================= 标签名称 ================= */
.tag-name {
  font-size: 16px;
  color: #1a1a1a;
}

.tag-input {
  flex: 1;
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: white;
  transition: 0.2s;
}

.tag-input:focus {
  border-color: #4f8cff;
}

/* ================= 操作按钮 ================= */
.tag-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit-btn {
  background: #f3f4f6;
  color: #6b7280;
}

.edit-btn:hover {
  background: #e5e7eb;
  color: #374151;
}

.delete-btn {
  background: #fee2e2;
  color: #ef4444;
}

.delete-btn:hover {
  background: #fecaca;
  color: #dc2626;
}

.confirm-btn {
  background: #22c55e;
  color: white;
}

.confirm-btn:hover {
  background: #16a34a;
}

.cancel-btn {
  background: #e5e7eb;
  color: #6b7280;
}

.cancel-btn:hover {
  background: #d1d5db;
  color: #374151;
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
  border: none;
  background: #f3f4f6;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #6b7280;
}

/* ================= 状态 ================= */
.loading-state {
  text-align: center;
  padding: 40px 20px;
  color: #9ca3af;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #9ca3af;
}
</style>