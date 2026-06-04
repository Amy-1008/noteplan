<template>
  <div class="note-edit-container">
    <div class="note-edit-inner">
      <!-- 顶部导航 -->
      <div class="edit-header">
        <button class="back-btn" @click="goBack">← 返回</button>
        <div class="header-actions">
          <button class="save-btn" @click="saveNote" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>

      <!-- 笔记本主体 -->
      <div class="notebook">
        <!-- 左侧：日期栏 -->
        <div class="date-column">
          <div class="date-display">
            <div class="date-number">{{ currentDay }}</div>
            <div class="date-detail">
              <div class="date-month">{{ currentMonth }}</div>
              <div class="date-year">{{ currentYear }}</div>
              <div class="date-weekday">{{ currentWeekday }}</div>
            </div>
          </div>
        </div>

        <!-- 右侧：内容区域 -->
        <div class="content-column">
          <div class="content-header">
            <input
                v-model="form.title"
                type="text"
                class="title-input"
                placeholder="标题"
            />
          </div>

          <div class="content-body">
            <textarea
                v-model="form.content"
                class="content-textarea"
                placeholder="开始写点什么..."
                rows="20"
            ></textarea>
          </div>

          <div class="content-footer">
            <div class="tag-section">
              <span class="tag-label">标签</span>
              <TagSelector v-model="form.tagId" @tag-created="handleTagCreated" />
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 版本历史区域（新增） ========== -->
      <div v-if="isEdit" class="version-section">
        <div class="version-header" @click="showVersions = !showVersions">
          <span>📜 版本历史</span>
          <span class="version-toggle">{{ showVersions ? '收起' : '展开' }}</span>
        </div>

        <div v-if="showVersions" class="version-list">
          <div v-for="ver in versionList" :key="ver.versionNo" class="version-item">
            <div class="version-info">
              <span class="version-no">版本 {{ ver.versionNo }}</span>
              <span class="version-time">{{ formatDate(ver.saveTime) }}</span>
            </div>
            <div class="version-actions">
              <button class="preview-btn" @click="previewVersion(ver)">预览</button>
              <button class="recover-btn" @click="handleRecoverVersion(ver.versionNo)">恢复</button>
            </div>
          </div>
          <div v-if="versionList.length === 0" class="empty-state">暂无历史版本</div>
        </div>
      </div>

      <!-- ========== 版本预览弹窗 ========== -->
      <div v-if="previewVisible" class="modal-overlay" @click.self="closePreview">
        <div class="modal-content">
          <div class="modal-header">
            <h3 class="modal-title">版本预览</h3>
            <button class="modal-close" @click="closePreview">✕</button>
          </div>
          <div class="modal-body">
            <div class="preview-header">
              <span class="preview-title">{{ previewTitle }}</span>
              <span v-if="previewTagName" class="tag-display">
                <span class="tag-dot" :style="{ backgroundColor: getTagColor(previewTagName) }"></span>
                {{ previewTagName }}
              </span>
            </div>
            <div class="preview-content">{{ previewContent }}</div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="closePreview">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addNote, updateNote, getNoteById, getNoteVersions, recoverVersion } from '@/api/note'
import TagSelector from '@/components/TagSelector.vue'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const saving = ref(false)
const tagList = ref([])
const showVersions = ref(false)
const versionList = ref([])
const previewVisible = ref(false)
const previewTitle = ref('')
const previewContent = ref('')
const previewTagName = ref('')

// 判断是否是编辑模式
const isEdit = computed(() => !!route.params.id)
const noteId = computed(() => route.params.id ? parseInt(route.params.id) : null)

// 表单数据
const form = reactive({
  id: null,
  title: '',
  content: '',
  tagId: null
})

// 当前日期
const now = new Date()
const currentDay = now.getDate()
const currentMonth = (now.getMonth() + 1) + '月'
const currentYear = now.getFullYear()
const currentWeekday = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.getDay()]

// 获取标签列表
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

// 加载笔记数据（编辑模式）
const loadNote = async () => {
  if (!isEdit.value) return

  try {
    const res = await getNoteById(noteId.value)
    if (res.data.code === 200) {
      const note = res.data.data
      form.id = note.id
      form.title = note.title || ''
      form.content = note.content || ''

      // 加载标签
      const tagRes = await axios.get('http://localhost:8080/api/tags/target', {
        params: { targetId: note.id, targetType: 'NOTE' }
      })
      if (tagRes.data.code === 200 && tagRes.data.data) {
        form.tagId = tagRes.data.data.id
      }

      // 加载版本列表
      const verRes = await getNoteVersions(note.id)
      if (verRes.data.code === 200) {
        versionList.value = verRes.data.data || []
      }
    } else {
      ElMessage.error('加载笔记失败')
      goBack()
    }
  } catch (err) {
    ElMessage.error('加载笔记失败')
    goBack()
  }
}

