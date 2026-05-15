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
            <div class="schedule-location" v-if="item.location">{{ item.location }}</div>
            <div class="schedule-remark" v-if="item.remark">{{ item.remark }}</div>
          </div>
          <div class="schedule-status" :class="item.status">
            {{ item.status === 'completed' ? '已完成' : '待完成' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 备忘录列表 -->
    <div v-if="notes.length" class="section">
      <div class="section-header">
        <span class="section-icon">📝</span>
        <span class="section-title">备忘录</span>
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
      <div class="empty-text">这一天没有日程或备忘录</div>
      <div class="empty-hint">点击下方按钮添加</div>
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
  console.log('加载日期:', props.date);
  try {
    const res = await getDayDetail(props.date);
    console.log('返回数据:', res.data);
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
  border-bottom: 2px solid #E8F4F8;
}

.section-icon {
  font-size: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #2C6B8F;
}

.section-count {
  margin-left: auto;
  background: #E8F4F8;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: #4A90D9;
}

/* 日程卡片 */
.schedule-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #FFF5E8;
  border-radius: 12px;
  margin-bottom: 12px;
  border-left: 4px solid #E8A735;
}

.schedule-time {
  min-width: 100px;
  font-size: 13px;
  font-weight: 500;
  color: #E8A735;
}

.schedule-info {
  flex: 1;
}

.schedule-name {
  font-size: 15px;
  font-weight: 500;
  color: #2C6B8F;
  margin-bottom: 6px;
}

.schedule-location {
  font-size: 12px;
  color: #8BB3CA;
  margin-bottom: 4px;
}

.schedule-remark {
  font-size: 12px;
  color: #A0C4D8;
}

.schedule-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 20px;
  height: fit-content;
}

.schedule-status.pending {
  background: #FFE7BA;
  color: #E8A735;
}

.schedule-status.completed {
  background: #D9F0D9;
  color: #52C41A;
}

/* 备忘录卡片 */
.note-item {
  padding: 16px;
  background: #E8FCE8;
  border-radius: 12px;
  margin-bottom: 12px;
  border-left: 4px solid #52C41A;
}

.note-title {
  font-size: 15px;
  font-weight: 500;
  color: #2C6B8F;
  margin-bottom: 6px;
}

.note-time {
  font-size: 11px;
  color: #8BB3CA;
  margin-bottom: 10px;
}

.note-content {
  font-size: 13px;
  color: #5A7E9A;
  line-height: 1.5;
  white-space: pre-wrap;
  margin-bottom: 10px;
}

.note-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  font-size: 11px;
  color: #52C41A;
  background: white;
  padding: 2px 12px;
  border-radius: 16px;
}

/* 空状态 */
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
  color: #8BB3CA;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 12px;
  color: #B8D4E3;
}
</style>