<template>
  <div class="calendar-page">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <div class="logo-section">
        <h1>📅 我的日历</h1>
        <p>日程 · 备忘录</p>
      </div>
      <div class="month-section">
        <button class="month-btn" @click="prevMonth">‹</button>
        <span class="month-text">{{ currentYear }}年 {{ currentMonth }}月</span>
        <button class="month-btn" @click="nextMonth">›</button>
        <button class="today-btn" @click="today">今天</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-number">{{ totalEvents }}</div>
        <div class="stat-label">总事件</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ totalSchedules }}</div>
        <div class="stat-label">日程</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ totalNotes }}</div>
        <div class="stat-label">备忘录</div>
      </div>
      <div class="stat-card">
        <div class="stat-number">{{ markedDays }}</div>
        <div class="stat-label">有记录</div>
      </div>
    </div>

    <!-- 图例（放在统计卡片下面） -->
    <div class="legend">
      <div class="legend-item">
        <span class="legend-badge schedule-badge"></span>
        <span>日程</span>
      </div>
      <div class="legend-item">
        <span class="legend-badge note-badge"></span>
        <span>备忘录</span>
      </div>
      <div class="legend-item">
        <span class="legend-badge today-badge"></span>
        <span>今天</span>
      </div>
    </div>

    <!-- 星期标题 -->
    <div class="weekdays">
      <div v-for="week in weekdays" :key="week" class="weekday">{{ week }}</div>
    </div>

    <!-- 日历网格 -->
    <div class="calendar-grid-wrapper">
      <div class="calendar-grid">
        <div
            v-for="(day, idx) in calendarDays"
            :key="idx"
            class="calendar-day"
            :class="{
            'other-month': day.isOtherMonth,
            'today': day.isToday
          }"
            @click="openDayDetail(day)"
        >
          <div class="day-header">
            <span class="day-number">{{ day.dayNum }}</span>
            <span v-if="day.isToday" class="today-badge-mark">今天</span>
          </div>
          <div class="day-events">
            <div v-for="event in day.events.slice(0, 2)" :key="event.id" class="event-item" :class="event.type">
              <span class="event-type">{{ event.type === 'schedule' ? '📅' : '📝' }}</span>
              <span class="event-title">{{ event.title }}</span>
            </div>
            <div v-if="day.eventCount > 2" class="more-events">
              +{{ day.eventCount - 2 }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-drawer
        v-model="drawerVisible"
        :title="selectedDateTitle"
        direction="rtl"
        size="480px"
    >
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
const calendarDays = ref([]);
const drawerVisible = ref(false);
const selectedDate = ref('');
const selectedDateTitle = ref('');

const totalEvents = ref(0);
const totalSchedules = ref(0);
const totalNotes = ref(0);
const markedDays = ref(0);

const weekdays = ['日', '一', '二', '三', '四', '五', '六'];

const loadMonthData = async () => {
  try {
    const res = await getMonthData(currentYear.value, currentMonth.value);
    const data = res.data;

    totalEvents.value = data.totalEvents || 0;
    totalSchedules.value = data.totalSchedules || 0;
    totalNotes.value = data.totalNotes || 0;
    markedDays.value = data.markedDays || 0;

    generateCalendar(data.events || {});
  } catch (error) {
    console.error('加载失败', error);
    generateCalendar({});
  }
};

const generateCalendar = (eventsMap) => {
  const firstDay = dayjs(`${currentYear.value}-${currentMonth.value}-01`);
  const startDay = firstDay.startOf('month').startOf('week');
  const days = [];

  for (let i = 0; i < 42; i++) {
    const currentDate = startDay.add(i, 'day');
    const isCurrentMonth = currentDate.month() + 1 === currentMonth.value;
    const dateKey = currentDate.format('YYYY-MM-DD');
    const dayEvents = eventsMap[dateKey] || [];

    days.push({
      date: dateKey,
      dayNum: currentDate.date(),
      isToday: currentDate.isSame(dayjs(), 'day'),
      isOtherMonth: !isCurrentMonth,
      events: dayEvents,
      eventCount: dayEvents.length
    });
  }

  calendarDays.value = days;
};

const openDayDetail = (day) => {
  if (day.isOtherMonth) return;
  selectedDate.value = day.date;
  selectedDateTitle.value = dayjs(day.date).format('YYYY年MM月DD日 (dddd)');
  drawerVisible.value = true;
};

const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12;
    currentYear.value--;
  } else {
    currentMonth.value--;
  }
  loadMonthData();
};

