import axios from 'axios'

// 后端接口地址（根据你的后端端口修改）
const BASE_URL = 'http://localhost:8080'

// 创建 axios 实例
const request = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
})

// 获取日程列表
export const getScheduleList = async () => {
    const response = await request.get('/api/schedule/list')
    return response.data
}

// 更新日程完成状态
export const updateScheduleComplete = async (id, completed) => {
    const response = await request.put('/api/schedule/complete', null, {
        params: { id, completed: completed ? 1 : 0 }
    })
    return response.data
}