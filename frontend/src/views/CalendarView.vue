<template>
  <div class="calendar-page">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="logo-section">
        <h1>📅 我的日历</h1>
        <p>日程 · 笔记</p>
      </div>
      <div class="month-section">
        <button class="month-btn" @click="prevMonth">‹</button>

        <div class="year-month-selector">
          <select v-model="selectedYear" @change="onYearChange" class="year-select">
            <option v-for="year in yearOptions" :key="year" :value="year">
              {{ year }}年
            </option>
          </select>
          <select v-model="selectedMonth" @change="onMonthChange" class="month-select">
            <option v-for="(month, index) in monthOptions" :key="index" :value="index + 1">
              {{ month }}月
            </option>
          </select>
        </div>

        <button class="month-btn" @click="nextMonth">›</button>
        <button class="today-btn" @click="today">今天</button>
      </div>
    </div>

    <!-- 视图切换 -->
    <div class="view-switch">
      <button class="view-btn" :class="{ active: viewMode === 'month' }" @click="switchView('month')">月视图</button>
      <button class="view-btn" :class="{ active: viewMode === 'week' }" @click="switchView('week')">周视图</button>
      <button class="view-btn" :class="{ active: viewMode === 'day' }" @click="switchView('day')">日视图</button>
    </div>

    <!-- 图例 -->
    <div class="legend">
      <div class="legend-item">
        <span class="legend-badge schedule-badge"></span>
        <span>日程</span>
      </div>
      <div class="legend-item">
        <span class="legend-badge note-badge"></span>
        <span>笔记</span>
      </div>
      <div class="legend-item">
        <span class="legend-badge today-badge"></span>
        <span>今天</span>
      </div>
    </div>

    <!-- 星期标题 -->
    <div class="weekdays" v-if="viewMode !== 'day'">
      <div v-for="week in weekdays" :key="week" class="weekday">{{ week }}</div>
    </div>

    <!-- 月视图 -->
    <div class="calendar-grid-wrapper" v-if="viewMode === 'month'">
      <div class="calendar-grid">
        <div
            v-for="(day, idx) in calendarDays"
            :key="idx"
            class="calendar-day"
            :class="{ 'other-month': day.isOtherMonth, 'today': day.isToday }"
            @click="openDayDetail(day)"
        >
          <div class="day-header">
            <span class="day-number">{{ day.dayNum }}</span>
            <span v-if="day.isToday" class="today-badge-mark">今天</span>
          </div>
          <div class="day-events">
            <template v-for="(event, idx) in day.events.slice(0, 2)" :key="event.id">
              <div class="event-item" :class="event.type">
                <span class="event-dot" :class="event.type"></span>
                <span class="event-title">{{ truncateTitle(event.title, 5) }}</span>
              </div>
            </template>
            <div v-if="day.events.length > 2" class="more-events">
              +{{ day.events.length - 2 }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 周视图 -->
    <div class="calendar-grid-wrapper" v-if="viewMode === 'week'">
      <div class="calendar-grid">
        <div
            v-for="(day, idx) in weekDays"
            :key="idx"
            class="calendar-day"
            :class="{ 'today': day.isToday }"
            @click="openDayDetail(day)"
        >
          <div class="day-header">
            <span class="day-number">{{ day.dayNum }}</span>
            <span class="weekday-name">{{ getShortWeekday(day.weekday) }}</span>
            <span v-if="day.isToday" class="today-badge-mark">今天</span>
          </div>
          <div class="day-events">
            <template v-for="(event, idx) in day.events.slice(0, 2)" :key="event.id">
              <div class="event-item" :class="event.type">
                <span class="event-dot" :class="event.type"></span>
                <span class="event-title">{{ truncateTitle(event.title, 5) }}</span>
              </div>
            </template>
            <div v-if="day.events.length > 2" class="more-events">
              +{{ day.events.length - 2 }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 日视图 -->
    <div class="day-view-wrapper" v-if="viewMode === 'day'">
      <div class="day-view-header">
        <h2>{{ currentDayData.dateTitle }}</h2>
        <div class="day-view-weekday">{{ currentDayData.weekday }}</div>
      </div>
      <div class="day-events-list">
        <div v-if="currentDayData.events.length === 0" class="empty-day">
          <div class="empty-icon">📭</div>
          <div>这一天没有安排</div>
        </div>
        <div v-for="event in currentDayData.events" :key="event.id" class="day-event-item" :class="event.type">
          <div class="day-event-type">{{ event.type === 'schedule' ? '📅 日程' : '📝 笔记' }}</div>
          <div class="day-event-title">{{ event.title }}</div>
          <div class="day-event-time" v-if="event.time">{{ event.time }}</div>
          <div class="day-event-content" v-if="event.content">{{ event.content }}</div>
          <div class="day-event-tags" v-if="event.tags && event.tags.length">
            <span v-for="tag in event.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-drawer v-model="drawerVisible" :title="selectedDateTitle" direction="rtl" size="480px">
      <DailyDetail :date="selectedDate" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import dayjs from 'dayjs';
import axios from 'axios'; // ✅ 添加 axios 导入
import { getMonthData } from '@/api/calendar';
import DailyDetail from '@/components/DailyDetail.vue';

const currentYear = ref(dayjs().year());
const currentMonth = ref(dayjs().month() + 1);
const selectedYear = ref(dayjs().year());
const selectedMonth = ref(dayjs().month() + 1);
const viewMode = ref('month');
const calendarDays = ref([]);
const weekDays = ref([]);
const currentDayData = ref({ dateTitle: '', weekday: '', events: [] });
const drawerVisible = ref(false);
const selectedDate = ref(dayjs().format('YYYY-MM-DD'));
const selectedDateTitle = ref('');
const allEvents = ref({});

const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
const monthOptions = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'];
const yearOptions = ref([]);

for (let i = dayjs().year() - 10; i <= dayjs().year() + 10; i++) {
  yearOptions.value.push(i);
}

const truncateTitle = (title, maxLen) => {
  if (!title) return '';
  return title.length > maxLen ? title.slice(0, maxLen) + '...' : title;
};

const getShortWeekday = (weekday) => {
  const short = ['日', '一', '二', '三', '四', '五', '六'];
  return short[weekday];
};

// 生成重复日程
const generateRecurringEvents = (schedule, startDate, endDate) => {
  const events = [];
  const start = dayjs(schedule.startTime || schedule.endTime);
  const end = dayjs(schedule.endTime);
  const duration = end.diff(start, 'minute');

  let current = start.clone();
  const maxIterations = 50; // 防止无限循环

  let i = 0;
  while (current.isBefore(endDate) && i < maxIterations) {
    const dateKey = current.format('YYYY-MM-DD');
    const eventEnd = current.add(duration, 'minute');

    // ✅ 检查是否在当月范围内
    if (current.month() + 1 === currentMonth.value && current.year() === currentYear.value) {
      events.push({
        id: `${schedule.id}-${i}`,
        type: 'schedule',
        title: schedule.title,
        time: `${current.format('HH:mm')} - ${eventEnd.format('HH:mm')}`,
        content: schedule.remark,
        tags: schedule.tags,
        originalId: schedule.id
      });
    }

    // 根据重复规则生成下一次
    switch (schedule.repeatRule) {
      case 'daily':
        current = current.add(1, 'day');
        break;
      case 'weekly':
        current = current.add(1, 'week');
        break;
      case 'monthly':
        current = current.add(1, 'month');
        break;
      case 'yearly':
        current = current.add(1, 'year');
        break;
      case 'workday':
        // 跳过周末
        current = current.add(1, 'day');
        while (current.day() === 0 || current.day() === 6) {
          current = current.add(1, 'day');
        }
        break;
      default:
        current = endDate; // 结束循环
    }
    i++;
  }

  return events;
};

const loadMonthData = async () => {
  try {
    const res = await getMonthData(currentYear.value, currentMonth.value);
    const events = res.data?.events || {};

    // ✅ 获取所有日程（包括重复日程）
    const scheduleRes = await axios.get('http://localhost:8080/api/schedule/list');
    const allSchedules = scheduleRes.data.data || [];

    // ✅ 生成重复日程
    const startDate = dayjs(`${currentYear.value}-${currentMonth.value}-01`);
    const endDate = startDate.endOf('month');

    allSchedules.forEach(schedule => {
      if (schedule.repeatRule && schedule.repeatRule !== 'none') {
        const recurringEvents = generateRecurringEvents(schedule, startDate, endDate);
        recurringEvents.forEach(event => {
          const dateKey = event.date || dayjs(event.time.split(' - ')[0], 'HH:mm').format('YYYY-MM-DD');
          if (!events[dateKey]) events[dateKey] = [];
          events[dateKey].push(event);
        });
      }
    });

    allEvents.value = events;

    if (viewMode.value === 'month') {
      generateCalendar();
    } else if (viewMode.value === 'week') {
      generateWeekView();
    } else if (viewMode.value === 'day') {
      const targetDate = selectedDate.value ? dayjs(selectedDate.value) : dayjs();
      const dateKey = targetDate.format('YYYY-MM-DD');
      const dayEvents = events[dateKey] || [];
      currentDayData.value = {
        dateTitle: targetDate.format('YYYY年MM月DD日'),
        weekday: getShortWeekday(targetDate.day()),
        events: dayEvents
      };
    }
  } catch (error) {
    console.error('加载失败', error);
    allEvents.value = {};
    generateCalendar();
  }
};

const generateCalendar = () => {
  const firstDay = dayjs(`${currentYear.value}-${currentMonth.value}-01`);
  const startDay = firstDay.startOf('month').startOf('week');
  const days = [];

  for (let i = 0; i < 42; i++) {
    const currentDate = startDay.add(i, 'day');
    const isCurrentMonth = currentDate.month() + 1 === currentMonth.value;
    const dateKey = currentDate.format('YYYY-MM-DD');
    const events = allEvents.value[dateKey] || [];

    days.push({
      date: dateKey,
      dayNum: currentDate.date(),
      isToday: currentDate.isSame(dayjs(), 'day'),
      isOtherMonth: !isCurrentMonth,
      events: events
    });
  }

  calendarDays.value = days;
};

const generateWeekView = () => {
  const today = dayjs();
  const startOfWeek = today.startOf('week');
  const days = [];

  for (let i = 0; i < 7; i++) {
    const currentDate = startOfWeek.add(i, 'day');
    const dateKey = currentDate.format('YYYY-MM-DD');
    const events = allEvents.value[dateKey] || [];

    days.push({
      date: dateKey,
      dayNum: currentDate.date(),
      weekday: currentDate.day(),
      isToday: currentDate.isSame(dayjs(), 'day'),
      events: events
    });
  }

  weekDays.value = days;
};

const generateDayView = () => {
  const targetDate = selectedDate.value ? dayjs(selectedDate.value) : dayjs();
  const dateKey = targetDate.format('YYYY-MM-DD');
  const events = allEvents.value[dateKey] || [];

  currentDayData.value = {
    dateTitle: targetDate.format('YYYY年MM月DD日'),
    weekday: getShortWeekday(targetDate.day()),
    events: events
  };
};

const switchView = (view) => {
  viewMode.value = view;
  if (view === 'week') {
    generateWeekView();
  } else if (view === 'day') {
    generateDayView();
  }
};

const openDayDetail = (day) => {
  if (day.isOtherMonth) return;
  selectedDate.value = day.date;
  selectedDateTitle.value = dayjs(day.date).format('YYYY年MM月DD日 (dddd)');
  drawerVisible.value = true;

  if (viewMode.value === 'day') {
    const events = allEvents.value[day.date] || [];
    currentDayData.value = {
      dateTitle: dayjs(day.date).format('YYYY年MM月DD日'),
      weekday: getShortWeekday(dayjs(day.date).day()),
      events: events
    };
  }
};

const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12;
    currentYear.value--;
  } else {
    currentMonth.value--;
  }
  selectedYear.value = currentYear.value;
  selectedMonth.value = currentMonth.value;
  loadMonthData();
};