// 保存笔记
const saveNote = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('内容不能为空')
    return
  }

  saving.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateNote({
        id: form.id,
        title: form.title,
        content: form.content
      })
    } else {
      res = await addNote({
        title: form.title,
        content: form.content
      })
    }

    if (res.data.code === 200) {
      const savedNote = res.data.data

      // 绑定标签
      if (form.tagId) {
        await axios.post('http://localhost:8080/api/tags/bind', null, {
          params: {
            targetId: savedNote.id,
            targetType: 'NOTE',
            tagId: form.tagId
          }
        })
      }

      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      goBack()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (err) {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 返回首页
const goBack = () => {
  router.push('/')
}

const handleTagCreated = (newTag) => {
  fetchTags()
}

// 预览版本
const previewVersion = (ver) => {
  previewContent.value = ver.content
  previewTitle.value = ver.title || '无标题'
  if (ver.tagId) {
    const tag = tagList.value.find(t => t.id === ver.tagId)
    previewTagName.value = tag ? tag.name : ''
  } else {
    previewTagName.value = ''
  }
  previewVisible.value = true
}

const closePreview = () => {
  previewVisible.value = false
}

// 恢复版本
const handleRecoverVersion = async (versionNo) => {
  try {
    const res = await recoverVersion(noteId.value, versionNo)
    if (res.data.code === 200) {
      ElMessage.success('恢复成功')
      loadNote()  // 重新加载笔记和版本列表
    } else {
      ElMessage.error(res.data.message || '恢复失败')
    }
  } catch (err) {
    ElMessage.error('恢复失败')
  }
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

onMounted(() => {
  fetchTags()
  loadNote()
})
</script>
<style scoped>
.note-edit-container {
  padding: 24px;
  height: 100%;
  display: flex;
  justify-content: center;
  background: var(--bg-primary);
  transition: background 0.3s;
}

.note-edit-inner {
  width: 100%;
  max-width: 820px;
}

/* 顶部导航 */
.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.back-btn {
  background: none;
  border: none;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  transition: 0.2s;
}

.back-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.save-btn {
  padding: 6px 20px;
  background: var(--accent);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: 0.2s;
}

.save-btn:hover {
  background: #3b7adf;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 笔记本主体 */
.notebook {
  display: flex;
  background: var(--card-bg);
  border-radius: 14px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
  overflow: hidden;
  min-height: 600px;
  transition: background 0.3s, border-color 0.3s;
}

/* 左侧日期栏 */
.date-column {
  width: 140px;
  padding: 32px 20px;
  border-right: 1px solid var(--card-border);
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.date-display {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.date-number {
  font-size: 48px;
  font-weight: 300;
  color: var(--text-primary);
  line-height: 1;
}

.date-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.date-month {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.date-year {
  font-size: 14px;
  color: var(--text-secondary);
}

.date-weekday {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 右侧内容栏 */
.content-column {
  flex: 1;
  padding: 32px 40px;
  display: flex;
  flex-direction: column;
}

.content-header {
  margin-bottom: 16px;
}

.title-input {
  width: 100%;
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  border: none;
  outline: none;
  background: transparent;
  padding: 0;
}

.title-input::placeholder {
  color: var(--text-secondary);
}

.content-body {
  flex: 1;
}

.content-textarea {
  width: 100%;
  height: 100%;
  min-height: 400px;
  border: none;
  outline: none;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
  resize: vertical;
  background: transparent;
  font-family: inherit;
}

.content-textarea::placeholder {
  color: var(--text-secondary);
}

.content-footer {
  padding-top: 16px;
  border-top: 1px solid var(--card-border);
}

.tag-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tag-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 版本历史区域 */
.version-section {
  margin-top: 24px;
  background: var(--card-bg);
  border-radius: 14px;
  border: 1px solid var(--card-border);
  overflow: hidden;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  background: var(--bg-hover);
  transition: 0.2s;
}

.version-header:hover {
  background: var(--border-color);
}

.version-header span {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.version-toggle {
  font-size: 13px;
  color: var(--text-secondary);
}

.version-list {
  padding: 16px;
}

.version-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--card-border);
}

.version-item:last-child {
  border-bottom: none;
}

.version-info {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.version-no {
  font-weight: 500;
  color: var(--text-primary);
}

.version-actions {
  display: flex;
  gap: 8px;
}

.preview-btn {
  padding: 4px 12px;
  background: var(--bg-hover);
  color: var(--text-secondary);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
}

.preview-btn:hover {
  background: var(--border-color);
}

.recover-btn {
  padding: 4px 12px;
  background: #dbeafe;
  color: #2563eb;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
}

.recover-btn:hover {
  background: #bfdbfe;
}

.empty-state {
  text-align: center;
  padding: 20px;
  color: var(--empty-text);
}

/* ========== 预览弹窗 ========== */
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
  background: var(--card-bg);
  border-radius: 14px;
  width: 600px;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--card-border);
}

.modal-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.modal-close {
  background: none;
  border: none;
  font-size: 20px;
  color: var(--text-secondary);
  cursor: pointer;
}

.modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.preview-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.preview-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--text-primary);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid var(--card-border);
}

.btn-cancel {
  padding: 6px 16px;
  background: var(--bg-hover);
  color: var(--text-secondary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.tag-display {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--tag-bg);
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: var(--tag-text);
}

.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}
</style>