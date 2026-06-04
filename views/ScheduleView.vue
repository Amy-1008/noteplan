<template>
  <div class="schedule-container">
    <!-- 左侧：标签侧边栏组件 -->
    <TagSidebar v-model="currentTag" @change="onTagChange" />

    <!-- 右侧主内容区 -->
    <div class="schedule-main">
      <!-- 头部 -->
      <div class="schedule-header">
        <h2>日程</h2>
        <div class="header-actions">
          <button
              class="action-btn"
              :class="{ active: deleteMode }"
              @click="toggleDeleteMode"
          >
            {{ deleteMode ? '取消选择' : '批量删除' }}
          </button>
          <button class="create-btn" @click="toggleAddForm">
            {{ isAdding ? '取消' : '+ 新建日程' }}
          </button>
        </div>
      </div>

      <!-- 删除模式提示 -->
      <div v-if="deleteMode" class="delete-mode-bar">
        <span>已选择 {{ selectedIds.length }} 个日程</span>
        <div class="delete-mode-actions">
          <button class="btn-cancel" @click="cancelDelete">取消</button>
          <button class="btn-delete" @click="batchDelete" :disabled="selectedIds.length === 0">
            确认删除
          </button>
        </div>
      </div>

      <!-- ========== 新增日程表单（笔记本风格） ========== -->
      <div v-if="isAdding" class="add-form-container">
        <div class="add-form-header">
          <div class="header-left">
            <span class="form-title">新建日程</span>
            <span class="form-date">{{ currentDate }}</span>
          </div>
          <div class="header-actions">
            <button class="btn-cancel" @click="toggleAddForm">取消</button>
            <button class="btn-confirm" @click="submitSchedule" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>

        <div class="add-form-body">
          <!-- 标题 - 大字，无边框 -->
          <input
              v-model="formData.title"
              type="text"
              class="title-input"
              placeholder="标题"
          />

          <!-- 时间 - 简洁显示 -->
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
                  style="width: 100%; border: none;"
              />
              <div v-else class="period-picker">
                <el-date-picker
                    v-model="formData.startTime"
                    type="datetime"
                    placeholder="开始"
                    style="flex: 1; border: none;"
                />
                <span class="time-separator">→</span>
                <el-date-picker
                    v-model="formData.endTime"
                    type="datetime"
                    placeholder="结束"
                    style="flex: 1; border: none;"
                />
              </div>
            </div>
          </div>

          <!-- 重复频率 -->
          <div class="repeat-section">
            <span class="label">重复</span>
            <el-select v-model="formData.repeatRule" class="repeat-select" :teleported="false">
              <el-option label="不重复" value="none" />
              <el-option label="每天" value="daily" />
              <el-option label="每周" value="weekly" />
              <el-option label="每月" value="monthly" />
              <el-option label="每年" value="yearly" />
              <el-option label="工作日" value="workday" />
              <el-option label="节假日" value="holiday" />
            </el-select>
          </div>

          <!-- 备注 - 大文本框 -->
          <textarea
              v-model="formData.remark"
              class="remark-input"
              placeholder="备注"
              rows="4"
          ></textarea>

          <!-- 标签 -->
          <div class="tag-section">
            <span class="label">标签</span>
            <TagSelector v-model="formData.tagId" @tag-created="handleTagCreated" />
          </div>

          <!-- 关联笔记 -->
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

      <!-- 日程分组 -->
      <div class="schedule-groups">
        <!-- 已过期 -->
        <div v-if="expiredList.length > 0" class="schedule-group">
          <div class="group-title expired-title">
            <span class="group-icon">📅</span> 已过期
            <span class="group-count">{{ expiredList.length }}</span>
          </div>
          <div class="schedule-list">
            <div v-for="schedule in paginatedGroups.expired" :key="schedule.id" class="schedule-item expired">
              <div class="schedule-left">
                <input v-if="deleteMode" type="checkbox" :checked="selectedIds.includes(schedule.id)" @change="toggleSelect(schedule.id)" class="checkbox" />
                <input type="checkbox" :checked="schedule.completed" @change="!deleteMode && toggleComplete(schedule, $event)" :disabled="deleteMode" class="checkbox" />
                <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                  <span class="schedule-title">{{ schedule.title }}</span>
                  <span class="schedule-time">{{ formatScheduleTime(schedule) }}</span>
                </div>
              </div>
              <div class="schedule-right">
                <span v-if="schedule.tagId && getTagName(schedule.tagId)" class="tag-display">
                  <span class="tag-dot" :style="{ backgroundColor: getTagColor(getTagName(schedule.tagId)) }"></span>
                  {{ getTagName(schedule.tagId) }}
                </span>
              </div>
            </div>
          </div>
          <div class="group-pagination">
            <span class="page-info">{{ currentPageMap.expired }} / {{ Math.ceil(expiredList.length / pageSizeMap.expired) }}</span>
            <div class="pagination-btns">
              <button class="page-btn" @click="handlePageChange('expired', currentPageMap.expired - 1)" :disabled="currentPageMap.expired === 1">‹</button>
              <button class="page-btn" @click="handlePageChange('expired', currentPageMap.expired + 1)" :disabled="currentPageMap.expired >= Math.ceil(expiredList.length / pageSizeMap.expired)">›</button>
            </div>
          </div>
        </div>

        <!-- 接下来7天 -->
        <div v-if="nextWeekList.length > 0" class="schedule-group">
          <div class="group-title next-week-title">
            <span class="group-icon">⏰</span> 接下来7天
            <span class="group-count">{{ nextWeekList.length }}</span>
          </div>
          <div class="schedule-list">
            <div v-for="schedule in paginatedGroups.nextWeek" :key="schedule.id" class="schedule-item normal">
              <div class="schedule-left">
                <input v-if="deleteMode" type="checkbox" :checked="selectedIds.includes(schedule.id)" @change="toggleSelect(schedule.id)" class="checkbox" />
                <input type="checkbox" :checked="schedule.completed" @change="!deleteMode && toggleComplete(schedule, $event)" :disabled="deleteMode" class="checkbox" />
                <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                  <span class="schedule-title">{{ schedule.title }}</span>
                  <span class="schedule-time">{{ formatScheduleTime(schedule) }}</span>
                </div>
              </div>
              <div class="schedule-right">
                <span v-if="schedule.tagId && getTagName(schedule.tagId)" class="tag-display">
                  <span class="tag-dot" :style="{ backgroundColor: getTagColor(getTagName(schedule.tagId)) }"></span>
                  {{ getTagName(schedule.tagId) }}
                </span>
              </div>
            </div>
          </div>
          <div class="group-pagination">
            <span class="page-info">{{ currentPageMap.nextWeek }} / {{ Math.ceil(nextWeekList.length / pageSizeMap.nextWeek) }}</span>
            <div class="pagination-btns">
              <button class="page-btn" @click="handlePageChange('nextWeek', currentPageMap.nextWeek - 1)" :disabled="currentPageMap.nextWeek === 1">‹</button>
              <button class="page-btn" @click="handlePageChange('nextWeek', currentPageMap.nextWeek + 1)" :disabled="currentPageMap.nextWeek >= Math.ceil(nextWeekList.length / pageSizeMap.nextWeek)">›</button>
            </div>
          </div>
        </div>

        <!-- 其他时间 -->
        <div v-if="otherList.length > 0" class="schedule-group">
          <div class="group-title other-title">
            <span class="group-icon">📅</span> 其他时间
            <span class="group-count">{{ otherList.length }}</span>
          </div>
          <div class="schedule-list">
            <div v-for="schedule in paginatedGroups.other" :key="schedule.id" class="schedule-item normal">
              <div class="schedule-left">
                <input v-if="deleteMode" type="checkbox" :checked="selectedIds.includes(schedule.id)" @change="toggleSelect(schedule.id)" class="checkbox" />
                <input type="checkbox" :checked="schedule.completed" @change="!deleteMode && toggleComplete(schedule, $event)" :disabled="deleteMode" class="checkbox" />
                <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                  <span class="schedule-title">{{ schedule.title }}</span>
                  <span class="schedule-time">{{ formatScheduleTime(schedule) }}</span>
                </div>
              </div>
              <div class="schedule-right">
                <span v-if="schedule.tagId && getTagName(schedule.tagId)" class="tag-display">
                  <span class="tag-dot" :style="{ backgroundColor: getTagColor(getTagName(schedule.tagId)) }"></span>
                  {{ getTagName(schedule.tagId) }}
                </span>
              </div>
            </div>
          </div>
          <div class="group-pagination">
            <span class="page-info">{{ currentPageMap.other }} / {{ Math.ceil(otherList.length / pageSizeMap.other) }}</span>
            <div class="pagination-btns">
              <button class="page-btn" @click="handlePageChange('other', currentPageMap.other - 1)" :disabled="currentPageMap.other === 1">‹</button>
              <button class="page-btn" @click="handlePageChange('other', currentPageMap.other + 1)" :disabled="currentPageMap.other >= Math.ceil(otherList.length / pageSizeMap.other)">›</button>
            </div>
          </div>
        </div>

        <!-- 已完成 -->
        <div v-if="completedList.length > 0" class="schedule-group">
          <div class="group-title completed-title">
            <span class="group-icon">✅</span> 已完成
            <span class="group-count">{{ completedList.length }}</span>
          </div>
          <div class="schedule-list">
            <div v-for="schedule in paginatedGroups.completed" :key="schedule.id" class="schedule-item completed">
              <div class="schedule-left">
                <input v-if="deleteMode" type="checkbox" :checked="selectedIds.includes(schedule.id)" @change="toggleSelect(schedule.id)" class="checkbox" />
                <input type="checkbox" :checked="schedule.completed" @change="!deleteMode && toggleComplete(schedule, $event)" :disabled="deleteMode" class="checkbox" />
                <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                  <span class="schedule-title">{{ schedule.title }}</span>
                  <span class="schedule-time">{{ formatScheduleTime(schedule) }}</span>
                </div>
              </div>
              <div class="schedule-right">
                <span v-if="schedule.tagId && getTagName(schedule.tagId)" class="tag-display">
                  <span class="tag-dot" :style="{ backgroundColor: getTagColor(getTagName(schedule.tagId)) }"></span>
                  {{ getTagName(schedule.tagId) }}
                </span>
              </div>
            </div>
          </div>
          <div class="group-pagination">
            <span class="page-info">{{ currentPageMap.completed }} / {{ Math.ceil(completedList.length / pageSizeMap.completed) }}</span>
            <div class="pagination-btns">
              <button class="page-btn" @click="handlePageChange('completed', currentPageMap.completed - 1)" :disabled="currentPageMap.completed === 1">‹</button>
              <button class="page-btn" @click="handlePageChange('completed', currentPageMap.completed + 1)" :disabled="currentPageMap.completed >= Math.ceil(completedList.length / pageSizeMap.completed)">›</button>
            </div>
          </div>
        </div>

        <div v-if="noData" class="empty-state">暂无日程</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import TagSidebar from "@/components/TagSidebar.vue"
