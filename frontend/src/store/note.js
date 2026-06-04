import { defineStore } from 'pinia'
import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080'
})

export const useNoteStore = defineStore('note', {
    state: () => ({
        notes: [],
        tags: [],
        scheduleList: [], // ✅ 新增：存储日程列表
        activeTag: '全部',
        activeNote: null
    }),

    getters: {
        filteredNotes(state) {
            if (state.activeTag === '全部') return state.notes
            return state.notes.filter(note => {
                if (Array.isArray(note.tags)) {
                    return note.tags.includes(state.activeTag)
                }
                if (note.tag) {
                    return note.tag === state.activeTag
                }
                if (note.category) {
                    return note.category === state.activeTag
                }
                return false
            })
        },
        // 按标签统计笔记数量
        tagCountInNotes(state) {
            const counts = {}
            state.notes.forEach(n => {
                let tagName = '未分类'
                if (n.tags && Array.isArray(n.tags) && n.tags.length > 0) {
                    tagName = n.tags[0]
                } else if (n.tag) {
                    tagName = n.tag
                } else if (n.category) {
                    tagName = n.category
                } else if (n.tagId && state.tags) {
                    const tag = state.tags.find(t => t.id === n.tagId)
                    if (tag) tagName = tag.name
                }
                if (!counts[tagName]) counts[tagName] = 0
                counts[tagName]++
            })
            return counts
        }
    },

    actions: {
        async fetchNotes() {
            try {
                const res = await api.get('/api/note/list')
                this.notes = res.data.data || []
            } catch (e) {
                console.error('fetchNotes error:', e)
                this.notes = []
            }
        },

        async fetchTags() {
            try {
                const res = await api.get('/api/tags')
                this.tags = res.data.data || []
            } catch (e) {
                console.error('fetchTags error:', e)
                this.tags = []
            }
        },

        async fetchScheduleList() {
            try {
                const res = await api.get('/api/schedule/list')
                this.scheduleList = res.data.data || []
            } catch (e) {
                console.error('fetchScheduleList error:', e)
                this.scheduleList = []
            }
        },

        async createNote() {
            try {
                const res = await api.post('/api/note/add', {})
                this.notes.unshift(res.data.data)
            } catch (e) {
                console.error('createNote error:', e)
            }
        },

        setActiveTag(tag) {
            this.activeTag = tag
        },

        setActiveNote(note) {
            this.activeNote = note
        }
    }
})