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

const loadMonthData = async () => {
  try {
    const res = await getMonthData(currentYear.value, currentMonth.value);
    allEvents.value = res.data?.events || {};

    if (viewMode.value === 'month') {
      generateCalendar();
    } else if (viewMode.value === 'week') {
      generateWeekView();
    } else if (viewMode.value === 'day') {
      const targetDate = selectedDate.value ? dayjs(selectedDate.value) : dayjs();
      const dateKey = targetDate.format('YYYY-MM-DD');
      const events = allEvents.value[dateKey] || [];
      currentDayData.value = {
        dateTitle: targetDate.format('YYYY年MM月DD日'),
        weekday: getShortWeekday(targetDate.day()),
        events: events
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
  background: #fafafa;
  padding: 24px 32px;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-section h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 500;
  color: #1a1a1a;
}

.logo-section p {
  margin: 6px 0 0;
  color: #6b7280;
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
  background: white;
  font-size: 22px;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.month-btn:hover {
  background: #e5e7eb;
  color: #1a1a1a;
}

.year-month-selector {
  display: flex;
  gap: 8px;
  background: white;
  padding: 4px 12px;
  border-radius: 30px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.year-select, .month-select {
  padding: 6px 8px;
  border: none;
  background: transparent;
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
  cursor: pointer;
  outline: none;
  text-align: center;
}

.today-btn {
  padding: 6px 20px;
  border-radius: 25px;
  border: none;
  background: white;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.today-btn:hover {
  background: #e5e7eb;
  color: #1a1a1a;
}

.view-switch {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  background: white;
  padding: 6px;
  border-radius: 40px;
  width: fit-content;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.view-btn {
  padding: 8px 24px;
  border: none;
  background: transparent;
  border-radius: 30px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.2s;
}

.view-btn:hover {
  background: #f3f4f6;
}

.view-btn.active {
  background: #e5e7eb;
  color: #1a1a1a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.legend {
  display: flex;
  justify-content: flex-start;
  gap: 24px;
  margin-bottom: 20px;
  padding: 10px 16px;
  background: white;
  border-radius: 30px;
  width: fit-content;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
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
  border: 1px solid #6b7280;
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
  color: #6b7280;
  background: #f5f0e8;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.calendar-grid-wrapper {
  background: #f5f0e8;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.calendar-day {
  background: #f5f0e8;
  border-radius: 12px;
  min-height: 100px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
}

.calendar-day:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.other-month {
  background: #fafafa;
  opacity: 0.6;
}

.today {
  border: 2px solid #6b7280;
  background: #ede8e0;
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
  color: #1a1a1a;
}

.today .day-number {
  color: #1a1a1a;
  font-weight: 700;
}

.weekday-name {
  font-size: 11px;
  color: #9ca3af;
  margin-left: 4px;
}

.today-badge-mark {
  font-size: 9px;
  background: #6b7280;
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
  background: #fbbf24;
}

.event-dot.note {
  background: #34d399;
}

.event-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
  font-size: 11px;
  color: #1a1a1a;
}

.more-events {
  font-size: 10px;
  color: #9ca3af;
  padding: 2px 6px;
}

.day-view-wrapper {
  background: #f5f0e8;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.day-view-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.day-view-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #1a1a1a;
}

.day-view-weekday {
  font-size: 14px;
  color: #6b7280;
}

.day-events-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.day-event-item {
  background: #f5f0e8;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
}

.day-event-item.schedule {
  border-left: 4px solid #fbbf24;
}

.day-event-item.note {
  border-left: 4px solid #34d399;
}

.day-event-type {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.day-event-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.day-event-time {
  font-size: 12px;
  color: #fbbf24;
  margin-bottom: 6px;
}

.day-event-content {
  font-size: 13px;
  color: #374151;
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
  color: #34d399;
  background: #f0fdf4;
  padding: 2px 10px;
  border-radius: 16px;
}

.empty-day {
  text-align: center;
  padding: 60px 20px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

@media (max-width: 900px) {
  .calendar-page { padding: 16px; }
  .calendar-grid-wrapper { padding: 10px; }
  .calendar-grid { gap: 6px; }
  .calendar-day { min-height: 90px; padding: 8px; }
}

@media (max-width: 700px) {
  .calendar-day { min-height: 70px; }
  .event-item { display: none; }
  .view-switch { margin-bottom: 12px; }
  .view-btn { padding: 6px 16px; font-size: 12px; }
}
</style>