import TagSelector from "@/components/TagSelector.vue";
import { useRouter } from 'vue-router'

// ---------- 数据 ----------
const scheduleList = ref([])
const tagList = ref([])
const noteList = ref([])
const currentTag = ref('all')
const formRef = ref(null)
const router = useRouter()
const saving = ref(false)
const isAdding = ref(false)

// 当前日期
const currentDate = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

// 表单数据
const formData = ref({
  title: '',
  timeType: 'point',
  startTime: '',
  endTime: '',
  repeatRule: 'none',
  remark: '',
  tagId: null,
  noteIds: []
})

// 表单校验规则
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

// 分页
const currentPageMap = ref({
  expired: 1,
  nextWeek: 1,
  other: 1,
  completed: 1
})

const pageSizeMap = ref({
  expired: 5,
  nextWeek: 5,
  other: 5,
  completed: 5
})

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

// ---------- 辅助函数 ----------
const formatScheduleTime = (schedule) => {
  if (!schedule.endTime) return ''

  const formatTime = (dateStr) => {
    const date = new Date(dateStr)
    return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }

  if (!schedule.startTime) {
    return formatTime(schedule.endTime)
  }

  return `${formatTime(schedule.startTime)} ~ ${formatTime(schedule.endTime)}`
}

const isExpired = (schedule) => {
  if (schedule.completed) return false
  return new Date(schedule.endTime) < new Date()
}

