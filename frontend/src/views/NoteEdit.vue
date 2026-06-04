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
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addNote, updateNote, getNoteById } from '@/api/note'
import TagSelector from '@/components/TagSelector.vue'
import axios from 'axios'
import {useNoteStore} from "@/store/note.js";
const store = useNoteStore()
const router = useRouter()
const route = useRoute()
const saving = ref(false)
const tagList = ref([])

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
      store.triggerSidebarRefresh()
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
  background: #fafafa;
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
  color: #6b7280;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  transition: 0.2s;
}

.back-btn:hover {
  background: #f3f4f6;
  color: #1a1a1a;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.save-btn {
  padding: 6px 20px;
  background: #4f8cff;
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
  background: white;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  overflow: hidden;
  min-height: 600px;
}

/* 左侧日期栏 */
.date-column {
  width: 140px;
  padding: 32px 20px;
  border-right: 1px solid #f0f0f0;
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
  color: #1a1a1a;
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
  color: #6b7280;
}

.date-year {
  font-size: 14px;
  color: #6b7280;
}

.date-weekday {
  font-size: 12px;
  color: #9ca3af;
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
  color: #1a1a1a;
  border: none;
  outline: none;
  background: transparent;
  padding: 0;
}

.title-input::placeholder {
  color: #9ca3af;
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
  color: #374151;
  resize: vertical;
  background: transparent;
  font-family: inherit;
}

.content-textarea::placeholder {
  color: #9ca3af;
}

.content-footer {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.tag-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tag-label {
  font-size: 13px;
  color: #6b7280;
}
</style>