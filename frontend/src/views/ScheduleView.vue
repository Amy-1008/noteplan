<template>
  <div class="schedule-container">
    <!-- 左侧：标签侧边栏组件 -->
    <TagSidebar v-model="currentTag" @change="onTagChange" />

    <!-- 右侧主内容区 -->
    <div class="schedule-main">
      <div class="schedule-header">
        <h2>日程</h2>
        <div class="header-actions">
          <!-- 批量删除 -->
          <el-button
              :type="deleteMode ? 'danger' : 'default'"
              plain
              circle
              @click="toggleDeleteMode"
              :icon="Delete"
          />
          <!-- 添加按钮 -->
          <el-button type="primary" circle @click="openAddDialog" :icon="Plus" />
        </div>
      </div>

      <div v-if="deleteMode" class="delete-mode-bar">
        <span>已选择 {{ selectedIds.length }} 个日程</span>
        <div class="delete-mode-actions">
          <el-button size="small" @click="cancelDelete">取消</el-button>
          <el-button type="danger" size="small" @click="batchDelete" :disabled="selectedIds.length === 0">
            确认删除
          </el-button>
        </div>
      </div>

      <div class="schedule-groups">
        <!-- 已过期 -->
        <div v-if="expiredList.length > 0" class="schedule-group">
          <div class="group-title expired-title">
            📅 已过期 ({{ expiredList.length }})
          </div>
          <div class="schedule-list">
            <div
                v-for="schedule in paginatedGroups.expired"
                :key="schedule.id"
                class="schedule-item expired"
            >
              <el-checkbox
                  v-if="deleteMode"
                  :model-value="selectedIds.includes(schedule.id)"
                  @change="toggleSelect(schedule.id)"
                  @click.stop
                  class="delete-checkbox"
              />
              <el-checkbox
                  v-model="schedule.completed"
                  @click.stop="!deleteMode && toggleComplete(schedule, $event)"
                  :disabled="deleteMode"
              />
              <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                <span class="schedule-title">{{ schedule.title }}</span>
              </div>
              <div class="schedule-right">
                <el-tag
                    v-if="schedule.tagId && getTagName(schedule.tagId)"
                    size="small"
                    :type="getTagType(schedule.rank)"
                    effect="plain"
                    class="schedule-tag"
                >
                  {{ getTagName(schedule.tagId) }}
                </el-tag>
                <div v-else class="tag-placeholder"></div>
                <div class="schedule-time" :title="getFullDateTime(schedule)">
                  {{ formatScheduleTime(schedule) }}
                </div>
              </div>
            </div>
          </div>
          <div class="group-pagination">
            <div class="pagination-size">
              <span>每页</span>
              <el-select
                  v-model="pageSizeMap.expired"
                  size="small"
                  style="width: 80px"
                  @change="handlePageSizeChange('expired', $event)"
              >
                <el-option :value="5" label="5" />
                <el-option :value="10" label="10" />
                <el-option :value="20" label="20" />
              </el-select>
              <span>条</span>
            </div>
            <el-pagination
                v-if="expiredList.length > pageSizeMap.expired"
                small
                layout="prev, pager, next"
                :total="expiredList.length"
                :page-size="pageSizeMap.expired"
                v-model:current-page="currentPageMap.expired"
                @current-change="handlePageChange('expired', $event)"
            />
          </div>
        </div>

        <!-- 接下来7天 -->
        <div v-if="nextWeekList.length > 0" class="schedule-group">
          <div class="group-title next-week-title">
            ⏰ 接下来7天 ({{ nextWeekList.length }})
          </div>
          <div class="schedule-list">
            <div
                v-for="schedule in paginatedGroups.nextWeek"
                :key="schedule.id"
                class="schedule-item normal"
            >
              <el-checkbox
                  v-if="deleteMode"
                  :model-value="selectedIds.includes(schedule.id)"
                  @change="toggleSelect(schedule.id)"
                  @click.stop
                  class="delete-checkbox"
              />
              <el-checkbox
                  v-model="schedule.completed"
                  @click.stop="!deleteMode && toggleComplete(schedule, $event)"
                  :disabled="deleteMode"
              />
              <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                <span class="schedule-title">{{ schedule.title }}</span>
              </div>
              <div class="schedule-right">
                <el-tag
                    v-if="schedule.tagId && getTagName(schedule.tagId)"
                    size="small"
                    :type="getTagType(schedule.rank)"
                    effect="plain"
                    class="schedule-tag"
                >
                  {{ getTagName(schedule.tagId) }}
                </el-tag>
                <div v-else class="tag-placeholder"></div>
                <div class="schedule-time" :title="getFullDateTime(schedule)">
                  {{ formatScheduleTime(schedule) }}
                </div>
              </div>
            </div>
          </div>
          <div class="group-pagination">
            <div class="pagination-size">
              <span>每页</span>
              <el-select
                  v-model="pageSizeMap.nextWeek"
                  size="small"
                  style="width: 80px"
                  @change="handlePageSizeChange('nextWeek', $event)"
              >
                <el-option :value="5" label="5" />
                <el-option :value="10" label="10" />
                <el-option :value="20" label="20" />
              </el-select>
              <span>条</span>
            </div>
            <el-pagination
                v-if="nextWeekList.length > pageSizeMap.nextWeek"
                small
                layout="prev, pager, next"
                :total="nextWeekList.length"
                :page-size="pageSizeMap.nextWeek"
                v-model:current-page="currentPageMap.nextWeek"
                @current-change="handlePageChange('nextWeek', $event)"
            />
          </div>
        </div>

        <!-- 其他时间 -->
        <div v-if="otherList.length > 0" class="schedule-group">
          <div class="group-title other-title">
            📅 其他时间 ({{ otherList.length }})
          </div>
          <div class="schedule-list">
            <div
                v-for="schedule in paginatedGroups.other"
                :key="schedule.id"
                class="schedule-item normal"
            >
              <el-checkbox
                  v-if="deleteMode"
                  :model-value="selectedIds.includes(schedule.id)"
                  @change="toggleSelect(schedule.id)"
                  @click.stop
                  class="delete-checkbox"
              />
              <el-checkbox
                  v-model="schedule.completed"
                  @click.stop="!deleteMode && toggleComplete(schedule, $event)"
                  :disabled="deleteMode"
              />
              <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                <span class="schedule-title">{{ schedule.title }}</span>
              </div>
              <div class="schedule-right">
                <el-tag
                    v-if="schedule.tagId && getTagName(schedule.tagId)"
                    size="small"
                    :type="getTagType(schedule.rank)"
                    effect="plain"
                    class="schedule-tag"
                >
                  {{ getTagName(schedule.tagId) }}
                </el-tag>
                <div v-else class="tag-placeholder"></div>
                <div class="schedule-time" :title="getFullDateTime(schedule)">
                  {{ formatScheduleTime(schedule) }}
                </div>
              </div>
            </div>
          </div>
          <div class="group-pagination">
            <div class="pagination-size">
              <span>每页</span>
              <el-select
                  v-model="pageSizeMap.other"
                  size="small"
                  style="width: 80px"
                  @change="handlePageSizeChange('other', $event)"
              >
                <el-option :value="5" label="5" />
                <el-option :value="10" label="10" />
                <el-option :value="20" label="20" />
              </el-select>
              <span>条</span>
            </div>
            <el-pagination
                v-if="otherList.length > pageSizeMap.other"
                small
                layout="prev, pager, next"
                :total="otherList.length"
                :page-size="pageSizeMap.other"
                v-model:current-page="currentPageMap.other"
                @current-change="handlePageChange('other', $event)"
            />
          </div>
        </div>

        <!-- 已完成 -->
        <div v-if="completedList.length > 0" class="schedule-group">
          <div class="group-title completed-title">
            ✅ 已完成 ({{ completedList.length }})
          </div>
          <div class="schedule-list">
            <div
                v-for="schedule in paginatedGroups.completed"
                :key="schedule.id"
                class="schedule-item completed"
            >
              <el-checkbox
                  v-if="deleteMode"
                  :model-value="selectedIds.includes(schedule.id)"
                  @change="toggleSelect(schedule.id)"
                  @click.stop
                  class="delete-checkbox"
              />
              <el-checkbox
                  v-model="schedule.completed"
                  @click.stop="!deleteMode && toggleComplete(schedule, $event)"
                  :disabled="deleteMode"
              />
              <div class="schedule-content" @click="!deleteMode && goToDetail(schedule.id)">
                <span class="schedule-title">{{ schedule.title }}</span>
              </div>
              <div class="schedule-right">
                <el-tag
                    v-if="schedule.tagId && getTagName(schedule.tagId)"
                    size="small"
                    :type="getTagType(schedule.rank)"
                    effect="plain"
                    class="schedule-tag"
                >
                  {{ getTagName(schedule.tagId) }}
                </el-tag>
                <div v-else class="tag-placeholder"></div>
                <div class="schedule-time" :title="getFullDateTime(schedule)">
                  {{ formatScheduleTime(schedule) }}
                </div>
              </div>
            </div>
          </div>
          <div class="group-pagination">
            <div class="pagination-size">
              <span>每页</span>
              <el-select
                  v-model="pageSizeMap.completed"
                  size="small"
                  style="width: 80px"
                  @change="handlePageSizeChange('completed', $event)"
              >
                <el-option :value="5" label="5" />
                <el-option :value="10" label="10" />
                <el-option :value="20" label="20" />
              </el-select>
              <span>条</span>
            </div>
            <el-pagination
                v-if="completedList.length > pageSizeMap.completed"
                small
                layout="prev, pager, next"
                :total="completedList.length"
                :page-size="pageSizeMap.completed"
                v-model:current-page="currentPageMap.completed"
                @current-change="handlePageChange('completed', $event)"
            />
          </div>
        </div>

        <el-empty v-if="noData" description="暂无日程" />
      </div>
    </div>

    <!-- 添加日程弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        title="添加日程"
        width="550px"
        append-to-body
        :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input
              v-model="formData.title"
              placeholder="请输入日程标题"
              maxlength="20"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="时间类型" prop="timeType">
          <el-radio-group v-model="formData.timeType">
            <el-radio value="point">时间点</el-radio>
            <el-radio value="period">时间段</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="formData.timeType === 'point'" label="时间" prop="endTime">
          <el-date-picker
              v-model="formData.endTime"
              type="datetime"
              placeholder="选择日期时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              @change="handleEndTimeChange"
              style="width: 100%"
          />
        </el-form-item>

        <template v-else>
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker
                v-model="formData.startTime"
                type="datetime"
                placeholder="选择开始时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                @change="handleStartTimeChange"
                style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker
                v-model="formData.endTime"
                type="datetime"
                placeholder="选择结束时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                @change="handleEndTimeChangeForPeriod"
                style="width: 100%"
            />
          </el-form-item>
        </template>

        <el-form-item label="重复频率">
          <el-select v-model="formData.repeatRule" placeholder="不重复" style="width: 100%">
            <el-option label="无" value="none" />
            <el-option label="每天" value="daily" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
            <el-option label="每年" value="yearly" />
            <el-option label="工作日" value="workday" />
            <el-option label="节假日" value="holiday" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注"
              maxlength="800"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="标签" prop="tagId">
          <TagSelector
              v-model="formData.tagId"
              @tag-created="handleTagCreated"
          />
        </el-form-item>

        <el-form-item label="关联笔记">
          <!-- 已选笔记胶囊显示 -->
          <div class="selected-notes-list" v-if="selectedNotes.length > 0">
            <el-tag
                v-for="note in selectedNotes"
                :key="note.id"
                closable
                @close="removeNote(note.id)"
                type="success"
                effect="plain"
            >
              {{ note.title }}
            </el-tag>
          </div>

          <!-- 选择笔记按钮 -->
          <el-button size="small" @click="openNoteSelector">
            <el-icon><Plus /></el-icon> 选择笔记
          </el-button>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">确定</el-button>
      </template>
    </el-dialog>

    <!-- 笔记选择器弹窗 -->
    <el-dialog
        v-model="noteDialogVisible"
        title="选择关联笔记"
        width="600px"
        append-to-body
    >
      <div class="note-selector">
        <div class="note-search-bar">
          <el-input
              v-model="noteSearchKeyword"
              placeholder="按标题搜索"
              clearable
              prefix-icon="Search"
              style="width: 200px; margin-right: 12px;"
          />
          <el-select
              v-model="noteFilterTagId"
              placeholder="按标签筛选"
              clearable
              style="width: 150px"
          >
            <el-option
                v-for="tag in tagList"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
            />
          </el-select>
        </div>

        <!-- 笔记列表 - 添加渲染代码 -->
        <div class="note-list-selector">
          <div
              v-for="note in filteredNoteList"
              :key="note.id"
              class="note-item-selector"
              @click="toggleNoteSelection(note.id)"
          >
            <el-checkbox
                :model-value="tempSelectedNoteIds.includes(note.id)"
                @click.stop
                @change="toggleNoteSelection(note.id)"
            />
            <div class="note-info">
              <span class="note-title">{{ note.title || '无标题' }}</span>
              <span v-if="note.tagName" class="note-tag-name">#{{ note.tagName }}</span>
            </div>
            <el-button text @click.stop="viewNoteDetail(note)">
              <el-icon><Document /></el-icon> 查看
            </el-button>
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
    <el-dialog
        v-model="viewNoteDialogVisible"
        :title="currentViewNote?.title || '笔记详情'"
        width="500px"
        append-to-body
    >
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
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Document, Search } from '@element-plus/icons-vue'
import axios from 'axios'
import TagSidebar from "@/components/TagSidebar.vue"
import TagSelector from "@/components/TagSelector.vue";
import { useRouter } from 'vue-router'