const isNextWeek = (schedule) => {
  if (schedule.completed) return false
  const now = new Date()
  const endTime = new Date(schedule.endTime)
  const diffDays = Math.ceil((endTime - now) / (1000 * 60 * 60 * 24))
  return diffDays >= 0 && diffDays <= 7
}

// 分组列表
const expiredList = computed(() => scheduleList.value.filter(s => !s.completed && isExpired(s)))
const nextWeekList = computed(() => scheduleList.value.filter(s => !s.completed && !isExpired(s) && isNextWeek(s)))
const otherList = computed(() => scheduleList.value.filter(s => !s.completed && !isExpired(s) && !isNextWeek(s)))
const completedList = computed(() => scheduleList.value.filter(s => s.completed))

// 分页后的数据
const paginatedGroups = computed(() => {
  const paginate = (list, page, pageSize) => {
    const start = (page - 1) * pageSize
    return list.slice(start, start + pageSize)
  }
  return {
    expired: paginate(expiredList.value, currentPageMap.value.expired, pageSizeMap.value.expired),
    nextWeek: paginate(nextWeekList.value, currentPageMap.value.nextWeek, pageSizeMap.value.nextWeek),
    other: paginate(otherList.value, currentPageMap.value.other, pageSizeMap.value.other),
    completed: paginate(completedList.value, currentPageMap.value.completed, pageSizeMap.value.completed)
  }
})

