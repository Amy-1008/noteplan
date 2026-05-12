<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, required: true },
})
</script>

<template>
  <div class="card">
    <h2>{{ title }}</h2>
    <p class="muted">{{ desc }}</p>
  </div>
</template>
