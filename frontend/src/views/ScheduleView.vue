<template>
  <div class="schedule-container">
    <div class="schedule-main">
      <div class="schedule-header">
        <h2>日程</h2>
        <div class="header-actions">
          <button class="action-btn" :class="{ active: deleteMode }" @click="toggleDeleteMode">
            {{ deleteMode ? '取消选择' : '批量删除' }}
          </button>
          <button class="create-btn" @click="toggleAddForm">
            {{ isAdding ? '取消' : '+ 新建日程' }}
          </button>
        </div>
      </div>

      <div v-if="deleteMode" class="delete-mode-bar">
        <span>已选择 {{ selectedIds.length }} 个日程</span>
        <div class="delete-mode-actions">
          <button class="btn-cancel" @click="cancelDelete">取消</button>
          <button class="btn-delete" @click="batchDelete" :disabled="selectedIds.length === 0">确认删除</button>
        </div>
      </div>

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
          <el-form :model="formData" :rules="formRules" ref="formRef" label-width="80px">
            <el-form-item label="标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入日程标题" maxlength="20" show-word-limit />
            </el-form-item>

            <el-form-item label="时间类型" prop="timeType">
              <el-radio-group v-model="formData.timeType">
                <el-radio value="point">点</el-radio>
                <el-radio value="period">段</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="formData.timeType === 'point'" label="时间" prop="endTime">
              <el-date-picker
                  v-model="formData.endTime"
                  type="datetime"
                  placeholder="选择时间"
                  style="width: 100%"
                  @change="handleEndTimeChange"
              />
            </el-form-item>

            <template v-else>
              <el-form-item label="开始时间" prop="startTime">
                <el-date-picker
                    v-model="formData.startTime"
                    type="datetime"
                    placeholder="开始"
                    style="width: 100%"
                    @change="handleStartTimeChange"
                />
              </el-form-item>
              <el-form-item label="结束时间" prop="endTime">
                <el-date-picker
                    v-model="formData.endTime"
                    type="datetime"
                    placeholder="结束"
                    style="width: 100%"
                    @change="handleEndTimeChangeForPeriod"
                />
              </el-form-item>
            </template>

            <el-form-item label="重复频率">
              <el-select v-model="formData.repeatRule" placeholder="不重复" style="width: 100%">
                <el-option label="不重复" value="none" />
                <el-option label="每天" value="daily" />
                <el-option label="每周" value="weekly" />
                <el-option label="每月" value="monthly" />
                <el-option label="每年" value="yearly" />
                <el-option label="工作日" value="workday" />
                <el-option label="节假日" value="holiday" />
              </el-select>
            </el-form-item>

            <el-form-item label="备注">
              <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
            </el-form-item>

            <el-form-item label="标签">
              <TagSelector v-model="formData.tagId" @tag-created="handleTagCreated" />
            </el-form-item>

            <el-form-item label="关联笔记">
              <div class="selected-notes-list" v-if="selectedNotes.length > 0">
                <el-tag v-for="note in selectedNotes" :key="note.id" closable @close="removeNote(note.id)" type="success" effect="plain">
                  {{ note.title }}
                </el-tag>
              </div>
              <el-button size="small" @click="openNoteSelector">
                <el-icon><Plus /></el-icon> 选择笔记
              </el-button>
            </el-form-item>
          </el-form>

        </div>
      </div>

      <!-- 日程分组 -->
      <div class="schedule-groups">
        <div v-if="expiredList.length > 0" class="schedule-group">
          <div class="group-title expired-title"> 已过期 ({{ expiredList.length }})</div>
          <div class="schedule-list">
            <div v-for="schedule in paginatedGroups.expired" :key="schedule.id" class="schedule-item expired">
              <div class="schedule-left">
                <input v-if="deleteMode" type="checkbox" :checked="selectedIds.includes(schedule.id)" @change="toggleSelect(schedule.id)" class="checkbox" />
                <input type="checkbox" :checked="schedule.completed" @change="!deleteMode && toggleComplete(schedule, $event)" :disabled="deleteMode" class="checkbox" />
                <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                  <span class="schedule-title">{{ schedule.title }}</span>
                  <span class="schedule-time" :title="getFullDateTime(schedule)">{{ formatScheduleTime(schedule) }}</span>
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
            <div class="pagination-size">
              <span>每页</span>
              <select v-model="pageSizeMap.expired" @change="handlePageSizeChange('expired', $event.target.value)" class="page-size-select">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
              </select>
              <span>条</span>
            </div>
            <div class="pagination-controls">
              <span class="page-info">{{ currentPageMap.expired }} / {{ Math.ceil(expiredList.length / pageSizeMap.expired) || 1 }}</span>
              <div class="pagination-btns">
                <button class="page-btn" @click="handlePageChange('expired', currentPageMap.expired - 1)" :disabled="currentPageMap.expired === 1">‹</button>
                <button class="page-btn" @click="handlePageChange('expired', currentPageMap.expired + 1)" :disabled="currentPageMap.expired >= Math.ceil(expiredList.length / pageSizeMap.expired)">›</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="nextWeekList.length > 0" class="schedule-group">
          <div class="group-title next-week-title"> 接下来7天 ({{ nextWeekList.length }})</div>
          <div class="schedule-list">
            <div v-for="schedule in paginatedGroups.nextWeek" :key="schedule.id" class="schedule-item normal">
              <div class="schedule-left">
                <input v-if="deleteMode" type="checkbox" :checked="selectedIds.includes(schedule.id)" @change="toggleSelect(schedule.id)" class="checkbox" />
                <input type="checkbox" :checked="schedule.completed" @change="!deleteMode && toggleComplete(schedule, $event)" :disabled="deleteMode" class="checkbox" />
                <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                  <span class="schedule-title">{{ schedule.title }}</span>
                  <span class="schedule-time" :title="getFullDateTime(schedule)">{{ formatScheduleTime(schedule) }}</span>
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
            <div class="pagination-size">
              <span>每页</span>
              <select v-model="pageSizeMap.nextWeek" @change="handlePageSizeChange('nextWeek', $event.target.value)" class="page-size-select">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
              </select>
              <span>条</span>
            </div>
            <div class="pagination-controls">
              <span class="page-info">{{ currentPageMap.nextWeek }} / {{ Math.ceil(nextWeekList.length / pageSizeMap.nextWeek) || 1 }}</span>
              <div class="pagination-btns">
                <button class="page-btn" @click="handlePageChange('nextWeek', currentPageMap.nextWeek - 1)" :disabled="currentPageMap.nextWeek === 1">‹</button>
                <button class="page-btn" @click="handlePageChange('nextWeek', currentPageMap.nextWeek + 1)" :disabled="currentPageMap.nextWeek >= Math.ceil(nextWeekList.length / pageSizeMap.nextWeek)">›</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="otherList.length > 0" class="schedule-group">
          <div class="group-title other-title"> 其他时间 ({{ otherList.length }})</div>
          <div class="schedule-list">
            <div v-for="schedule in paginatedGroups.other" :key="schedule.id" class="schedule-item normal">
              <div class="schedule-left">
                <input v-if="deleteMode" type="checkbox" :checked="selectedIds.includes(schedule.id)" @change="toggleSelect(schedule.id)" class="checkbox" />
                <input type="checkbox" :checked="schedule.completed" @change="!deleteMode && toggleComplete(schedule, $event)" :disabled="deleteMode" class="checkbox" />
                <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                  <span class="schedule-title">{{ schedule.title }}</span>
                  <span class="schedule-time" :title="getFullDateTime(schedule)">{{ formatScheduleTime(schedule) }}</span>
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
            <div class="pagination-size">
              <span>每页</span>
              <select v-model="pageSizeMap.other" @change="handlePageSizeChange('other', $event.target.value)" class="page-size-select">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
              </select>
              <span>条</span>
            </div>
            <div class="pagination-controls">
              <span class="page-info">{{ currentPageMap.other }} / {{ Math.ceil(otherList.length / pageSizeMap.other) || 1 }}</span>
              <div class="pagination-btns">
                <button class="page-btn" @click="handlePageChange('other', currentPageMap.other - 1)" :disabled="currentPageMap.other === 1">‹</button>
                <button class="page-btn" @click="handlePageChange('other', currentPageMap.other + 1)" :disabled="currentPageMap.other >= Math.ceil(otherList.length / pageSizeMap.other)">›</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="completedList.length > 0" class="schedule-group">
          <div class="group-title completed-title"> 已完成 ({{ completedList.length }})</div>
          <div class="schedule-list">
            <div v-for="schedule in paginatedGroups.completed" :key="schedule.id" class="schedule-item completed">
              <div class="schedule-left">
                <input v-if="deleteMode" type="checkbox" :checked="selectedIds.includes(schedule.id)" @change="toggleSelect(schedule.id)" class="checkbox" />
                <input type="checkbox" :checked="schedule.completed" @change="!deleteMode && toggleComplete(schedule, $event)" :disabled="deleteMode" class="checkbox" />
                <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                  <span class="schedule-title">{{ schedule.title }}</span>
                  <span class="schedule-time" :title="getFullDateTime(schedule)">{{ formatScheduleTime(schedule) }}</span>
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
            <div class="pagination-size">
              <span>每页</span>
              <select v-model="pageSizeMap.completed" @change="handlePageSizeChange('completed', $event.target.value)" class="page-size-select">
                <option :value="5">5</option>
                <option :value="10">10</option>
                <option :value="20">20</option>
              </select>
              <span>条</span>
            </div>
            <div class="pagination-controls">
              <span class="page-info">{{ currentPageMap.completed }} / {{ Math.ceil(completedList.length / pageSizeMap.completed) || 1 }}</span>
              <div class="pagination-btns">
                <button class="page-btn" @click="handlePageChange('completed', currentPageMap.completed - 1)" :disabled="currentPageMap.completed === 1">‹</button>
                <button class="page-btn" @click="handlePageChange('completed', currentPageMap.completed + 1)" :disabled="currentPageMap.completed >= Math.ceil(completedList.length / pageSizeMap.completed)">›</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="noData" class="empty-state">暂无日程</div>
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
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Document, Search } from '@element-plus/icons-vue'
import axios from 'axios'
import TagSelector from "@/components/TagSelector.vue"
import { useRouter } from 'vue-router'
import { useNoteStore } from '@/store/note'