const noData = computed(() => scheduleList.value.length === 0)

// 获取标签名称
const getTagName = (tagId) => {
  if (!tagId) return null
  const tag = tagList.value.find(t => t.id === tagId)
  return tag ? tag.name : null
}

const getFullDateTime = (schedule) => {
  if (!schedule.endTime) return ''

  const formatFull = (dateStr) => {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }

  if (!schedule.startTime) {
    return formatFull(schedule.endTime)
  }
  return `${formatFull(schedule.startTime)} ~ ${formatFull(schedule.endTime)}`
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

// ---------- 笔记选择器相关 ----------
const selectedNotes = ref([])
const noteSearchKeyword = ref('')
const noteFilterTagId = ref(null)
const noteDialogVisible = ref(false)
const tempSelectedNoteIds = ref([])
const viewNoteDialogVisible = ref(false)
const currentViewNote = ref(null)

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

// ---------- 事件 ----------
const onTagChange = (tagId) => {
  currentTag.value = tagId
  fetchScheduleList()
}

const toggleComplete = async (schedule, event) => {
  if (event) event.stopPropagation()
  if (deleteMode.value) return

  const originalCompleted = schedule.completed
  const newCompleted = originalCompleted ? 0 : 1

  schedule.completed = newCompleted

  try {
    const response = await axios.put('http://localhost:8080/api/schedule/complete', null, {
      params: { id: schedule.id, completed: newCompleted }
    })
    if (response.data.code === 200) {
      await fetchScheduleList()
      ElMessage.success(newCompleted ? '已完成' : '已取消完成')
    } else {
      throw new Error(response.data.message)
    }
  } catch (error) {
    schedule.completed = originalCompleted
    console.error('更新完成状态失败', error)
    ElMessage.error('操作失败，请重试')
  }
}

const goToDetail = (id) => {
  if (deleteMode.value) return
  router.push({ path: '/schedule/detail', query: { id } })
}

const handlePageChange = (group, page) => {
  if (page < 1) return
  const maxPage = Math.ceil(
      group === 'expired' ? expiredList.value.length / pageSizeMap.value.expired :
      group === 'nextWeek' ? nextWeekList.value.length / pageSizeMap.value.nextWeek :
      group === 'other' ? otherList.value.length / pageSizeMap.value.other :
      completedList.value.length / pageSizeMap.value.completed
  )
  if (page > maxPage) return
  currentPageMap.value[group] = page
}

const handlePageSizeChange = (group, size) => {
  pageSizeMap.value[group] = parseInt(size)
  currentPageMap.value[group] = 1
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
  }
})