const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1;
    currentYear.value++;
  } else {
    currentMonth.value++;
  }
  selectedYear.value = currentYear.value;
  selectedMonth.value = currentMonth.value;
  loadMonthData();
};

const onYearChange = () => {
  currentYear.value = selectedYear.value;
  loadMonthData();
};

const onMonthChange = () => {
  currentMonth.value = selectedMonth.value;
  loadMonthData();
};

const today = () => {
  currentYear.value = dayjs().year();
  currentMonth.value = dayjs().month() + 1;
  selectedYear.value = currentYear.value;
  selectedMonth.value = currentMonth.value;
  selectedDate.value = dayjs().format('YYYY-MM-DD');

  loadMonthData();
  if (viewMode.value === 'week') {
    generateWeekView();
  } else if (viewMode.value === 'day') {
    const events = allEvents.value[selectedDate.value] || [];
    currentDayData.value = {
      dateTitle: dayjs().format('YYYY年MM月DD日'),
      weekday: getShortWeekday(dayjs().day()),
      events: events
    };
  }
};

onMounted(() => {
  loadMonthData();
});
</script>

<style scoped>
* {
  box-sizing: border-box;
}
.calendar-page {
  min-height: 100%;
  background: var(--bg-primary);
  padding: 24px 32px;
  transition: background 0.3s;
}
:deep(.el-drawer) {
  background: var(--card-bg);
  transition: background 0.3s;
}

