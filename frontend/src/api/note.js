import client from './client'

// 获取所有笔记
export const getNoteList = () => {
    return client.get('/note/list')
}

// 根据日期获取笔记
export const getNotesByDate = (date) => {
    return client.get('/note/by-date', { params: { date } })
}

// 获取单个笔记
export const getNoteById = (id) => {
    return client.get(`/note/${id}`)
}

// 新增笔记
export const addNote = (note) => {
    return client.post('/note/add', note)
}

// 更新笔记
export const updateNote = (note) => {
    return client.put('/note/update', note)
}

// 删除笔记
export const deleteNote = (id) => {
    return client.delete(`/note/delete/${id}`)
}

// 获取笔记版本列表
export const getNoteVersions = (noteId) => {
    return client.get(`/note/versions/${noteId}`)
}

// 恢复指定版本
export const recoverVersion = (noteId, versionNo) => {
    return client.post('/note/recover', null, { params: { noteId, versionNo } })
}