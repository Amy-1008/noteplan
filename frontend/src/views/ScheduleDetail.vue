<template>
  <div class="detail-container">
    <div class="detail-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>日程详情</h2>
      <el-button type="primary" @click="saveSchedule">保存</el-button>
    </div>

    <el-form :model="formData" :rules="formRules" ref="formRef" label-width="80px" class="detail-form">
      <el-form-item label="标题" prop="title">
        <el-input
            v-model="formData.title"
            placeholder="请输入日程标题"
            maxlength="20"
            show-word-limit
        />
      </el-form-item>

      <!-- 时间类型选择 -->
      <el-form-item label="时间类型" prop="timeType">
        <el-radio-group v-model="formData.timeType">
          <el-radio value="point">时间点</el-radio>
          <el-radio value="period">时间段</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 时间点模式 -->
      <el-form-item v-if="formData.timeType === 'point'" label="时间" prop="endTime">
        <el-date-picker
            v-model="formData.endTime"
            type="datetime"
            placeholder="选择日期时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
        />
      </el-form-item>

      <!-- 时间段模式 -->
      <template v-else>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
              v-model="formData.startTime"
              type="datetime"
              placeholder="选择开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
              v-model="formData.endTime"
              type="datetime"
              placeholder="选择结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>
      </template>

      <el-form-item label="重复频率">
        <el-select v-model="formData.repeatRule" placeholder="不重复" style="width: 100%">
          <el-option label="无" value="none" />
          <el-option label="每天" value="daily" />
          <el-option label="每周" value="weekly" />
          <el-option label="每月" value="monthly" />
          <el-option label="每年" value="yearly" />
          <el-option label="工作日" value="workday" />
          <el-option label="节假日" value="holiday" />
        </el-select>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="5"
            placeholder="请输入备注"
            maxlength="800"
            show-word-limit
        />
      </el-form-item>


      <el-form-item label="标签" prop="tagId">
        <TagSelector
            v-model="formData.tagId"
            @tag-created="handleTagCreated"
        />
      </el-form-item>


      <el-form-item label="关联笔记">
        <div class="notes-display">
          <!-- 显示已选中的笔记（胶囊形式） -->
          <div class="notes-list">
            <el-tag
                v-for="note in selectedNotes"
                :key="note.id"
                closable
                @close="removeNote(note.id)"
                type="success"
                effect="plain"
                style="margin-right: 8px; margin-bottom: 4px;"
            >
              {{ note.title }}
            </el-tag>
            <span v-if="selectedNotes.length === 0" class="placeholder-text">未关联笔记</span>
          </div>

          <!-- 选择笔记按钮/下拉框 -->
          <el-select
              v-model="pendingNoteId"
              placeholder="选择关联笔记"
              clearable
              size="small"
              style="width: 150px; margin-top: 8px;"
              @change="addNote"
          >
            <el-option
                v-for="note in availableNotes"
                :key="note.id"
                :label="note.title"
                :value="note.id"
            />
          </el-select>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import axios from 'axios'
import TagSelector from "@/components/TagSelector.vue";

const route = useRoute()
const router = useRouter()
const formRef = ref(null)

const scheduleId = ref(route.query.id)
const tagList = ref([])

// 所有笔记列表（从后端获取）
const allNotes = ref([])

// 格式化日期时间
const formatDateTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

// 表单数据
const formData = ref({
  id: null,
  title: '',
  timeType: 'point',
  startTime: '',
  endTime: '',
  repeatRule: 'none',
  remark: '',
  tagId: null,
  noteIds: []
})

// 表单校验规则
const formRules = {
  title: [
    { required: true, message: '请输入日程标题', trigger: 'blur' },
    { max: 20, message: '标题不能超过20个字符', trigger: 'blur' }
  ],
  endTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
  remark: [
    { max: 800, message: '备注不能超过800个字符', trigger: 'blur' }
  ]
}

// ---------- 标签和笔记显示相关 ----------
const selectedTag = ref(null)        // 当前选中的标签对象
const selectedNotes = ref([])        // 当前选中的笔记对象列表
const pendingNoteId = ref(null)      // 临时选中的笔记ID

// 可选的笔记列表（排除已选中的）
const availableNotes = computed(() => {
  const selectedIds = selectedNotes.value.map(n => n.id)
  return allNotes.value.filter(n => !selectedIds.includes(n.id))
})

