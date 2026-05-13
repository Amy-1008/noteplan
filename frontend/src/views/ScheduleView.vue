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
              <span class="schedule-date">{{ formatDate(schedule.startTime) }}</span>
            </div>
          </div>
          <!-- 分页栏：每页条数选择 + 页码切换 -->
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
              <span class="schedule-date">{{ formatDate(schedule.startTime) }}</span>
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
              <span class="schedule-date">{{ formatDate(schedule.startTime) }}</span>
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
              <span class="schedule-date">{{ formatDate(schedule.startTime) }}</span>
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
const currentTag = ref('all')

// 每个分组独立的当前页
const currentPageMap = ref({
  expired: 1,
  nextWeek: 1,
  other: 1,
  completed: 1
})

// 每个分组独立的每页条数
const pageSizeMap = ref({
  expired: 5,
  nextWeek: 5,
  other: 5,
  completed: 5
})

// ---------- 辅助函数 ----------
const isExpired = (schedule) => {
  if (schedule.completed) return false
  return new Date(schedule.startTime) < new Date(new Date().setHours(0, 0, 0, 0))
}

const isNextWeek = (schedule) => {
  if (schedule.completed) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const targetDate = new Date(schedule.startTime)
  targetDate.setHours(0, 0, 0, 0)
  const diffDays = Math.ceil((targetDate - today) / (1000 * 60 * 60 * 24))
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
  currentPageMap.value[group] = 1  // 改变每页条数时，回到第一页
}

const openAddDialog = () => {
  ElMessage.info('添加日程弹窗待实现')
}

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}-${date.getDate()}`
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

const fetchScheduleList = async () => {
  try {
    const params = currentTag.value !== 'all' ? { tagId: currentTag.value } : {}
    const response = await axios.get('http://localhost:8080/api/schedule/list', { params })
    if (response.data.code === 200) {
      scheduleList.value = response.data.data
      // 重置分页
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
  fetchScheduleList()
})
</script>

<style scoped>
.schedule-container {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
}

/* 左侧标签栏 */
.schedule-sidebar {
  width: 220px;
  background-color: white;
  border-right: 1px solid #e4e7ed;
  padding: 20px 0;
  overflow-y: auto;
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