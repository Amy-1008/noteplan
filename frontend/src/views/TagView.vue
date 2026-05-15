<template>
  <div class="tags-container">
    <!-- 头部 -->
    <div class="tags-header">
      <h2 class="page-title">标签管理</h2>
      <button class="add-btn" @click="addNewTag">
        <span class="plus-icon">+</span> 新增标签
      </button>
    </div>

    <!-- 标签列表 -->
    <div class="tags-list">
      <div v-if="loading" class="empty-state">加载中...</div>
      <div v-else-if="paginatedTags.length === 0 && !isAdding" class="empty-state">
        暂无标签，点击右上角"新增标签"开始添加
      </div>

      <!-- 新增标签的编辑行（显示在最上方） -->
      <div v-if="isAdding" class="tag-item editing-mode add-mode">
        <div class="tag-info">
          <div class="star-placeholder"></div>
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
          <span class="char-count">{{ newTagName.length }}/20</span>
        </div>
        <div class="tag-actions">
          <button class="action-btn confirm-btn" @click="confirmAdd" title="确认">
            ✓
          </button>
          <button class="action-btn cancel-btn" @click="cancelAdd" title="取消">
            ✗
          </button>
        </div>
      </div>

      <!-- 标签列表项 -->
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
            <button class="action-btn confirm-btn" @click="confirmEdit(tag.id)" title="确认">
              ✓
            </button>
            <button class="action-btn cancel-btn" @click="cancelEdit" title="取消">
              ✗
            </button>
          </template>
          <template v-else>
            <button class="action-btn edit-btn" @click="startEdit(tag)" title="编辑">
              ✎
            </button>
            <button class="action-btn delete-btn" @click="confirmDelete(tag)" title="删除">
              🗑
            </button>
          </template>
        </div>
      </div>
    </div>

    <!-- 分页组件 -->
    <div v-if="totalPages > 1" class="pagination">
      <button
          class="page-btn"
          @click="goToPage(currentPage - 1)"
          :disabled="currentPage === 1"
      >
        上一页
      </button>
      <div class="page-numbers">
        <button
            v-for="page in displayPages"
            :key="page"
            class="page-num"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
        >
          {{ page }}
        </button>
      </div>
      <button
          class="page-btn"
          @click="goToPage(currentPage + 1)"
          :disabled="currentPage === totalPages"
      >
        下一页
      </button>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-content">
        <h3 class="modal-title">确认删除</h3>
        <p class="modal-message">
          确定要删除标签 <strong>{{ deleteTarget?.name }}</strong> 吗？
          <br />
          <span class="warning-text">删除后，所有关联该标签的笔记和日程将解除绑定。</span>
        </p>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="closeDeleteModal">取消</button>
          <button class="modal-btn confirm" @click="deleteTag">确认删除</button>
        </div>
      </div>
    </div>

    <!-- 提示消息组件 -->
    <div v-if="toastMessage" class="toast" :class="toastType">
      {{ toastMessage }}
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

// 提示消息
const toastMessage = ref('')
const toastType = ref('info')
let toastTimer = null

// 输入框引用
const addInput = ref(null)
const editInput = ref(null)

// 分页后的标签列表（按 rank 降序排序后分页）
const paginatedTags = computed(() => {
  // 先按 rank 降序排序（rank=1的在前）
  const sorted = [...tagList.value].sort((a, b) => b.rank - a.rank)
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return sorted.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(tagList.value.length / pageSize)
})

