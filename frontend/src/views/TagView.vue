<template>
  <div class="page-container">
    <div class="page-inner">
      <!-- 头部 -->
      <div class="page-header">
        <h2 class="page-title">标签管理</h2>
        <button class="create-btn" @click="addNewTag">+ 新增标签</button>
      </div>

      <!-- 新增标签 -->
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
          <button class="action-btn confirm-btn" @click="confirmAdd">✓</button>
          <button class="action-btn cancel-btn" @click="cancelAdd">✗</button>
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
              <button class="action-btn confirm-btn" @click="confirmEdit(tag.id)">✓</button>
              <button class="action-btn cancel-btn" @click="cancelEdit">✗</button>
            </template>
            <template v-else>
              <button class="action-btn edit-btn" @click="startEdit(tag)">✎</button>
              <button class="action-btn delete-btn" @click="confirmDelete(tag)">🗑</button>
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

    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-content">
        <h3 class="modal-title">确认删除</h3>
        <p class="modal-message">
          确定要删除标签 <strong>{{ deleteTarget?.name }}</strong> 吗？
        </p>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="closeDeleteModal">取消</button>
          <button class="modal-btn confirm" @click="deleteTag">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import axios from 'axios'

const API_BASE = 'http://localhost:8080/api'

const tagList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 10
const editingId = ref(null)
const editName = ref('')
const isAdding = ref(false)
const newTagName = ref('')
const showDeleteModal = ref(false)
const deleteTarget = ref(null)
const addInput = ref(null)
const editInput = ref(null)

const paginatedTags = computed(() => {
  const sorted = [...tagList.value].sort((a, b) => b.rank - a.rank)
  const start = (currentPage.value - 1) * pageSize
  return sorted.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.ceil(tagList.value.length / pageSize))

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

const startEdit = (tag) => {
  editingId.value = tag.id
  editName.value = tag.name
  nextTick(() => editInput.value?.focus())
}

const confirmEdit = async (id) => {
  const trimmedName = editName.value.trim()
  if (!trimmedName) return
  const targetTag = tagList.value.find(t => t.id === id)
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

const cancelEdit = () => {
  editingId.value = null
  editName.value = ''
}

const confirmDelete = (tag) => {
  deleteTarget.value = tag
  showDeleteModal.value = true
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  deleteTarget.value = null
}

const deleteTag = async () => {
  if (!deleteTarget.value) return
  try {
    const response = await axios.delete(`${API_BASE}/tags/${deleteTarget.value.id}`)
    if (response.data.code === 200) {
      tagList.value = tagList.value.filter(t => t.id !== deleteTarget.value.id)
      closeDeleteModal()
    }
  } catch (error) {
    console.error('删除标签失败:', error)
    closeDeleteModal()
  }
}

const addNewTag = () => {
  if (isAdding.value) return
  isAdding.value = true
  newTagName.value = ''
  nextTick(() => addInput.value?.focus())
}

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

const cancelAdd = () => {
  isAdding.value = false
  newTagName.value = ''
}

const goToPage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

onMounted(() => fetchTags())
</script>

<style scoped>
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
}

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

.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
}

.star {
  font-size: 18px;
}

.star.filled {
  color: #fbbf24;
}

.star.empty {
  color: #d1d5db;
}

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
}

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
}

.edit-btn {
  background: #f3f4f6;
  color: #6b7280;
}

.delete-btn {
  background: #fee2e2;
  color: #ef4444;
}

.confirm-btn {
  background: #22c55e;
  color: white;
}

.cancel-btn {
  background: #e5e7eb;
  color: #6b7280;
}

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
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #6b7280;
}

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
}

.modal-content {
  background: white;
  border-radius: 14px;
  padding: 24px;
  max-width: 400px;
  width: 90%;
}

.modal-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.modal-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.modal-btn.cancel {
  background: #f3f4f6;
  color: #6b7280;
}

.modal-btn.confirm {
  background: #ef4444;
  color: white;
}
</style>