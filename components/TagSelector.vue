<template>
  <div class="tag-selector">
    <el-select
        v-model="selectedTagId"
        placeholder="请选择标签"
        clearable
        :disabled="disabled"
        @clear="handleClear"
        style="width: 100%"
    >
      <!-- 新建标签选项（置顶） -->
      <el-option
          value="__CREATE_NEW__"
          class="create-new-option"
      >
        <div class="create-new-item" @click.stop.prevent="openCreateDialog">
          <el-icon><Plus /></el-icon>
          <span>新建标签</span>
        </div>
      </el-option>

      <!-- 分割线 -->
      <el-option
          value="__DIVIDER__"
          disabled
          class="divider-option"
      >
        <el-divider />
      </el-option>

      <!-- 标签列表 -->
      <el-option
          v-for="tag in tagList"
          :key="tag.id"
          :label="tag.name"
          :value="tag.id"
      />

      <el-option
          v-if="tagList.length === 0 && !loading"
          value="__EMPTY__"
          disabled
          class="empty-option"
      >
        暂无标签，请点击"新建标签"创建
      </el-option>
    </el-select>

    <el-dialog
        v-model="createDialogVisible"
        title="新建标签"
        width="400px"
        :close-on-click-modal="false"
        :append-to-body="true"
        :modal="true"
        class="create-tag-dialog-center"
        @close="resetCreateForm"
    >
      <el-form :model="createForm" @submit.prevent="confirmCreate">
        <el-form-item label="标签名">
          <el-input
              v-model="createForm.name"
              placeholder="请输入标签名（最多20字）"
              maxlength="20"
              show-word-limit
              autofocus
              @keyup.enter="confirmCreate"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const props = defineProps({
  modelValue: {
    type: Number,
    default: null
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'tag-created'])
const tagList = ref([])
const loading = ref(false)
const createDialogVisible = ref(false)
const createForm = ref({
  name: ''
})

const selectedTagId = computed({
  get: () => props.modelValue,
  set: (val) => {
    if (val === '__CREATE_NEW__' || val === '__DIVIDER__' || val === '__EMPTY__') {
      return
    }
    emit('update:modelValue', val)
    emit('change', val)
  }
})

// 获取标签列表
const fetchTagList = async () => {
  loading.value = true
  try {
    const response = await axios.get('http://localhost:8080/api/tags')
    if (response.data.code === 200) {
      tagList.value = response.data.data || []
    }
  } catch (error) {
    console.error('获取标签失败', error)
    ElMessage.error('获取标签失败')
  } finally {
    loading.value = false
  }
}

// 打开新建标签弹窗
const openCreateDialog = () => {
  createDialogVisible.value = true
}

// 重置新建表单
const resetCreateForm = () => {
  createForm.value.name = ''
}

// 确认新建标签
const confirmCreate = async () => {
  const trimmedName = createForm.value.name.trim()
  if (!trimmedName) {
    ElMessage.warning('标签名不能为空')
    return
  }
  if (trimmedName.length > 20) {
    ElMessage.warning('标签名不能超过20个字符')
    return
  }

  // 检查重名
  const existingTag = tagList.value.find(t => t.name === trimmedName)
  if (existingTag) {
    ElMessage.warning(`标签名 "${trimmedName}" 已存在`)
    return
  }

  try {
    const response = await axios.post('http://localhost:8080/api/tags', {
      name: trimmedName,
      rank: 0
    })
    if (response.data.code === 200) {
      const newTag = response.data.data
      tagList.value.push(newTag)
      // 自动选中新建的标签
      selectedTagId.value = newTag.id
      createDialogVisible.value = false
      resetCreateForm()
      ElMessage.success('创建成功')
      emit('tag-created', newTag)
    } else {
      ElMessage.error(response.data.message || '创建失败')
    }
  } catch (error) {
    console.error('创建标签失败', error)
    ElMessage.error(error.response?.data?.message || '创建失败')
  }
}

// 清除选择
const handleClear = () => {
  emit('update:modelValue', null)
  emit('change', null)
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
.tag-selector {
  width: 100%;
}

/* 新建标签选项样式 */
.create-new-option {
  background-color: #f5f7fa;
}

.create-new-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  font-weight: 500;
  cursor: pointer;
}

.create-new-item:hover {
  color: #66b1ff;
}

.divider-option {
  padding: 0 !important;
  cursor: default;
  height: 1px;
}

.divider-option:hover {
  background-color: transparent;
}

:deep(.el-divider) {
  margin: 0 !important;
}

/* 空状态样式 */
.empty-option {
  color: #c0c4cc;
  text-align: center;
  cursor: default;
}
</style>