// ---------- 数据 ----------
const scheduleList = ref([])
const tagList = ref([])
const noteList = ref([])
const currentTag = ref('all')
const dialogVisible = ref(false)
const formRef = ref(null)
const router = useRouter()

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

const getTagType = (rank) => {
  return rank === 1 ? 'danger' : 'info'
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
  // 标签变化时刷新日程列表
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
  currentPageMap.value[group] = page
}

const handlePageSizeChange = (group, size) => {
  pageSizeMap.value[group] = size
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

const openAddDialog = () => {
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
  dialogVisible.value = true
}

const submitSchedule = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请填写必填项')
      return
    }

    try {
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

      const response = await axios.post('http://localhost:8080/api/schedule/add', submitData)

      if (response.data.code === 200) {
        ElMessage.success('添加成功')
        dialogVisible.value = false
        fetchScheduleList()
      } else {
        ElMessage.error(response.data.message || '添加失败')
      }
    } catch (error) {
      console.error('添加日程失败', error)
      ElMessage.error('添加失败')
    }
  })
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
    if (currentTag.value === 'uncategorized') {
      const response = await axios.get('http://localhost:8080/api/schedule/list')
      const allSchedules = response.data.data || []

      const allTagsResponse = await axios.get('http://localhost:8080/api/tags/filter', {
        params: { targetType: 'SCHEDULE' }
      })
      const taggedScheduleIds = (allTagsResponse.data.data || []).map(item => item.id)

      scheduleList.value = allSchedules.filter(s => !taggedScheduleIds.includes(s.id))

    } else if (currentTag.value !== 'all') {
      const filterResponse = await axios.get('http://localhost:8080/api/tags/filter', {
        params: {
          tagId: currentTag.value,
          targetType: 'SCHEDULE'
        }
      })
      const scheduleIds = (filterResponse.data.data || []).map(item => item.id)

      if (scheduleIds.length > 0) {
        const response = await axios.get('http://localhost:8080/api/schedule/list', {
          params: { ids: scheduleIds.join(',') }
        })
        scheduleList.value = response.data.data || []
      } else {
        scheduleList.value = []
      }
    } else {
      const response = await axios.get('http://localhost:8080/api/schedule/list')
      scheduleList.value = response.data.data || []
    }

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

