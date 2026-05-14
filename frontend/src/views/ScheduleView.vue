<template>
  <div class="schedule-container">
    <!-- 左侧：标签分类块 -->
    <div class="schedule-sidebar">
      <div class="sidebar-header">分类</div>
      <div class="tag-list">
        <div
            class="tag-item"
            :class="{ active: currentTag === 'all' }"
            @click="currentTag = 'all'"
        >
          📋 所有
        </div>
        <div
            v-for="tag in tagList"
            :key="tag.id"
            class="tag-item"
            :class="{ active: currentTag === tag.id }"
            @click="currentTag = tag.id"
        >
          #{{ tag.name }}
        </div>
        <div v-if="tagList.length === 0" class="tag-empty">暂无标签</div>
      </div>
    </div>

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
            >
              <el-checkbox v-model="schedule.completed" @change="toggleComplete(schedule)" />
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
            >
              <el-checkbox v-model="schedule.completed" @change="toggleComplete(schedule)" />
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
            >
              <el-checkbox v-model="schedule.completed" @change="toggleComplete(schedule)" />
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
            >
              <el-checkbox v-model="schedule.completed" disabled />
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
        :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入日程标题" />
        </el-form-item>

        <!-- 时间类型选择：时间点 / 时间段 -->
        <el-form-item label="时间类型" prop="timeType">
          <el-radio-group v-model="formData.timeType">
            <el-radio label="point">时间点</el-radio>
            <el-radio label="period">时间段</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 时间点模式：只选结束时间 -->
        <el-form-item v-if="formData.timeType === 'point'" label="时间" prop="endTime">
          <el-date-picker
              v-model="formData.endTime"
              type="datetime"
              placeholder="选择日期时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm:ss"
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
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker
                v-model="formData.endTime"
                type="datetime"
                placeholder="选择结束时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
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
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-select v-model="formData.tagIds" multiple placeholder="选择标签" style="width: 100%">
            <el-option
                v-for="tag in tagList"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
            />
          </el-select>
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

// ---------- 数据 ----------
const scheduleList = ref([])
const tagList = ref([])
const noteList = ref([])
const currentTag = ref('all')
const dialogVisible = ref(false)
const formRef = ref(null)

// 表单数据
const formData = ref({
  title: '',
  timeType: 'point',           // 'point' 或 'period'
  startTime: '',               // 时间段时用
  endTime: '',                 // 时间点/时间段都用
  repeatRule: 'none',
  remark: '',
  tagIds: [],
  noteIds: []
})

// 表单校验规则
const formRules = {
  title: [{ required: true, message: '请输入日程标题', trigger: 'blur' }],
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
  }]
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

// ---------- 辅助函数 ----------
// 格式化日程时间显示
const formatScheduleTime = (schedule) => {
  if (!schedule.endTime) return ''

  const formatTime = (dateStr) => {
    const date = new Date(dateStr)
    return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }

  // 有时间点：startTime 为 null
  if (!schedule.startTime) {
    return formatTime(schedule.endTime)
  }

  // 时间段：显示 开始 ~ 结束
  return `${formatTime(schedule.startTime)} ~ ${formatTime(schedule.endTime)}`
}

// 判断是否过期
const isExpired = (schedule) => {
  if (schedule.completed) return false
  return new Date(schedule.endTime) < new Date()
}

// 判断是否在接下来7天内
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
const toggleComplete = async (schedule) => {
  try {
    await axios.put('http://localhost:8080/api/schedule/complete', null, {
      params: { id: schedule.id, completed: schedule.completed ? 1 : 0 }
    })
    ElMessage.success(schedule.completed ? '已完成' : '已取消完成')
    fetchScheduleList()
  } catch (error) {
    schedule.completed = !schedule.completed
    ElMessage.error('操作失败')
  }
}

const handlePageChange = (group, page) => {
  currentPageMap.value[group] = page
}

const handlePageSizeChange = (group, size) => {
  pageSizeMap.value[group] = size
  currentPageMap.value[group] = 1
}

const openAddDialog = () => {
  formData.value = {
    title: '',
    timeType: 'point',
    startTime: '',
    endTime: '',
    repeatRule: 'none',
    remark: '',
    tagIds: [],
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
      // 构建提交数据
      const submitData = {
        title: formData.value.title,
        repeatRule: formData.value.repeatRule,
        remark: formData.value.remark,
        tagIds: formData.value.tagIds,
        noteIds: formData.value.noteIds
      }

      if (formData.value.timeType === 'point') {
        // 时间点：startTime = null, endTime = 填的值
        submitData.startTime = null
        submitData.endTime = formData.value.endTime
      } else {
        // 时间段：startTime 和 endTime 都填
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
    const response = await axios.get('http://localhost:8080/api/tag/list')
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
    const params = currentTag.value !== 'all' ? { tagId: currentTag.value } : {}
    const response = await axios.get('http://localhost:8080/api/schedule/list', { params })
    if (response.data.code === 200) {
      scheduleList.value = response.data.data
      currentPageMap.value = {
        expired: 1,
        nextWeek: 1,
        other: 1,
        completed: 1
      }
    }
  } catch (error) {
    console.error('获取日程失败', error)
  }
}

watch(currentTag, () => fetchScheduleList())

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

/* 左侧标签栏 */
.schedule-sidebar {
  width: 220px;
  background-color: white;
  border-right: 1px solid #e4e7ed;
  padding: 20px 0;
  overflow-y: auto;
  flex-shrink: 0;
}

.sidebar-header {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding: 0 16px 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 12px;
}

.tag-list {
  display: flex;
  flex-direction: column;
}

.tag-item {
  padding: 10px 16px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.tag-item.active {
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.tag-empty {
  padding: 10px 16px;
  font-size: 13px;
  color: #c0c4cc;
  text-align: center;
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