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

      <el-form-item label="标签">
        <el-select v-model="formData.tagId" multiple placeholder="选择标签" style="width: 100%">
          <el-option
              v-for="tag in tagList"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="关联笔记">
        <el-select
            v-model="formData.noteIds"
            multiple
            placeholder="选择关联笔记"
            style="width: 100%"
        >
          <el-option
              v-for="note in noteList"
              :key="note.id"
              :label="note.title"
              :value="note.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)

const scheduleId = ref(route.query.id)
const tagList = ref([])
const noteList = ref([])

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

      // 判断时间类型
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
      noteList.value = response.data.data
    }
  } catch (error) {
    console.error('获取笔记失败', error)
  }
}

// 保存日程
const saveSchedule = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请填写必填项')
      return
    }

    // 二次确认
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
</style>