const handleTagCreated = (newTag) => {
  fetchTagList()
}

onMounted(() => {
  fetchTagList()
  fetchNoteList()
  fetchScheduleList()
})
</script>

<style scoped>
.schedule-container {
  display: flex;
  height: 100vh;
  width: 100%;
  background-color: #f5f7fa;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

/* 右侧主内容区 */
.schedule-main {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.schedule-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.delete-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 8px;
  padding: 8px 16px;
  margin-bottom: 16px;
}

.delete-mode-bar span {
  font-size: 14px;
  color: #f56c6c;
}

.delete-mode-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.schedule-group {
  margin-bottom: 28px;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.group-title {
  font-size: 16px;
  font-weight: 500;
  padding: 12px 16px;
  border-left: 4px solid;
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

.expired-title {
  border-left-color: #f56c6c;
  color: #f56c6c;
}

.next-week-title {
  border-left-color: #e6a23c;
  color: #e6a23c;
}

.other-title {
  border-left-color: #909399;
  color: #909399;
}

.completed-title {
  border-left-color: #67c23a;
  color: #67c23a;
}

/* 日程项 */
.schedule-list {
  background: white;
}

.schedule-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  transition: background 0.2s;
  gap: 12px;
  cursor: pointer;
}

.schedule-item:last-child {
  border-bottom: none;
}

.schedule-item:hover {
  background-color: #f5f7fa;
}

/* 左侧：标题区域 */
.schedule-content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.schedule-title {
  font-size: 14px;
  font-weight: 500;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 6px;
  flex-shrink: 0;
  width: 100px;
}

.schedule-tag {
  font-size: 12px;
  height: 22px;
  line-height: 20px;
  margin: 0;
  background-color: #ecf5ff;
  border-color: #d9ecff;
  color: #409eff;
  display: inline-block;
  width: auto;
  max-width: none;
}

.schedule-tag.tag-important {
  background-color: #fef0f0;
  border-color: #fbc4c4;
  color: #f56c6c;
}

.schedule-time {
  font-size: 12px;
  color: #909399;
  cursor: default;
  white-space: nowrap;
}

.schedule-time:hover {
  color: #409eff;
}

.schedule-item.expired {
  background-color: #fef0f0;
}

.schedule-item.expired .schedule-title {
  color: #f56c6c;
}

.schedule-item.completed {
  opacity: 0.6;
}

.schedule-item.completed .schedule-title {
  text-decoration: line-through;
  color: #909399;
}

.schedule-item.normal .schedule-title {
  color: #303133;
}

.group-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #fafafa;
  border-top: 1px solid #ebeef5;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.tag-placeholder {
  height: 22px;
}

.delete-checkbox {
  margin-right: 4px;
}

/* 笔记选择器样式 */
.note-selector {
  padding: 8px 0;
}

.note-search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.note-list-selector {
  max-height: 400px;
  overflow-y: auto;
}

.note-item-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.note-item-selector:hover {
  background-color: #f5f7fa;
}

.note-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-title {
  font-size: 14px;
  color: #303133;
}

.note-tag-name {
  font-size: 12px;
  color: #909399;
}

.note-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.note-tag-title {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-view-icon {
  cursor: pointer;
  margin-left: 4px;
}

.note-view-icon:hover {
  color: #409eff;
}

.note-view-content {
  max-height: 400px;
  overflow-y: auto;
}

.note-view-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 12px;
}

.note-view-body {
  white-space: pre-wrap;
  line-height: 1.6;
}

.notes-display {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notes-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.placeholder-text {
  color: #c0c4cc;
  font-size: 13px;
}

.selected-notes-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.selected-note-tag {
  cursor: default;
}
</style>