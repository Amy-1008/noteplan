<script setup>
import { onMounted, ref } from 'vue'
import { fetchHealth } from '@/api/client'

const status = ref('检查中…')
const detail = ref(null)

onMounted(async () => {
  try {
    const { data } = await fetchHealth()
    if (data?.code === 200) {
      status.value = '后端已连通'
      detail.value = data.data
    } else {
      status.value = '响应异常'
    }
  } catch (e) {
    status.value = e.message || '系统繁忙，请稍后再试'
  }
})
</script>

<template>
  <div class="card">
    <h2>欢迎使用 NotePlan</h2>
    <p class="muted">记事本与日程一体化管理（课程设计骨架工程）。</p>
    <p><strong>联调状态：</strong>{{ status }}</p>
    <pre v-if="detail" class="muted">{{ JSON.stringify(detail, null, 2) }}</pre>
  </div>
</template>