:deep(.el-drawer__header) {
  border-bottom: 1px solid var(--card-border);
  padding: 16px 20px;
  margin: 0;
}
:deep(.el-drawer__title) {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
}

:deep(.el-drawer__close-btn) {
  color: var(--text-secondary);
}

:deep(.el-drawer__close-btn:hover) {
  color: var(--text-primary);
}

:deep(.el-drawer__body) {
  padding: 0;
  background: var(--card-bg);
  transition: background 0.3s;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--card-border);
}

.logo-section h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 500;
  color: var(--text-primary);
}

.logo-section p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.month-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.month-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: var(--card-bg);
  font-size: 22px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.month-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.year-month-selector {
  display: flex;
  gap: 8px;
  background: var(--card-bg);
  padding: 4px 12px;
  border-radius: 30px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.year-select, .month-select {
  padding: 6px 8px;
  border: none;
  background: transparent;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
  outline: none;
  text-align: center;
}

.today-btn {
  padding: 6px 20px;
  border-radius: 25px;
  border: none;
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.today-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.view-switch {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  background: var(--card-bg);
  padding: 6px;
  border-radius: 40px;
  width: fit-content;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.view-btn {
  padding: 8px 24px;
  border: none;
  background: transparent;
  border-radius: 30px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.view-btn:hover {
  background: var(--bg-hover);
}

.view-btn.active {
  background: var(--bg-hover);
  color: var(--text-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.legend {
  display: flex;
  justify-content: flex-start;
  gap: 24px;
  margin-bottom: 20px;
  padding: 10px 16px;
  background: var(--card-bg);
  border-radius: 30px;
  width: fit-content;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-badge {
  width: 20px;
  height: 10px;
  border-radius: 5px;
}

.schedule-badge {
  background: #fbbf24;
}

.note-badge {
  background: #34d399;
}

.today-badge {
  background: #dbeafe;
  border: 1px solid var(--text-secondary);
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}

.weekday {
  text-align: center;
  padding: 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--card-bg);
  border-radius: 10px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.calendar-grid-wrapper {
  background: var(--card-bg);
  border-radius: 14px;
  padding: 16px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.calendar-day {
  background: var(--card-bg);
  border-radius: 12px;
  min-height: 100px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--card-border);
}

.calendar-day:hover {
  transform: translateY(-2px);
  box-shadow: var(--card-shadow-hover);
}

.other-month {
  background: var(--bg-hover);
  opacity: 0.6;
}

.today {
  border: 2px solid var(--accent);
  background: var(--accent-soft);
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.day-number {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.today .day-number {
  color: var(--accent);
  font-weight: 700;
}

.weekday-name {
  font-size: 11px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.today-badge-mark {
  font-size: 9px;
  background: var(--accent);
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
}

.day-events {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  padding: 3px 6px;
  border-radius: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.event-dot.schedule {
  background: var(--event-schedule-color);
}

.event-dot.note {
  background: var(--event-note-color);
}

.event-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
  font-size: 11px;
  color: var(--text-primary);
}

.more-events {
  font-size: 10px;
  color: var(--text-secondary);
  padding: 2px 6px;
}

.day-view-wrapper {
  background: var(--card-bg);
  border-radius: 14px;
  padding: 24px;
  box-shadow: var(--card-shadow);
  border: 1px solid var(--card-border);
}

.day-view-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--card-border);
}

.day-view-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: var(--text-primary);
}

.day-view-weekday {
  font-size: 14px;
  color: var(--text-secondary);
}

.day-events-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.day-event-item {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
  border: 1px solid var(--card-border);
}

.day-event-item.schedule {
  border-left: 4px solid #fbbf24;
}

.day-event-item.note {
  border-left: 4px solid #34d399;
}

.day-event-type {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.day-event-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.day-event-time {
  font-size: 12px;
  color: #fbbf24;
  margin-bottom: 6px;
}

.day-event-content {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
}

.day-event-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.tag {
  font-size: 11px;
  color: var(--event-tag-color);
  background: var(--event-tag-bg);
  padding: 2px 10px;
  border-radius: 16px;
}
.empty-day {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}
</style>