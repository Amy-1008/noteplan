<template>
  <div class="page-container">
    <div class="page-inner">
      <!-- 顶部导航 -->
      <div class="page-header">
        <h2 class="page-title">{{ isEdit ? '编辑日程' : '新建日程' }}</h2>
        <div class="header-actions">
          <button class="btn-cancel" @click="goBack">取消</button>
          <button class="btn-confirm" @click="saveSchedule" :disabled="saving">
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
                v-model="formData.title"
                type="text"
                class="title-input"
                placeholder="标题"
            />
          </div>

          <div class="time-section">
            <div class="time-type">
              <span class="label">时间</span>
              <el-radio-group v-model="formData.timeType" class="radio-group">
                <el-radio value="point">点</el-radio>
                <el-radio value="period">段</el-radio>
              </el-radio-group>
            </div>

            <div class="time-picker">
              <el-date-picker
                  v-if="formData.timeType === 'point'"
                  v-model="formData.endTime"
                  type="datetime"
                  placeholder="选择时间"
                  style="width: 100%"
                  @change="handleEndTimeChange"
              />
              <div v-else class="period-picker">
                <el-date-picker
                    v-model="formData.startTime"
                    type="datetime"
                    placeholder="开始"
                    style="flex: 1"
                    @change="handleStartTimeChange"
                />
                <span class="time-separator">→</span>
                <el-date-picker
                    v-model="formData.endTime"
                    type="datetime"
                    placeholder="结束"
                    style="flex: 1"
                    @change="handleEndTimeChangeForPeriod"
                />
              </div>
            </div>
          </div>

          <div class="repeat-section">
            <span class="label">重复</span>
            <el-select v-model="formData.repeatRule" class="repeat-select">
              <el-option label="不重复" value="none" />
              <el-option label="每天" value="daily" />
              <el-option label="每周" value="weekly" />
              <el-option label="每月" value="monthly" />
              <el-option label="每年" value="yearly" />
              <el-option label="工作日" value="workday" />
              <el-option label="节假日" value="holiday" />
            </el-select>
          </div>

          <div class="remark-section">
            <span class="label">备注</span>
            <textarea
                v-model="formData.remark"
                class="remark-input"
                placeholder="备注"
                rows="4"
            ></textarea>
          </div>

          <div class="tag-section">
            <span class="label">标签</span>
            <TagSelector v-model="formData.tagId" @tag-created="handleTagCreated" />
          </div>

          <div class="note-section">
            <span class="label">关联笔记</span>
            <div class="notes-display">
              <div class="notes-list">
                <span v-for="note in selectedNotes" :key="note.id" class="note-tag">
                  {{ note.title }}
                  <button class="remove-tag" @click="removeNote(note.id)">×</button>
                </span>
                <span v-if="selectedNotes.length === 0" class="placeholder-text">未关联笔记</span>
              </div>
              <button class="btn-cancel" @click="openNoteSelector">+ 选择笔记</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 笔记选择器弹窗 -->
    <el-dialog v-model="noteDialogVisible" title="选择关联笔记" width="600px" append-to-body>
      <div class="note-selector">
        <div class="note-search-bar">
          <el-input v-model="noteSearchKeyword" placeholder="按标题搜索" clearable prefix-icon="Search" style="width: 200px" />
          <el-select v-model="noteFilterTagId" placeholder="按标签筛选" clearable style="width: 150px">
            <el-option v-for="tag in tagList" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </div>
        <div class="note-list-selector">
          <div v-for="note in filteredNoteList" :key="note.id" class="note-item-selector" @click="toggleNoteSelection(note.id)">
            <el-checkbox :model-value="tempSelectedNoteIds.includes(note.id)" @click.stop @change="toggleNoteSelection(note.id)" />
            <div class="note-info">
              <span class="note-title">{{ note.title || '无标题' }}</span>
              <span v-if="note.tagName" class="note-tag-name">#{{ note.tagName }}</span>
            </div>
            <el-button text @click.stop="viewNoteDetail(note)"><el-icon><Document /></el-icon> 查看</el-button>
          </div>
          <el-empty v-if="filteredNoteList.length === 0" description="暂无笔记" />
        </div>
      </div>
      <template #footer>
        <el-button @click="noteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmNoteSelection">确定</el-button>
      </template>
    </el-dialog>

    <!-- 笔记详情查看弹窗 -->
    <el-dialog v-model="viewNoteDialogVisible" :title="currentViewNote?.title || '笔记详情'" width="500px" append-to-body>
      <div class="note-view-content">
        <div class="note-view-meta">
          <span>更新于：{{ formatDate(currentViewNote?.updateTime) }}</span>
          <el-tag v-if="currentViewNote?.tagName" size="small">{{ currentViewNote.tagName }}</el-tag>
        </div>
        <el-divider />
        <div class="note-view-body">{{ currentViewNote?.content }}</div>
      </div>
      <template #footer>
        <el-button @click="viewNoteDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Document, Search } from '@element-plus/icons-vue'
