import client from './client'

export const getTagList = () => {
  return client.get('/tags')
}

export const getTagByTarget = (targetId, targetType) => {
  return client.get('/tags/target', { params: { targetId, targetType } })
}

export const getTagFilteredTargets = (params) => {
  return client.get('/tags/filter', { params })
}

export const createTag = (payload) => {
  return client.post('/tags', payload)
}

export const updateTag = (id, payload) => {
  return client.put(`/tags/${id}`, payload)
}

export const deleteTag = (id) => {
  return client.delete(`/tags/${id}`)
}

export const bindTag = (targetId, targetType, tagId) => {
  return client.post('/tags/bind', null, { params: { targetId, targetType, tagId } })
}

export const clearTag = (targetId, targetType) => {
  return client.delete('/tags/clear', { params: { targetId, targetType } })
}