const store = useNoteStore()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const isAdding = ref(false)
const getOneHourLater = () => {
  const date = new Date()
  date.setHours(date.getHours() + 1)
  return date
}
const currentDate = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

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

// ---------- 辅助函数 ----------
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
const expiredList = computed(() => {
  const list = store.scheduleList || []
  return list.filter(s => !s.completed && isExpired(s))
})

const nextWeekList = computed(() => {
  const list = store.scheduleList || []
  return list.filter(s => !s.completed && !isExpired(s) && isNextWeek(s))
})

const otherList = computed(() => {
  const list = store.scheduleList || []
  return list.filter(s => !s.completed && !isExpired(s) && !isNextWeek(s))
})

const completedList = computed(() => {
  const list = store.scheduleList || []
  return list.filter(s => s.completed)
})

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

const noData = computed(() => (store.scheduleList || []).length === 0)

const getTagName = (tagId) => {
  if (!tagId) return null
  const tag = store.tags.find(t => t.id === tagId)
  return tag ? tag.name : null
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

// ---------- 事件 ----------
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
  console.log('handlePageChange:', group, page, 'currentPage:', currentPageMap.value[group])
  if (page < 1) return
  let totalCount = 0
  let pageSize = 0

  switch(group) {
    case 'expired':
      totalCount = expiredList.value.length
      pageSize = pageSizeMap.value.expired
      break
    case 'nextWeek':
      totalCount = nextWeekList.value.length
      pageSize = pageSizeMap.value.nextWeek
      break
    case 'other':
      totalCount = otherList.value.length
      pageSize = pageSizeMap.value.other
      break
    case 'completed':
      totalCount = completedList.value.length
      pageSize = pageSizeMap.value.completed
      break
    default:
      return
  }

  const maxPage = Math.ceil(totalCount / pageSize) || 1
  if (page > maxPage) return
  currentPageMap.value[group] = page
}