const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1;
    currentYear.value++;
  } else {
    currentMonth.value++;
  }
  loadMonthData();
};

const today = () => {
  currentYear.value = dayjs().year();
  currentMonth.value = dayjs().month() + 1;
  loadMonthData();
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
  min-height: 100vh;
  background: linear-gradient(135deg, #D4EEFF 0%, #B8E1FF 100%);
  padding: 24px 32px;
}

/* 顶部栏 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(255, 255, 255, 0.6);
}

.logo-section h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 500;
  color: #2C6B8F;
}

.logo-section p {
  margin: 6px 0 0;
  color: #5BA3C7;
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
  color: #4A90D9;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.month-btn:hover {
  background: #4A90D9;
  color: white;
}

.month-text {
  font-size: 20px;
  font-weight: 500;
  color: #2C6B8F;
  min-width: 130px;
  text-align: center;
}

.today-btn {
  padding: 6px 20px;
  border-radius: 25px;
  border: none;
  background: white;
  color: #4A90D9;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.today-btn:hover {
  background: #4A90D9;
  color: white;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 16px;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: #2C6B8F;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #8BB3CA;
  margin-top: 6px;
}

/* 图例 - 放在上面 */
.legend {
  display: flex;
  justify-content: flex-start;
  gap: 24px;
  margin-bottom: 20px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 30px;
  width: fit-content;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #2C6B8F;
}

.legend-badge {
  width: 20px;
  height: 10px;
  border-radius: 5px;
}

.schedule-badge {
  background: #FFF0D4;
  border-left: 3px solid #E8A735;
}

.note-badge {
  background: #E0F5E0;
  border-left: 3px solid #52C41A;
}

.today-badge {
  background: #E8F4FF;
  border: 1px solid #4A90D9;
}

/* 星期 */
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
  color: #4A90D9;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 10px;
}

/* 日历网格容器 - 限制宽度 */
.calendar-grid-wrapper {
  background: rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  padding: 16px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.calendar-day {
  background: white;
  border-radius: 12px;
  min-height: 110px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.calendar-day:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: #FFFFFF;
}

.other-month {
  background: rgba(255, 255, 255, 0.5);
  opacity: 0.6;
}

.today {
  border: 2px solid #4A90D9;
  background: #F0F8FF;
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.day-number {
  font-size: 16px;
  font-weight: 600;
  color: #2C6B8F;
}

.today .day-number {
  color: #4A90D9;
  font-weight: 700;
}

.today-badge-mark {
  font-size: 9px;
  background: #4A90D9;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
}

/* 事件列表 */
.day-events {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  padding: 4px 6px;
  border-radius: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-item.schedule {
  background: #FFF0D4;
  border-left: 2px solid #E8A735;
}

.event-item.note {
  background: #E0F5E0;
  border-left: 2px solid #52C41A;
}

.event-type {
  font-size: 9px;
}

.event-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
  font-size: 10px;
}

.event-item.schedule .event-title {
  color: #C47A0A;
}

.event-item.note .event-title {
  color: #2E8B0A;
}

.more-events {
  font-size: 9px;
  color: #8BB3CA;
  padding: 2px 6px;
}

/* 响应式 */
@media (max-width: 900px) {
  .calendar-page {
    padding: 16px;
  }

  .calendar-grid-wrapper {
    padding: 10px;
  }

  .calendar-grid {
    gap: 6px;
  }

  .calendar-day {
    min-height: 90px;
    padding: 8px;
  }

  .day-number {
    font-size: 14px;
  }

  .event-title {
    font-size: 9px;
  }
}

@media (max-width: 700px) {
  .stats-row {
    gap: 10px;
  }

  .stat-card {
    padding: 12px;
  }

  .stat-number {
    font-size: 22px;
  }

  .calendar-day {
    min-height: 70px;
  }

  .event-item {
    display: none;
  }

  .more-events {
    display: none;
  }

  .legend {
    margin-bottom: 12px;
  }
}
</style>