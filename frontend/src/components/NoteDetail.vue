<template>
  <div class="detail" v-if="note">
    <h2>{{ note.title }}</h2>
    <div class="time">{{ format(note.createTime) }}</div>

    <textarea v-model="note.content" class="editor"></textarea>
  </div>

  <div v-else class="detail empty">
    请选择一条笔记
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useNoteStore } from '@/store/note'

const store = useNoteStore()

// ✅ 只用 activeNote（关键）
const note = computed(() => store.activeNote)

const format = (t) =>
  t ? new Date(t).toLocaleString('zh-CN') : ''
</script>

<style scoped>
.detail {
  width: 320px;
  border-left: 1px solid #eee;
  padding: 16px;
  overflow-y: auto;
}

.editor {
  width: 100%;
  height: 300px;
  margin-top: 10px;
}
</style>