// 根据标签ID获取标签对象
const getTagById = (tagId) => {
  return tagList.value.find(t => t.id === tagId)
}

// 根据笔记ID列表获取笔记对象列表
const getNotesByIds = (noteIds) => {
  return allNotes.value.filter(n => noteIds.includes(n.id))
}

// 标签被选中时
const onTagSelected = (tagId) => {
  if (tagId) {
    selectedTag.value = getTagById(tagId)
    formData.value.tagId = tagId
  } else {
    selectedTag.value = null
    formData.value.tagId = null
  }
}

// 移除标签
const removeTag = () => {
  selectedTag.value = null
  formData.value.tagId = null
}

// 添加笔记
const addNote = (noteId) => {
  if (noteId) {
    const note = allNotes.value.find(n => n.id === noteId)
    if (note && !selectedNotes.value.find(n => n.id === noteId)) {
      selectedNotes.value.push(note)
      formData.value.noteIds = selectedNotes.value.map(n => n.id)
    }
    pendingNoteId.value = null
  }
}

// 移除笔记
const removeNote = (noteId) => {
  selectedNotes.value = selectedNotes.value.filter(n => n.id !== noteId)
  formData.value.noteIds = selectedNotes.value.map(n => n.id)
}

// 获取日程详情
const fetchScheduleDetail = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/schedule/detail', {
      params: { id: scheduleId.value }
    })
    if (response.data.code === 200) {
      const data = response.data.data
      formData.value.id = data.id
      formData.value.title = data.title
      formData.value.repeatRule = data.repeatRule || 'none'
      formData.value.remark = data.remark || ''

      // 设置标签
      if (data.tagId) {
        selectedTag.value = getTagById(data.tagId)
        formData.value.tagId = data.tagId
      }

      // 设置关联笔记
      if (data.noteIds && data.noteIds.length > 0) {
        selectedNotes.value = getNotesByIds(data.noteIds)
        formData.value.noteIds = data.noteIds
      }

      // 时间类型处理
      if (!data.startTime) {
        formData.value.timeType = 'point'
        formData.value.endTime = data.endTime
        formData.value.startTime = ''
      } else {
        formData.value.timeType = 'period'
        formData.value.startTime = data.startTime
        formData.value.endTime = data.endTime
      }
    }
  } catch (error) {
    console.error('获取日程详情失败', error)
    ElMessage.error('加载失败')
  }
}

// 获取标签列表
const fetchTagList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/tags')
    if (response.data.code === 200) {
      tagList.value = response.data.data
    }
  } catch (error) {
    console.error('获取标签失败', error)
  }
}

// 获取笔记列表
const fetchNoteList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/note/list')
    if (response.data.code === 200) {
      allNotes.value = response.data.data
    }
  } catch (error) {
    console.error('获取笔记失败', error)
  }
}

// 保存日程
const saveSchedule = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请填写必填项')
      return
    }

    ElMessageBox.confirm('确定要保存修改吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        const submitData = {
          id: formData.value.id,
          title: formData.value.title,
          repeatRule: formData.value.repeatRule,
          remark: formData.value.remark,
          tagId: formData.value.tagId,
          noteIds: formData.value.noteIds
        }

        if (formData.value.timeType === 'point') {
          submitData.startTime = null
          submitData.endTime = formData.value.endTime
        } else {
          submitData.startTime = formData.value.startTime
          submitData.endTime = formData.value.endTime
        }

        const response = await axios.put('http://localhost:8080/api/schedule/update', submitData)

        if (response.data.code === 200) {
          ElMessage.success('保存成功')
          router.back()
        } else {
          ElMessage.error(response.data.message || '保存失败')
        }
      } catch (error) {
        console.error('保存失败', error)
        ElMessage.error('保存失败')
      }
    }).catch(() => {})
  })
}

// 返回
const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchScheduleDetail()
  fetchTagList()
  fetchNoteList()
})
</script>

<style scoped>
.detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 30px;
}

.detail-header h2 {
  margin: 0;
}

.detail-form {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.tag-display {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.notes-display {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notes-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.placeholder-text {
  color: #c0c4cc;
  font-size: 13px;
}

</style>