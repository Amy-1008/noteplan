<template>
  <div class="schedule-container">
    <!-- 右侧主内容区 -->
    <div class="schedule-main">
      <!-- 头部 -->
      <div class="schedule-header">
        <h2>日程</h2>
        <el-button type="primary" circle @click="openAddDialog">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <!-- 分组日程列表 -->
      <div class="schedule-groups">
        <!-- 1. 已过期 -->
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
                  v-model="schedule.completed"
                  @change="toggleComplete(schedule)"
              />
              <span class="schedule-title">{{ schedule.title }}</span>
              <span class="schedule-date">{{ formatDate(schedule.startTime) }}</span>
            </div>
            <!-- 分页控件（已过期分组） -->
            <el-pagination
                v-if="expiredList.length > pageSize"
                small
                layout="prev, pager, next"
                :total="expiredList.length"
                :page-size="pageSize"
                v-model:current-page="currentPageMap.expired"
                @current-change="handlePageChange('expired', $event)"
            />
          </div>
        </div>

        <!-- 2. 接下来7天 -->
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
                  v-model="schedule.completed"
                  @change="toggleComplete(schedule)"
              />
              <span class="schedule-title">{{ schedule.title }}</span>
              <span class="schedule-date">{{ formatDate(schedule.startTime) }}</span>
            </div>
            <el-pagination
                v-if="nextWeekList.length > pageSize"
                small
                layout="prev, pager, next"
                :total="nextWeekList.length"
                :page-size="pageSize"
                v-model:current-page="currentPageMap.nextWeek"
                @current-change="handlePageChange('nextWeek', $event)"
            />
          </div>
        </div>

        <!-- 3. 其他时间 -->
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
                  v-model="schedule.completed"
                  @change="toggleComplete(schedule)"
              />
              <span class="schedule-title">{{ schedule.title }}</span>
              <span class="schedule-date">{{ formatDate(schedule.startTime) }}</span>
            </div>
            <el-pagination
                v-if="otherList.length > pageSize"
                small
                layout="prev, pager, next"
                :total="otherList.length"
                :page-size="pageSize"
                v-model:current-page="currentPageMap.other"
                @current-change="handlePageChange('other', $event)"
            />
          </div>
        </div>

        <!-- 4. 已完成 -->
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
                  v-model="schedule.completed"
                  disabled
              />
              <span class="schedule-title">{{ schedule.title }}</span>
              <span class="schedule-date">{{ formatDate(schedule.startTime) }}</span>
            </div>
            <el-pagination
                v-if="completedList.length > pageSize"
                small
                layout="prev, pager, next"
                :total="completedList.length"
                :page-size="pageSize"
                v-model:current-page="currentPageMap.completed"
                @current-change="handlePageChange('completed', $event)"
            />
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-if="noData" description="暂无日程" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getScheduleList, updateScheduleComplete } from '@/api/schedule' // 假设的API
import axios from 'axios'

// ---------- 数据 ----------
const scheduleList = ref([])        // 所有日程
const currentTag = ref('all')       // 当前选中的标签（接收自左侧组件）
const pageSize = 5                  // 每组最多显示5条

// 分页当前页（每个分组独立）
const currentPageMap = ref({
  expired: 1,
  nextWeek: 1,
  other: 1,
  completed: 1
})

// ---------- 辅助函数 ----------
// 判断日程是否过期（未完成且时间 < 今天）
const isExpired = (schedule) => {
  if (schedule.completed) return false
  return new Date(schedule.startTime) < new Date(new Date().setHours(0, 0, 0, 0))
}

// 判断是否在接下来7天内
const isNextWeek = (schedule) => {
  if (schedule.completed) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const targetDate = new Date(schedule.startTime)
  targetDate.setHours(0, 0, 0, 0)
  const diffDays = Math.ceil((targetDate - today) / (1000 * 60 * 60 * 24))
  return diffDays >= 0 && diffDays <= 7
}

// 分组后的列表（未完成的按时间分组）
const expiredList = computed(() => {
  return scheduleList.value.filter(s => !s.completed && isExpired(s))
})

const nextWeekList = computed(() => {
  return scheduleList.value.filter(s => !s.completed && !isExpired(s) && isNextWeek(s))
})

const otherList = computed(() => {
  return scheduleList.value.filter(s => !s.completed && !isExpired(s) && !isNextWeek(s))
})

const completedList = computed(() => {
  return scheduleList.value.filter(s => s.completed)
})

// 分页后的数据（每个分组独立分页）
const paginatedGroups = computed(() => {
  const paginate = (list, page) => {
    const start = (page - 1) * pageSize
    return list.slice(start, start + pageSize)
  }
  return {
    expired: paginate(expiredList.value, currentPageMap.value.expired),
    nextWeek: paginate(nextWeekList.value, currentPageMap.value.nextWeek),
    other: paginate(otherList.value, currentPageMap.value.other),
    completed: paginate(completedList.value, currentPageMap.value.completed)
  }
})

const noData = computed(() => {
  return scheduleList.value.length === 0
})

// ---------- 事件 ----------
const toggleComplete = async (schedule) => {
  try {
    await updateScheduleComplete(schedule.id, schedule.completed)
    ElMessage.success(schedule.completed ? '已完成' : '已取消完成')
  } catch (error) {
    schedule.completed = !schedule.completed // 回滚
    ElMessage.error('操作失败')
  }
}

const handlePageChange = (group, page) => {
  currentPageMap.value[group] = page
}

const openAddDialog = () => {
  // TODO: 弹窗添加日程（后续实现）
  ElMessage.info('添加日程弹窗待实现')
}

// 格式化日期（MM-DD）
const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}-${date.getDate()}`
}

// ---------- 监听标签切换（接收左侧组件传来的值）----------
// 假设左侧组件通过事件传值，或者使用全局状态（pinia）
// 这里用一个 watch 示例，实际实现时替换成 props 或 store
watch(currentTag, (newTag) => {
  fetchScheduleList(newTag)
})

// ---------- 加载数据 ----------
// 调用后端接口
const fetchScheduleList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/schedule/list')
    if (response.data.code === 200) {
      scheduleList.value = response.data.data
    }
  } catch (error) {
    console.error('获取日程失败', error)
  }
}

onMounted(() => {
  fetchScheduleList()
})
</script>

<style scoped>
.schedule-container {
  display: flex;
  height: 100%;
  background-color: #f5f7fa;
}

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
  margin-bottom: 24px;
}

.group-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 4px solid;
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
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
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

/* 分页 */
:deep(.el-pagination) {
  padding: 12px 16px;
  justify-content: flex-end;
  background: white;
  border-top: 1px solid #ebeef5;
}
</style>