const handlePageSizeChange = (group, size) => {
  console.log('handlePageSizeChange:', group, size)
  const newSize = parseInt(size)
  if (isNaN(newSize) || newSize <= 0) return
  pageSizeMap.value[group] = newSize
  currentPageMap.value[group] = 1
}

// 校验并调整重复规则的时间（工作日/节假日）
const validateAndAdjustRepeatTime = (repeatRule, time) => {
  if (!time || repeatRule === 'none') return time

  let newDate = new Date(time)
  let adjusted = false
  let originalDate = new Date(time)

  if (repeatRule === 'workday') {
    // 工作日：周一到周五（getDay(): 0=周日, 6=周六）
    while (newDate.getDay() === 0 || newDate.getDay() === 6) {
      newDate.setDate(newDate.getDate() + 1)
      adjusted = true
    }
  } else if (repeatRule === 'holiday') {
    // 节假日：周六或周日
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
    ElMessage.info('已自动将结束时间设置为开始后一小时')
  }
})

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
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请填写必填项')
      return
    }

    saving.value = true
    try {
      let startTime = formData.value.startTime
      let endTime = formData.value.endTime
      const repeatRule = formData.value.repeatRule

      // 校验时间是否符合重复规则
      if (formData.value.timeType === 'point' && endTime) {
        endTime = validateAndAdjustRepeatTime(repeatRule, endTime)
      } else if (formData.value.timeType === 'period') {
        if (startTime) startTime = validateAndAdjustRepeatTime(repeatRule, startTime)
        if (endTime) endTime = validateAndAdjustRepeatTime(repeatRule, endTime)
      }

      const submitData = {
        title: formData.value.title,
        repeatRule: repeatRule,
        remark: formData.value.remark,
        tagId: formData.value.tagId,
        noteIds: formData.value.noteIds,
        startTime: formData.value.timeType === 'point' ? null : (startTime || null),
        endTime: endTime || null
      }

      const response = await axios.post('http://localhost:8080/api/schedule/add', submitData)

      if (response.data.code === 200) {
        ElMessage.success('添加成功')
        isAdding.value = false
        resetForm()
        fetchScheduleList()
      } else {
        ElMessage.error(response.data.message || '添加失败')
      }
    } catch (error) {
      console.error('添加日程失败', error)
      ElMessage.error('添加失败，请检查网络')
    } finally {
      saving.value = false
    }
  })
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
    const data = response.data.data || []
    store.fullScheduleList = data  // ✅ 保存完整列表
    store.scheduleList = data
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