// 显示的页码（最多5个）
const displayPages = computed(() => {
  const pages = []
  let start = Math.max(1, currentPage.value - 2)
  let end = Math.min(totalPages.value, start + 4)

  if (end - start < 4) {
    start = Math.max(1, end - 4)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// 显示提示消息
const showToast = (message, type = 'error') => {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = message
  toastType.value = type
  toastTimer = setTimeout(() => {
    toastMessage.value = ''
  }, 3000)
}

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
    showToast('获取标签失败，请检查网络连接', 'error')
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
      // 保持当前页，不跳转
      showToast(newRank === 1 ? '已置顶' : '已取消置顶', 'success')
    } else {
      showToast(response.data.message || '操作失败', 'error')
    }
  } catch (error) {
    console.error('切换置顶状态失败:', error)
    const errorMsg = error.response?.data?.message || '操作失败'
    showToast(errorMsg, 'error')
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
  if (!trimmedName) {
    showToast('标签名不能为空', 'error')
    return
  }
  if (trimmedName.length > 20) {
    showToast('标签名不能超过20个字符', 'error')
    return
  }

  const targetTag = tagList.value.find(t => t.id === id)
  if (!targetTag) return

  // 检查是否重名（排除自身）
  const existingTag = tagList.value.find(t => t.name === trimmedName && t.id !== id)
  if (existingTag) {
    showToast(`标签名 "${trimmedName}" 已存在，请使用其他名称`, 'error')
    return
  }

  try {
    const response = await axios.put(`${API_BASE}/tags/${id}`, {
      name: trimmedName,
      rank: targetTag.rank
    })
    if (response.data.code === 200) {
      targetTag.name = trimmedName
      cancelEdit()
      showToast('修改成功', 'success')
    } else {
      showToast(response.data.message || '更新失败', 'error')
    }
  } catch (error) {
    console.error('更新标签失败:', error)
    const errorMsg = error.response?.data?.message || '更新标签失败'
    showToast(errorMsg, 'error')
  }
}

// 取消编辑
const cancelEdit = () => {
  editingId.value = null
  editName.value = ''
  originalName.value = ''
}

// 确认删除（弹出确认框）
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
      // 如果当前页没有数据了，跳到上一页
      if (paginatedTags.value.length === 0 && currentPage.value > 1) {
        currentPage.value--
      }
      closeDeleteModal()
      showToast('删除成功', 'success')
    } else {
      showToast(response.data.message || '删除失败', 'error')
      closeDeleteModal()
    }
  } catch (error) {
    console.error('删除标签失败:', error)
    const errorMsg = error.response?.data?.message || '删除标签失败'
    showToast(errorMsg, 'error')
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
  if (!trimmedName) {
    showToast('标签名不能为空', 'error')
    return
  }
  if (trimmedName.length > 20) {
    showToast('标签名不能超过20个字符', 'error')
    return
  }

  // 检查是否重名
  const existingTag = tagList.value.find(t => t.name === trimmedName)
  if (existingTag) {
    showToast(`标签名 "${trimmedName}" 已存在，请使用其他名称`, 'error')
    return
  }

  try {
    const response = await axios.post(`${API_BASE}/tags`, {
      name: trimmedName,
      rank: 0
    })
    if (response.data.code === 200) {
      tagList.value.push(response.data.data)
      // 跳转到最后一页（新增的标签在最后一页）
      currentPage.value = totalPages.value
      cancelAdd()
      showToast('创建成功', 'success')
    } else {
      showToast(response.data.message || '创建失败', 'error')
    }
  } catch (error) {
    console.error('创建标签失败:', error)
    const errorMsg = error.response?.data?.message || '创建标签失败'
    showToast(errorMsg, 'error')
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

// 页面加载时获取数据
onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.tags-container {
  max-width: 800px;
  margin: 0 auto;
  min-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

/* 头部 */
.tags-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a1a1a;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.5rem 1rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.add-btn:hover {
  background: #1d4ed8;
}

.plus-icon {
  font-size: 1.1rem;
  font-weight: bold;
}

/* 标签列表 */
.tags-list {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  flex: 1 0 auto;

}

.tag-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.tag-item:last-child {
  border-bottom: none;
}

.tag-item:hover {
  background: #fafbfc;
}

.tag-item.editing-mode {
  background: #fefce8;
}

.tag-item.add-mode {
  background: #eff6ff;
}

/* 标签信息区 */
.tag-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* 星星按钮 */
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
  font-size: 1.25rem;
  line-height: 1;
}

.star.filled {
  color: #fbbf24;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.star.empty {
  color: #d1d5db;
}

.star.empty:hover {
  color: #fbbf24;
}

.star-placeholder {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.tag-name {
  font-size: 1rem;
  color: #1a1a1a;
  word-break: break-all;
}

.tag-input {
  flex: 1;
  max-width: 300px;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
  outline: none;
  transition: border 0.2s;
}

.tag-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.char-count {
  font-size: 0.75rem;
  color: #9ca3af;
}

/* 操作按钮区 */
.tag-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}

.action-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s;
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

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  color: #9ca3af;
  font-size: 0.9rem;
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1.5rem;
  padding: 0.5rem;
}

.page-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.page-btn:hover:not(:disabled) {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 0.25rem;
}

.page-num {
  min-width: 32px;
  height: 32px;
  padding: 0 0.5rem;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.page-num:hover {
  background: #f3f4f6;
}

.page-num.active {
  background: #2563eb;
  border-color: #2563eb;
  color: white;
}

/* 弹窗样式 */
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
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-title {
  margin: 0 0 0.75rem 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.modal-message {
  margin: 0 0 1.25rem 0;
  color: #4b5563;
  line-height: 1.5;
}

.warning-text {
  font-size: 0.8rem;
  color: #ef4444;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.modal-btn {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-btn.cancel {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.modal-btn.cancel:hover {
  background: #e5e7eb;
}

.modal-btn.confirm {
  background: #ef4444;
  border: none;
  color: white;
}

.modal-btn.confirm:hover {
  background: #dc2626;
}

/* 提示消息样式 */
.toast {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-size: 0.9rem;
  z-index: 1100;
  animation: fadeInUp 0.3s ease;
  white-space: nowrap;
}
.toast.error {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.toast.success {
  background: #dcfce7;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}
</style>