// 切换添加表单的展开/收起
const toggleAddForm = () => {
  if (isAdding.value) {
    isAdding.value = false
    resetForm()
  } else {
    const defaultTime = getDefaultTime()
    formData.value = {
      title: '',
      timeType: 'point',
      startTime: '',
      endTime: defaultTime,
      repeatRule: 'none',
      remark: '',
      tagId: null,
      noteIds: []
    }
    selectedNotes.value = []
    isAdding.value = true
  }
}

const resetForm = () => {
  formData.value = {
    title: '',
    timeType: 'point',
    startTime: '',
    endTime: '',
    repeatRule: 'none',
    remark: '',
    tagId: null,
    noteIds: []
  }
  selectedNotes.value = []
  formRef.value?.clearValidate()
}

const submitSchedule = async () => {
  console.log('✅ 按钮被点击了！')
  
  // 检查 formRef 是否存在
  console.log('formRef:', formRef.value)
  
  // 直接打印表单数据
  console.log('表单数据:', formData.value)
  
  // 构造提交数据
  const submitData = {
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
    submitData.startTime = formData.value.startTime
    submitData.endTime = formData.value.endTime
  }

  console.log('提交数据:', submitData)

  // 发送请求
  try {
    const response = await axios.post('http://localhost:8080/api/schedule/add', submitData)
    console.log('服务器响应:', response.data)
    if (response.data.code === 200) {
      ElMessage.success('添加成功')
      isAdding.value = false
      resetForm()
      fetchScheduleList()
    } else {
      ElMessage.error(response.data.message || '添加失败')
    }
  } catch (error) {
    console.error('错误详情:', error)
    if (error.response) {
      console.error('服务器返回:', error.response.data)
      ElMessage.error(error.response.data.message || '添加失败')
    } else {
      ElMessage.error('请求失败，请检查网络或后端服务')
    }
  }
}

const handleTagCreated = (newTag) => {
  fetchTagList()
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

const fetchScheduleList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/schedule/list')
    scheduleList.value = response.data.data || []

    currentPageMap.value = {
      expired: 1,
      nextWeek: 1,
      other: 1,
      completed: 1
    }
  } catch (error) {
    console.error('获取日程失败', error)
    ElMessage.error('获取日程失败')
  }
}

// ---------- 批量删除相关 ----------
const deleteMode = ref(false)
const selectedIds = ref([])

const toggleDeleteMode = () => {
  deleteMode.value = !deleteMode.value
  if (!deleteMode.value) {
    selectedIds.value = []
  }
}

const cancelDelete = () => {
  deleteMode.value = false
  selectedIds.value = []
}

const toggleSelect = (id) => {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const batchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的日程')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedIds.value.length} 个日程吗？删除后不可恢复！`,
        '批量删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const response = await axios.delete('http://localhost:8080/api/schedule/batch-delete', {
      data: selectedIds.value
    })

    if (response.data.code === 200) {
      ElMessage.success(`成功删除 ${selectedIds.value.length} 个日程`)
      deleteMode.value = false
      selectedIds.value = []
      fetchScheduleList()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchTagList()
  fetchNoteList()
  fetchScheduleList()
})
</script>

<style scoped>
/* ================= 全局重置 ================= */
.schedule-container {
  display: flex;
  height: 100vh;
  width: 100%;
  background: #f9f9f9;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.schedule-main {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

/* ================= 头部 ================= */
.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.schedule-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn, .create-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: 0.2s;
}

.action-btn {
  background: #f3f4f6;
  color: #6b7280;
}

.action-btn:hover {
  background: #e5e7eb;
}

.action-btn.active {
  background: #e5e7eb;
  color: #1a1a1a;
}

.create-btn {
  background: #fbbf24;
  color: #1a1a1a;
}

.create-btn:hover {
  background: #f59e0b;
}

/* ================= 删除模式 ================= */
.delete-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fef2f2;
  border-radius: 8px;
}

.delete-mode-bar span {
  font-size: 14px;
  color: #ef4444;
}

.delete-mode-actions {
  display: flex;
  gap: 8px;
}