import TagSelector from '@/components/TagSelector.vue'
import axios from 'axios'
import { useNoteStore } from '@/store/note'

const store = useNoteStore()
const router = useRouter()
const route = useRoute()
const saving = ref(false)
const formRef = ref(null)

const scheduleId = ref(route.query.id)
const isEdit = computed(() => !!scheduleId.value)

// 当前日期
const now = new Date()
const currentDay = now.getDate()
const currentMonth = (now.getMonth() + 1) + '月'
const currentYear = now.getFullYear()
const currentWeekday = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.getDay()]

// 表单数据
const formData = ref({
  id: null,
  title: '',
  timeType: 'point',
  startTime: '',
  endTime: '',
  repeatRule: 'none',
  remark: '',
  tagId: null,
  noteIds: []
})

// 表单校验规则（与新增一致）
const formRules = {
  title: [
    { required: true, message: '请输入日程标题', trigger: 'blur' },
    { max: 20, message: '标题不能超过20个字符', trigger: 'blur' }
  ],
  endTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
  startTime: [{
    required: true,
    message: '请选择开始时间',
    trigger: 'change',
    validator: (rule, value, callback) => {
      if (formData.value.timeType === 'period' && !value) {
        callback(new Error('请选择开始时间'))
      } else {
        callback()
      }
    }
  }],
  remark: [
    { max: 800, message: '备注不能超过800个字符', trigger: 'blur' }
  ]
}

// ---------- 时间辅助函数 ----------
const getOneHourLater = () => {
  const date = new Date()
  date.setHours(date.getHours() + 1)
  return date
}

const formatDateTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

const getDefaultTime = () => {
  return formatDateTime(getOneHourLater())
}

const handleEndTimeChange = (val) => {
  if (val && formData.value.timeType === 'point') {
    const now = new Date()
    const selectedDate = new Date(val)
    selectedDate.setHours(now.getHours() + 1, now.getMinutes(), now.getSeconds())
    formData.value.endTime = formatDateTime(selectedDate)
  }
}

const handleStartTimeChange = (val) => {
  if (val && formData.value.timeType === 'period') {
    const start = new Date(val)
    const end = formData.value.endTime ? new Date(formData.value.endTime) : null

    if (!end || end <= start) {
      const autoEnd = new Date(start.getTime() + 60 * 60 * 1000)
      formData.value.endTime = formatDateTime(autoEnd)
      ElMessage.info('结束时间已自动调整为开始时间后1小时')
    }
  }
}

const handleEndTimeChangeForPeriod = (val) => {
  if (val && formData.value.timeType === 'period') {
    const end = new Date(val)
    const start = formData.value.startTime ? new Date(formData.value.startTime) : null

    if (start && end <= start) {
      const autoEnd = new Date(start.getTime() + 60 * 60 * 1000)
      formData.value.endTime = formatDateTime(autoEnd)
      ElMessage.warning('结束时间不能早于或等于开始时间，已自动调整为开始时间后1小时')
    }
  }
}

