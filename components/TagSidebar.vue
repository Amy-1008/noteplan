<template>
  <div class="tag-sidebar">
    <div class="sidebar-header">
      <span>分类</span>
    </div>
    <div class="tag-list">
      <div
          class="tag-item"
          :class="{ active: currentTag === 'all' }"
          @click="selectTag('all')"
      >
        📋 所有
      </div>
      <!-- 未分类 -->
      <div
          class="tag-item"
          :class="{ active: currentTag === 'uncategorized' }"
          @click="selectTag('uncategorized')"
      >
        📁 未分类
      </div>
      <div
          v-for="tag in sortedTagList"
          :key="tag.id"
          class="tag-item"
          :class="{ active: currentTag === tag.id }"
          @click="selectTag(tag.id)"
      >
        <button
            class="star-btn"
            :class="{ active: tag.rank === 1 }"
            @click.stop="toggleRank(tag)"
            :title="tag.rank === 1 ? '取消置顶' : '置顶'"
        >
          <span v-if="tag.rank === 1" class="star filled">★</span>
          <span v-else class="star empty">☆</span>
        </button>
        <span class="tag-name">{{ tag.name }}</span>
      </div>
      <div v-if="tagList.length === 0" class="tag-empty">暂无标签</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import axios from 'axios'

const props = defineProps({
  // 当前选中的标签ID，'all' 表示所有
  modelValue: {
    type: [String, Number],
    default: 'all'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const tagList = ref([])

// 按 rank 降序排序（置顶的在前）
const sortedTagList = computed(() => {
  return [...tagList.value].sort((a, b) => (b.rank || 0) - (a.rank || 0))
})

// 当前选中的标签
const currentTag = computed({
  get: () => props.modelValue,
  set: (val) => {
    emit('update:modelValue', val)
    emit('change', val)
  }
})

// 获取标签列表
const fetchTagList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/tags')
    if (response.data.code === 200) {
      tagList.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取标签失败', error)
  }
}

// 切换置顶状态
const toggleRank = async (tag) => {
  const newRank = tag.rank === 1 ? 0 : 1
  try {
    const response = await axios.put(`http://localhost:8080/api/tags/${tag.id}`, {
      name: tag.name,
      rank: newRank
    })
    if (response.data.code === 200) {
      tag.rank = newRank
    } else {
      console.error('切换置顶失败', response.data.message)
    }
  } catch (error) {
    console.error('切换置顶失败', error)
  }
}

// 选择标签
const selectTag = (tagId) => {
  currentTag.value = tagId
}

// 刷新标签列表（供父组件调用）
const refresh = () => {
  fetchTagList()
}

defineExpose({
  refresh
})

onMounted(() => {
  fetchTagList()
})
</script>

<style scoped>
.tag-sidebar {
  width: 220px;
  background-color: white;
  border-right: 1px solid #e4e7ed;
  padding: 20px 0;
  overflow-y: auto;
  flex-shrink: 0;
  height: 100%;
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
  display: flex;
  align-items: center;
  gap: 8px;
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

.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.star {
  font-size: 14px;
}

.star.filled {
  color: #fbbf24;
}

.star.empty {
  color: #d1d5db;
}

.tag-name {
  flex: 1;
}

.tag-empty {
  padding: 10px 16px;
  font-size: 13px;
  color: #c0c4cc;
  text-align: center;
}
</style>