.btn-delete {
  padding: 6px 16px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.btn-delete:hover {
  background: #dc2626;
}

.btn-delete:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel {
  padding: 6px 16px;
  background: #f3f4f6;
  color: #6b7280;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.btn-cancel:hover {
  background: #e5e7eb;
}

/* ================= 新增日程表单（笔记本风格） ================= */
.add-form-container {
  background: white;
  border-radius: 14px;
  margin-bottom: 24px;
  overflow: hidden;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); max-height: 0; }
  to { opacity: 1; transform: translateY(0); max-height: 2000px; }
}

.add-form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.form-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.form-date {
  font-size: 14px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.add-form-body {
  padding: 24px;
}

/* 标题输入 */
.title-input {
  width: 100%;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  border: none;
  outline: none;
  padding: 0 0 12px 0;
  background: transparent;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.title-input::placeholder {
  color: #9ca3af;
}

.title-input:focus {
  border-bottom-color: #4f8cff;
}

/* 时间区域 */
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
  color: #9ca3af;
  font-size: 16px;
}

/* 重复频率 */
.repeat-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.repeat-select {
  flex: 1;
}

/* 备注输入 */
.remark-input {
  width: 100%;
  font-size: 16px;
  line-height: 1.8;
  color: #374151;
  border: none;
  outline: none;
  padding: 0 0 12px 0;
  background: transparent;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
}

.remark-input::placeholder {
  color: #9ca3af;
}

.remark-input:focus {
  border-bottom-color: #4f8cff;
}

/* 标签和笔记区域 */
.tag-section, .note-section {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.label {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  min-width: 50px;
  padding-top: 4px;
}

.notes-display {
  flex: 1;
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
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  color: #4b5563;
}

.remove-tag {
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  padding: 0 4px;
  font-size: 14px;
}

.remove-tag:hover {
  color: #ef4444;
}

.placeholder-text {
  color: #9ca3af;
  font-size: 13px;
}

/* ================= 日程分组 ================= */
.schedule-group {
  margin-bottom: 24px;
}

.group-title {
  font-size: 16px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-icon {
  font-size: 18px;
}

.group-count {
  background: #f3f4f6;
  color: #6b7280;
  padding: 0 8px;
  border-radius: 12px;
  font-size: 12px;
}

.schedule-list {
  background: white;
  border-radius: 14px;
  overflow: hidden;
}

.schedule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  transition: 0.2s;
}

.schedule-item:last-child {
  border-bottom: none;
}

.schedule-item:hover {
  background: #fafafa;
}

.schedule-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  flex-shrink: 0;
}

.schedule-content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.schedule-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  display: block;
}

.schedule-time {
  font-size: 13px;
  color: #9ca3af;
}

.schedule-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 状态颜色 */
.expired-title {
  color: #ef4444;
}

.next-week-title {
  color: #f59e0b;
}

.other-title {
  color: #6b7280;
}

.completed-title {
  color: #34d399;
}

.schedule-item.expired .schedule-title {
  color: #ef4444;
}

.schedule-item.completed {
  opacity: 0.6;
}

.schedule-item.completed .schedule-title {
  text-decoration: line-through;
  color: #9ca3af;
}

/* ================= 标签样式 ================= */
.tag-display {
  display: inline-flex;
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

/* ================= 分页 ================= */
.group-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 0 0 14px 14px;
}

.page-info {
  font-size: 13px;
  color: #6b7280;
}

.pagination-btns {
  display: flex;
  gap: 8px;
}

.page-btn {
  padding: 4px 12px;
  border: none;
  background: #f3f4f6;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
}

.page-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ================= 空状态 ================= */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #9ca3af;
}

/* ================= 笔记选择器样式 ================= */
.note-selector {
  padding: 8px 0;
}

.note-search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.note-list-selector {
  max-height: 300px;
  overflow-y: auto;
}

.note-item-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: 0.2s;
}

.note-item-selector:hover {
  background: #fafafa;
}

.note-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-title {
  font-size: 14px;
  color: #1a1a1a;
}

.placeholder-text {
  color: #9ca3af;
  font-size: 13px;
}
</style>
