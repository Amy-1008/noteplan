<template>
  <div class="note-detail-full" v-if="note">
    <div class="detail-header">
      <button class="back-btn" @click="$emit('close')">
        ← 返回
      </button>
      <div class="detail-meta">
        <span class="detail-date">{{ formatDate(note.createTime) }}</span>
        <span v-if="note.tags && note.tags.length" class="detail-tag">
          {{ note.tags[0] }}
        </span>
      </div>
    </div>

    <div class="detail-body">
      <h2 class="detail-title">{{ note.title || '无标题' }}</h2>
      <div class="detail-content">{{ note.content || '暂无内容' }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  note: {
    type: Object,
    required: true
  }
})

defineEmits(['close'])

const formatDate = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}
</script>

<style scoped>
.note-detail-full {
  background: white;
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  min-height: 200px;
  animation: fadeIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.back-btn {
  background: none;
  border: none;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  transition: 0.2s;
}

.back-btn:hover {
  background: #f3f4f6;
  color: #1a1a1a;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #9ca3af;
}

.detail-tag {
  background: #eef2ff;
  color: #4f8cff;
  padding: 2px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.detail-title {
  margin: 0 0 16px 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.detail-content {
  font-size: 15px;
  line-height: 1.8;
  color: #374151;
  white-space: pre-wrap;
}
</style>