// ---------- 笔记选择器相关 ----------
const selectedNotes = ref([])
const noteSearchKeyword = ref('')
const noteFilterTagId = ref(null)
const noteDialogVisible = ref(false)
const tempSelectedNoteIds = ref([])
const viewNoteDialogVisible = ref(false)
const currentViewNote = ref(null)
const tagList = ref([])
const noteList = ref([])

const filteredNoteList = computed(() => {
  let result = [...noteList.value]

  if (noteSearchKeyword.value.trim()) {
    const keyword = noteSearchKeyword.value.trim().toLowerCase()
    result = result.filter(note =>
        note.title?.toLowerCase().includes(keyword)
    )
  }

  if (noteFilterTagId.value) {
    result = result.filter(note => note.tagId === noteFilterTagId.value)
  }

  return result
})

const toggleNoteSelection = (noteId) => {
  const index = tempSelectedNoteIds.value.indexOf(noteId)
  if (index > -1) {
    tempSelectedNoteIds.value.splice(index, 1)
  } else {
    tempSelectedNoteIds.value.push(noteId)
  }
}

const openNoteSelector = () => {
  tempSelectedNoteIds.value = [...formData.value.noteIds]
  noteSearchKeyword.value = ''
  noteFilterTagId.value = null
  noteDialogVisible.value = true
}

const confirmNoteSelection = () => {
  selectedNotes.value = noteList.value.filter(n => tempSelectedNoteIds.value.includes(n.id))
  formData.value.noteIds = tempSelectedNoteIds.value
  noteDialogVisible.value = false
}

const removeNote = (noteId) => {
  selectedNotes.value = selectedNotes.value.filter(n => n.id !== noteId)
  formData.value.noteIds = selectedNotes.value.map(n => n.id)
}

const viewNoteDetail = (note) => {
  currentViewNote.value = note
  viewNoteDialogVisible.value = true
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// ---------- API ----------
const fetchTagList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/tags')
    if (response.data.code === 200) {
      tagList.value = response.data.data
    }
  } catch (error) {
    console.error('获取标签失败', error)
  }
}

const fetchNoteList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/note/list')
    if (response.data.code === 200) {
      const notes = response.data.data || []
      for (const note of notes) {
        const tagRes = await axios.get('http://localhost:8080/api/tags/target', {
          params: { targetId: note.id, targetType: 'NOTE' }
        })
        if (tagRes.data.code === 200 && tagRes.data.data) {
          note.tagName = tagRes.data.data.name
          note.tagId = tagRes.data.data.id
        }
      }
      noteList.value = notes
    }
  } catch (error) {
    console.error('获取笔记失败', error)
  }
}

// 加载日程详情
const fetchScheduleDetail = async () => {
  if (!scheduleId.value) return

  try {
    const response = await axios.get('http://localhost:8080/api/schedule/detail', {
      params: { id: scheduleId.value }
    })
    if (response.data.code === 200) {
      const data = response.data.data
      formData.value.id = data.id
      formData.value.title = data.title || ''
      formData.value.repeatRule = data.repeatRule || 'none'
      formData.value.remark = data.remark || ''
      formData.value.tagId = data.tagId || null

      if (data.noteIds && data.noteIds.length > 0) {
        await fetchNoteList()
        selectedNotes.value = noteList.value.filter(n => data.noteIds.includes(n.id))
        formData.value.noteIds = data.noteIds
      }

      if (!data.startTime) {
        formData.value.timeType = 'point'
        formData.value.endTime = data.endTime
      } else {
        formData.value.timeType = 'period'
        formData.value.startTime = data.startTime
        formData.value.endTime = data.endTime
      }
    }
  } catch (error) {
    console.error('获取日程详情失败', error)
    ElMessage.error('加载失败')
  }
}