// 监听窗口事件，响应 Sidebar 的筛选
onMounted(() => {
  // 加载初始数据
  fetchTagList()
  fetchNoteList()
  fetchScheduleList()

  // 监听筛选事件
  window.addEventListener('schedule-filtered', (event) => {
    store.scheduleList = event.detail
    currentPageMap.value = {
      expired: 1,
      nextWeek: 1,
      other: 1,
      completed: 1
    }
  })

  // 监听刷新事件
  window.addEventListener('schedule-refreshed', (event) => {
    store.scheduleList = event.detail
  })
})

// 组件卸载时移除监听
onUnmounted(() => {
  window.removeEventListener('schedule-filtered', () => {})
  window.removeEventListener('schedule-refreshed', () => {})
})
</script>
<style scoped>
.schedule-container {
  display: flex;
  height: 100vh;
  width: 100%;
  background: var(--bg-primary);
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.schedule-main {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

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
  color: var(--text-primary);
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
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.action-btn:hover {
  background: var(--border-color);
}

.action-btn.active {
  background: var(--border-color);
  color: var(--text-primary);
}

.create-btn {
  background: #fbbf24;
  color: #1a1a1a;
}

.create-btn:hover {
  background: #f59e0b;
}

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
  background: var(--bg-hover);
  color: var(--text-secondary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.btn-cancel:hover {
  background: var(--border-color);
}

.btn-confirm {
  padding: 6px 16px;
  background: var(--bg-hover);
  color: var(--text-secondary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.btn-confirm:hover {
  background: var(--border-color);
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 表单样式 */
.add-form-container {
  background: var(--card-bg);
  border-radius: 14px;
  margin-bottom: 24px;
  overflow: hidden;
  border: 1px solid var(--card-border);
}

.add-form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--card-border);
}

.form-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.form-date {
  font-size: 14px;
  color: var(--text-secondary);
}

.add-form-body {
  padding: 24px;
}

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

.remark-input {
  width: 100%;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
  border: none;
  outline: none;
  padding: 0 0 12px 0;
  background: transparent;
  border-bottom: 1px solid var(--card-border);
  margin-bottom: 20px;
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

/* 日程分组 */
.schedule-group {
  background: var(--card-bg);
  border-radius: 14px;
  border: 1px solid var(--card-border);
  margin-bottom: 16px;
  overflow: hidden;
}

.group-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-secondary);
  padding: 12px 16px;
  border-bottom: 1px solid var(--card-border);
}

.schedule-list {
  background: var(--card-bg);
}

.schedule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--card-border);
  transition: 0.2s;
}

.schedule-item:last-child {
  border-bottom: none;
}

.schedule-item:hover {
  background: var(--bg-hover);
}

.schedule-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.schedule-content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.schedule-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  display: block;
}

.schedule-time {
  font-size: 13px;
  color: var(--text-secondary);
}

.schedule-item.expired .schedule-title {
  color: #ef4444;
}

.schedule-item.completed {
  opacity: 0.6;
}

.schedule-item.completed .schedule-title {
  text-decoration: line-through;
  color: var(--text-secondary);
}

.group-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-hover);
  border-top: 1px solid var(--card-border);
}

.page-info {
  font-size: 13px;
  color: var(--text-secondary);
}

.pagination-btns {
  display: flex;
  gap: 8px;
}

.page-btn {
  padding: 4px 12px;
  border: none;
  background: var(--bg-hover);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
}

.page-btn:hover:not(:disabled) {
  background: var(--border-color);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--empty-text);
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

.pagination-size {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.page-size-select {
  padding: 4px 8px;
  background: var(--bg-hover);
  border: 1px solid var(--card-border);
  border-radius: 6px;
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>