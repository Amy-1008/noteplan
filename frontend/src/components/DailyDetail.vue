<template>
  <div class="daily-detail">
    <!-- 日程列表 -->
    <div v-if="schedules.length" class="section">
      <div class="section-header">
        <span class="section-icon">📅</span>
        <span class="section-title">日程</span>
        <span class="section-count">{{ schedules.length }}</span>
      </div>
      <div class="schedule-list">
        <div v-for="item in schedules" :key="item.id" class="schedule-item">
          <div class="schedule-time">{{ item.time }}</div>
          <div class="schedule-info">
            <div class="schedule-name">{{ item.title }}</div>
            <div class="schedule-remark" v-if="item.remark">{{ item.remark }}</div>
            <div class="schedule-tags" v-if="item.tags && item.tags.length">
              <span v-for="tag in item.tags" :key="tag" class="tag">#{{ tag }}</span>
            </div>
          </div>
          <div class="schedule-status" :class="item.status">
            {{ item.status === 'completed' ? '已完成' : '待完成' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div v-if="notes.length" class="section">
      <div class="section-header">
        <span class="section-icon">📝</span>
        <span class="section-title">笔记</span>
        <span class="section-count">{{ notes.length }}</span>
      </div>
      <div class="note-list">
        <div v-for="item in notes" :key="item.id" class="note-item">
          <div class="note-title">{{ item.title }}</div>
          <div class="note-time">{{ item.createTime }}</div>
          <div class="note-content">{{ item.content }}</div>
          <div class="note-tags" v-if="item.tags && item.tags.length">
            <span v-for="tag in item.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!schedules.length && !notes.length" class="empty">
      <div class="empty-icon">📭</div>
      <div class="empty-text">这一天没有日程或笔记</div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { getDayDetail } from '@/api/calendar';

const props = defineProps({
  date: {
    type: String,
    required: true
  }
});

const schedules = ref([]);
const notes = ref([]);

const loadDetail = async () => {
  try {
    const res = await getDayDetail(props.date);
    schedules.value = res.data.schedules || [];
    notes.value = res.data.notes || [];
  } catch (error) {
    console.error('加载失败', error);
    schedules.value = [];
    notes.value = [];
  }
};

watch(() => props.date, () => {
  loadDetail();
}, { immediate: true });
</script>

<style scoped>
.daily-detail {
  padding: 20px;
  background: var(--card-bg);
  transition: background 0.3s;
}

.section {
  margin-bottom: 30px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--card-border);
}

.section-icon {
  font-size: 18px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.section-count {
  margin-left: auto;
  background: var(--bg-hover);
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: var(--text-secondary);
}

.schedule-item {
  display: flex;
  gap: 16px;
  padding: 14px;
  background: var(--schedule-bg);
  border-radius: 12px;
  margin-bottom: 10px;
  border-left: 3px solid var(--schedule-time-color);
  transition: background 0.3s;
}

.schedule-time {
  min-width: 80px;
  font-size: 13px;
  font-weight: 500;
  color: var(--schedule-time-color);
}

.schedule-info {
  flex: 1;
}

.schedule-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.schedule-remark {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.schedule-tags, .note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.schedule-tags .tag, .note-tags .tag {
  font-size: 10px;
  color: var(--schedule-time-color);
  background: var(--card-bg);
  padding: 2px 8px;
  border-radius: 12px;
}

.schedule-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 20px;
  height: fit-content;
}

.schedule-status.pending {
  background: var(--schedule-status-pending-bg);
  color: var(--schedule-status-pending-color);
}

.schedule-status.completed {
  background: var(--schedule-status-completed-bg);
  color: var(--schedule-status-completed-color);
}

.note-item {
  padding: 14px;
  background: var(--note-bg);
  border-radius: 12px;
  margin-bottom: 10px;
  border-left: 3px solid var(--note-border-color);
  transition: background 0.3s;
}

.note-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.note-time {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.note-content {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 8px;
}

.note-tags .tag {
  color: var(--note-border-color);
  background: var(--note-tag-bg);
}

.empty {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>