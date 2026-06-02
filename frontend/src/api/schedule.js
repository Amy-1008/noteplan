import client from './client'

// 获取日程列表
export const getScheduleList = (params) => {
    return client.get('/schedule/list', { params })
}

// 更新日程完成状态
export const updateScheduleComplete = (id, completed) => {
    return client.put('/schedule/complete', null, {
        params: { id, completed: completed ? 1 : 0 }
    })
}

export const searchSchedules = (keyword) => {
    return client.get('/schedule/search', {
        params: { keyword }
    })
}

export const addSchedule = (payload) => {
    return client.post('/schedule/add', payload)
}

export const batchDeleteSchedules = (ids) => {
    return client.delete('/schedule/batch-delete', { data: ids })
}

export const getScheduleDetail = (id) => {
    return client.get('/schedule/detail', { params: { id } })
}

export const updateSchedule = (payload) => {
    return client.put('/schedule/update', payload)
}