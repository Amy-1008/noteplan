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
        <div class="content-column">
          <div class="content-header">
            <el-input v-model="formData.title" class="title-input" placeholder="标题" maxlength="20" show-word-limit/>
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
              <!-- 时间点模式 -->
              <el-date-picker
                  v-if="formData.timeType === 'point'"
                  v-model="formData.endTime"
                  type="datetime"
                  placeholder="选择时间"
                  style="width: 100%"
                  @change="handleEndTimeChange"
              />
              <!-- 时间段模式 -->
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
            <el-input v-model="formData.remark" type="textarea" class="remark-input" placeholder="备注" rows="4" maxlength="800" show-word-limit/>
          </div>

          <div class="tag-section">
            <span class="label">标签</span>
            <TagSelector v-model="formData.tagId" @tag-created="handleTagCreated" />
          </div>

          <div class="note-section">
            <span class="label">关联笔记</span>
            <div class="notes-display">
              <div class="notes-list">
                <span
                    v-for="note in selectedNotes"
                    :key="note.id"
                    class="note-tag clickable"
                    @click="goToNoteDetail(note.id)"
                >
                  {{ note.title }}
                  <button class="remove-tag" @click.stop="removeNote(note.id)">×</button>
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

// 时间辅助函数
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

const handleEndTimeChange = (val) => {
  if (val && formData.value.timeType === 'point') {
    formData.value.endTime = formatDateTime(new Date(val))
  }
}

const handleStartTimeChange = (val) => {
  if (val && formData.value.timeType === 'period') {
    const start = new Date(val)
    let end = formData.value.endTime ? new Date(formData.value.endTime) : null

    if (!end) {
      const autoEnd = new Date(start.getTime() + 60 * 60 * 1000)
      formData.value.endTime = formatDateTime(autoEnd)
      formData.value.startTime = formatDateTime(start)
      ElMessage.info('结束时间已自动设置为开始时间后1小时')
      return
    }

    formData.value.startTime = formatDateTime(start)

    if (end <= start) {
      const autoEnd = new Date(start.getTime() + 60 * 60 * 1000)
      formData.value.endTime = formatDateTime(autoEnd)
      ElMessage.info('结束时间已自动调整为开始时间后1小时')
    }
  }
}

const handleEndTimeChangeForPeriod = (val) => {
  if (val && formData.value.timeType === 'period') {
    const end = new Date(val)
    let start = formData.value.startTime ? new Date(formData.value.startTime) : null

    if (!start) {
      const autoStart = new Date(end.getTime() - 60 * 60 * 1000)
      formData.value.startTime = formatDateTime(autoStart)
      formData.value.endTime = formatDateTime(end)
      ElMessage.info('开始时间已自动设置为结束时间前1小时')
      return
    }

    formData.value.endTime = formatDateTime(end)

    if (end <= start) {
      const autoStart = new Date(end.getTime() - 60 * 60 * 1000)
      formData.value.startTime = formatDateTime(autoStart)
      ElMessage.info('开始时间已自动调整为结束时间前1小时')
    }
  }
}
// 笔记选择器相关
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

// API
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
  if (!scheduleId.value) {
    isInitializing.value = false
    return
  }

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

      // 设置时间类型
      if (!data.startTime) {
        formData.value.timeType = 'point'
        formData.value.endTime = data.endTime
        formData.value.startTime = ''
      } else {
        formData.value.timeType = 'period'
        formData.value.startTime = data.startTime
        formData.value.endTime = data.endTime
      }
    }
  } catch (error) {
    console.error('获取日程详情失败', error)
    ElMessage.error('加载失败')
  } finally {
    setTimeout(() => {
      isInitializing.value = false
    }, 100)
  }
}

// 校验并调整重复规则的时间
const validateAndAdjustRepeatTime = (repeatRule, time) => {
  if (!time || repeatRule === 'none') return time

  let newDate = new Date(time)
  let adjusted = false

  if (repeatRule === 'workday') {
    while (newDate.getDay() === 0 || newDate.getDay() === 6) {
      newDate.setDate(newDate.getDate() + 1)
      adjusted = true
    }
  } else if (repeatRule === 'holiday') {
    while (newDate.getDay() !== 0 && newDate.getDay() !== 6) {
      newDate.setDate(newDate.getDate() + 1)
      adjusted = true
    }
  }

  if (adjusted) {
    ElMessage.warning(`时间已自动调整为最近的${repeatRule === 'workday' ? '工作日' : '节假日'}：${newDate.toLocaleString()}`)
  }

  return formatDateTime(newDate)
}

