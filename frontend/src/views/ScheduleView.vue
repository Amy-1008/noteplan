<template>
  <div class="schedule-container">
    <!-- 左侧：标签侧边栏组件 -->
    <TagSidebar v-model="currentTag" @change="onTagChange" />

    <!-- 右侧主内容区 -->
    <div class="schedule-main">
      <div class="schedule-header">
        <h2>日程</h2>
        <el-button type="primary" circle @click="openAddDialog">
          <el-icon><Plus /></el-icon>
        </el-button>
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
                @click="goToDetail(schedule.id)"
            >
              <el-checkbox v-model="schedule.completed" @click.stop="toggleComplete(schedule, $event)"/>
              <span class="schedule-title">{{ schedule.title }}</span>
              <span class="schedule-date">{{ formatScheduleTime(schedule) }}</span>
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
                @click="goToDetail(schedule.id)"
            >
              <el-checkbox v-model="schedule.completed" @click.stop="toggleComplete(schedule, $event)"/>
              <span class="schedule-title">{{ schedule.title }}</span>
              <span class="schedule-date">{{ formatScheduleTime(schedule) }}</span>
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
                @click="goToDetail(schedule.id)"
            >
              <el-checkbox v-model="schedule.completed" @click.stop="toggleComplete(schedule, $event)" />
              <span class="schedule-title">{{ schedule.title }}</span>
              <span class="schedule-date">{{ formatScheduleTime(schedule) }}</span>
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
                @click="goToDetail(schedule.id)"
            >
              <el-checkbox
                  v-model="schedule.completed"
                  @click.stop="toggleComplete(schedule, $event)"
              />
              <span class="schedule-title">{{ schedule.title }}</span>
              <span class="schedule-date">{{ formatScheduleTime(schedule) }}</span>
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

        <!-- 时间类型选择：时间点 / 时间段 -->
        <el-form-item label="时间类型" prop="timeType">
          <el-radio-group v-model="formData.timeType">
            <el-radio value="point">时间点</el-radio>
            <el-radio value="period">时间段</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 时间点模式：只选结束时间 -->
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

        <!-- 时间段模式：选开始时间和结束时间 -->
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
          <el-select
              v-model="formData.noteIds"
              multiple
              placeholder="选择关联笔记"
              style="width: 100%"
          >
            <el-option
                v-for="note in noteList"
                :key="note.id"
                :label="note.title"
                :value="note.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'
import TagSidebar from "@/components/TagSidebar.vue"
import TagSelector from "@/components/TagSelector.vue";
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

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
  tagId:null,
  noteIds: []
})

// 表单校验规则
const formRules = {
  title: [
    { required: true, message: '请输入日程标题', trigger: 'blur' },
    { max: 20, message: '标题不能超过20个字符', trigger: 'blur' }  // 添加这行
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
  // 备注的校验规则（需要添加到 el-form-item 的 prop）
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

// ---------- 事件 ----------
const onTagChange = (tagId) => {
  // 标签变化时刷新日程列表
  fetchScheduleList()
}

const toggleComplete = async (schedule, event) => {
  // 阻止事件冒泡到父元素
  if (event) event.stopPropagation()

  const originalCompleted = schedule.completed
  const newCompleted = originalCompleted ? 0 : 1

  // 先乐观更新 UI
  schedule.completed = newCompleted

  try {
    console.log('发送请求:', { id: schedule.id, completed: newCompleted })  // 添加日志
    const response = await axios.put('http://localhost:8080/api/schedule/complete', null, {
      params: { id: schedule.id, completed: newCompleted }
    })
    console.log('响应:', response.data)  // 添加日志
    if (response.data.code === 200) {
      await fetchScheduleList()
      ElMessage.success(newCompleted ? '已完成' : '已取消完成')
    } else {
      throw new Error(response.data.message)
    }
  } catch (error) {
    // 失败时回滚状态
    schedule.completed = originalCompleted
    console.error('更新完成状态失败', error)
    ElMessage.error('操作失败，请重试')
  }
}

const goToDetail = (id) => {
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

// 时间段模式：结束时间变化时，校验是否大于开始时间
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

// 监听时间类型切换
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
    tagId:null,
    noteIds: []
  }
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
      noteList.value = response.data.data
    }
  } catch (error) {
    console.error('获取笔记失败', error)
  }
}

const fetchScheduleList = async () => {
  try {
    if (currentTag.value === 'uncategorized') {
      // 未分类：获取所有日程，然后筛选出没有关联任何标签的日程
      const response = await axios.get('http://localhost:8080/api/schedule/list')
      const allSchedules = response.data.data || []

      // 获取所有有标签的日程ID（返回的是 [{id, type}] 数组）
      const allTagsResponse = await axios.get('http://localhost:8080/api/tags/filter', {
        params: { targetType: 'SCHEDULE' }
      })
      // 提取出 id 数组
      const taggedScheduleIds = (allTagsResponse.data.data || []).map(item => item.id)

      // 筛选出没有标签的日程
      scheduleList.value = allSchedules.filter(s => !taggedScheduleIds.includes(s.id))

    } else if (currentTag.value !== 'all') {
      // 有标签筛选
      const filterResponse = await axios.get('http://localhost:8080/api/tags/filter', {
        params: {
          tagId: currentTag.value,
          targetType: 'SCHEDULE'
        }
      })
      // 提取出 id 数组
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
      // 所有日程
      const response = await axios.get('http://localhost:8080/api/schedule/list')
      scheduleList.value = response.data.data || []
    }

    // 重置分页
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

/* 分组样式 */
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
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  transition: background 0.2s;
}

.schedule-item:last-child {
  border-bottom: none;
}

.schedule-item:hover {
  background-color: #f5f7fa;
}

.schedule-title {
  flex: 1;
  margin-left: 12px;
  font-size: 14px;
}

.schedule-date {
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}

/* 状态样式 */
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

/* 分组底部：分页栏 */
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
</style>