// 保存日程
const saveSchedule = async () => {
  if (!formData.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  saving.value = true
  try {
    const submitData = {
      id: formData.value.id,
      title: formData.value.title,
      repeatRule: formData.value.repeatRule,
      remark: formData.value.remark,
      tagId: formData.value.tagId,
      noteIds: formData.value.noteIds
    }

    if (formData.value.timeType === 'point') {
      submitData.startTime = null
      submitData.endTime = formData.value.endTime
    } else {
      if (!formData.value.startTime || !formData.value.endTime) {
        ElMessage.warning('请选择完整的时间段')
        saving.value = false
        return
      }
      submitData.startTime = formData.value.startTime
      submitData.endTime = formData.value.endTime
    }

    const response = await axios.put('http://localhost:8080/api/schedule/update', submitData)

    if (response.data.code === 200) {
      ElMessage.success('保存成功')
      goBack()
    } else {
      ElMessage.error(response.data.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 返回日程列表
const goBack = () => {
  router.push('/schedules')
}

const handleTagCreated = (newTag) => {
  fetchTagList()
}
// 监听时间类型变化
watch(() => formData.value.timeType, (newVal) => {
  const defaultTime = getDefaultTime()

  if (newVal === 'point') {
    formData.value.startTime = ''
    formData.value.endTime = defaultTime
  } else {
    const defaultStart = new Date()
    const defaultEnd = new Date(defaultStart.getTime() + 60 * 60 * 1000)
    formData.value.startTime = formatDateTime(defaultStart)
    formData.value.endTime = formatDateTime(defaultEnd)
    ElMessage.info('已自动将结束时间设置为开始后一小时')
  }
})
onMounted(() => {
  fetchTagList()
  fetchNoteList()
  fetchScheduleDetail()
})
</script>

<style scoped>
.detail-container {
  padding: 24px 32px;
  max-width: 680px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
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

.save-btn {
  padding: 6px 20px;
  background: var(--accent);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: 0.2s;
}

.save-btn:hover {
  background: #3b7adf;
}

.detail-form {
  background: var(--card-bg);
  border-radius: 14px;
  padding: 24px;
  border: 1px solid var(--card-border);
  box-shadow: var(--card-shadow);
  transition: background 0.3s, border-color 0.3s;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--card-border);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  background: var(--input-bg);
  color: var(--text-primary);
  transition: background 0.3s, border-color 0.3s;
}

.form-input:focus {
  border-color: var(--accent);
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--card-border);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
  background: var(--input-bg);
  color: var(--text-primary);
  transition: background 0.3s, border-color 0.3s;
}

.form-textarea:focus {
  border-color: var(--accent);
}

.notes-display {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notes-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.note-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--tag-bg);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  color: var(--tag-text);
}

.remove-tag {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0 4px;
  font-size: 14px;
  transition: 0.2s;
}

.remove-tag:hover {
  color: #ef4444;
}

.placeholder-text {
  color: var(--text-secondary);
  font-size: 13px;
}

.btn-cancel {
  padding: 4px 12px;
  background: var(--bg-hover);
  color: var(--text-secondary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: 0.2s;
}

.btn-cancel:hover {
  background: var(--border-color);
}

/* 笔记本风格的表单 */
.title-input {
  width: 100%;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  border: none;
  outline: none;
  padding: 0 0 12px 0;
  background: transparent;
  border-bottom: 1px solid var(--card-border);
  margin-bottom: 20px;
}

.title-input::placeholder {
  color: var(--text-secondary);
}

.title-input:focus {
  border-bottom-color: var(--accent);
}

.time-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.time-type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.radio-group {
  display: flex;
  gap: 4px;
}

.time-picker {
  flex: 1;
}

.period-picker {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-separator {
  color: var(--text-secondary);
  font-size: 16px;
}

.repeat-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.repeat-select {
  flex: 1;
}

.remark-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.remark-input {
  flex: 1;
  width: 100%;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
  border: none;
  outline: none;
  padding: 0 0 12px 0;
  background: transparent;
  border-bottom: 1px solid var(--card-border);
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
}

.remark-input::placeholder {
  color: var(--text-secondary);
}

.remark-input:focus {
  border-bottom-color: var(--accent);
}

.tag-section, .note-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  min-width: 50px;
  padding-top: 4px;
}
</style>