// 保存日程
const saveSchedule = async () => {
  if (!formData.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  saving.value = true
  try {
    let startTime = formData.value.startTime
    let endTime = formData.value.endTime
    const repeatRule = formData.value.repeatRule

    // 时间段模式的完整性校验
    if (formData.value.timeType === 'period') {
      const start = startTime ? new Date(startTime) : null
      const end = endTime ? new Date(endTime) : null

      if (!start || !end) {
        ElMessage.warning('请选择完整的时间段')
        saving.value = false
        return
      }
      if (end <= start) {
        ElMessage.warning('结束时间不能早于或等于开始时间')
        saving.value = false
        return
      }
    }

    if (formData.value.timeType === 'point' && !endTime) {
      ElMessage.warning('请选择时间')
      saving.value = false
      return
    }

    if (formData.value.timeType === 'point' && endTime) {
      endTime = validateAndAdjustRepeatTime(repeatRule, endTime)
    } else if (formData.value.timeType === 'period') {
      if (startTime) startTime = validateAndAdjustRepeatTime(repeatRule, startTime)
      if (endTime) endTime = validateAndAdjustRepeatTime(repeatRule, endTime)
    }

    const submitData = {
      id: formData.value.id,
      title: formData.value.title,
      repeatRule: repeatRule,
      remark: formData.value.remark,
      tagId: formData.value.tagId,
      noteIds: formData.value.noteIds,
      startTime: formData.value.timeType === 'point' ? null : (startTime || null),
      endTime: endTime || null
    }

    const response = await axios.put('http://localhost:8080/api/schedule/update', submitData)

    if (response.data.code === 200) {
      ElMessage.success('保存成功')
      store.triggerSidebarRefresh()
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

// 跳转到笔记详情页
const goToNoteDetail = (noteId) => {
  if (noteId) {
    sessionStorage.setItem('returnToSchedule', scheduleId.value)
    sessionStorage.setItem('fromSchedule', 'true')
    router.push({ path: `/notes/edit/${noteId}`, query: { from: 'schedule' } })
  }
}

// 返回日程列表页
const goBack = () => {
  router.push('/schedules')
}

const handleTagCreated = (newTag) => {
  fetchTagList()
}

// 添加标志位
const isInitializing = ref(true)

watch(() => formData.value.timeType, (newVal, oldVal) => {
  if (isInitializing.value) return
  if (oldVal === newVal) return

  if (newVal === 'point') {
    // 切换到时间点：清空开始时间，保留结束时间
    formData.value.startTime = ''
  } else {
    // 切换到时间段
    // 如果有结束时间但没有开始时间，自动生成开始时间
    if (formData.value.endTime && !formData.value.startTime) {
      const endDate = new Date(formData.value.endTime)
      const autoStart = new Date(endDate.getTime() - 60 * 60 * 1000)
      formData.value.startTime = formatDateTime(autoStart)
    }
    // 如果完全没有时间，才设置默认值
    else if (!formData.value.startTime && !formData.value.endTime) {
      const defaultStart = new Date()
      const defaultEnd = new Date(defaultStart.getTime() + 60 * 60 * 1000)
      formData.value.startTime = formatDateTime(defaultStart)
      formData.value.endTime = formatDateTime(defaultEnd)
    }
  }
})

onMounted(async () => {
  isInitializing.value = true  // 开始初始化
  await fetchTagList()
  await fetchNoteList()
  await fetchScheduleDetail()
})
</script>

<style scoped>
.page-container {
  padding: 24px 32px;
  max-width: 900px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
}

.page-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 顶部导航 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-cancel, .btn-confirm {
  padding: 6px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: 0.2s;
  border: none;
}

.btn-cancel {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.btn-cancel:hover {
  background: var(--border-color);
}

.btn-confirm {
  background: #fbbf24;
  color: #1a1a1a;
}

.btn-confirm:hover {
  background: #f59e0b;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 笔记本主体 */
.notebook {
  background: var(--card-bg);
  border-radius: 14px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
  overflow: hidden;
  transition: background 0.3s, border-color 0.3s;
}

.content-column {
  padding: 32px 40px;
  display: flex;
  flex-direction: column;
}

.content-header {
  margin-bottom: 24px;
}

.title-input {
  width: 100%;
}

.title-input :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  padding: 0 0 12px 0;
  border-bottom: 1px solid var(--card-border);
  border-radius: 0;
}

.title-input :deep(.el-input__inner) {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  height: auto;
  padding: 0;
}

.title-input :deep(.el-input__wrapper:hover) {
  box-shadow: none;
}

.title-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: none;
  border-bottom-color: var(--accent);
}

/* 表单各区域 */
.time-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.time-type {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.radio-group {
  display: flex;
  gap: 4px;
}

.time-picker {
  flex: 1;
  min-width: 280px;
}

.period-picker {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.time-separator {
  color: var(--text-secondary);
  font-size: 16px;
  flex-shrink: 0;
}

.repeat-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.repeat-select {
  flex: 1;
  min-width: 200px;
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
}

.tag-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.note-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  min-width: 50px;
  padding-top: 4px;
  flex-shrink: 0;
}

/* 关联笔记按钮样式 */
.note-section .btn-cancel {
  padding: 4px 12px;
  font-size: 12px;
}

/* 关联笔记显示区域 */
.notes-display {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.notes-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.note-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #e0e7ff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  color: #1e40af;
}

.note-tag.clickable {
  cursor: pointer;
}

.note-tag.clickable:hover {
  transform: scale(1.02);
  opacity: 0.85;
}

.remove-tag {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 0 4px;
  font-size: 14px;
  border-radius: 50%;
}

.remove-tag:hover {
  color: #ef4444;
}

.placeholder-text {
  color: var(--text-secondary);
  font-size: 13px;
}

/* 笔记选择器弹窗样式 */
.note-selector {
  padding: 8px 0;
}

.note-search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--card-border);
  flex-wrap: wrap;
}

.note-list-selector {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
}

.note-item-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--bg-hover);
  border-radius: 32px;
  cursor: pointer;
  border: 1px solid var(--card-border);
  flex-wrap: wrap;
  gap: 8px;
}

.note-item-selector:hover {
  background: var(--border-color);
}

.note-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.note-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.note-tag-name {
  font-size: 11px;
  padding: 2px 10px;
  background: #e0e7ff;
  color: #4338ca;
  border-radius: 16px;
}

/* 笔记详情弹窗 */
.note-view-content {
  max-height: 400px;
  overflow-y: auto;
}

.note-view-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--text-secondary);
  font-size: 12px;
  margin-bottom: 8px;
}

.note-view-body {
  white-space: pre-wrap;
  line-height: 1.6;
  color: var(--text-primary);
}
</style>