import { defineStore } from 'pinia'

export const useTagStore = defineStore('tag', {
  state: () => ({
    tags: [
      { id: 1, name: 'Work' },
      { id: 2, name: 'Life' }
    ]
  })
})