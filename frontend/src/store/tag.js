import { defineStore } from 'pinia'
import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080'
})

export const useNoteStore = defineStore('note', {
    state: () => ({
        notes: [],
        tags: [],
        activeTag: '全部',
        activeNote: null   // ✅ 新增
    }),

    getters: {
        // ✅ 核心：标签过滤
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

        setActiveTag(tag) {
            this.activeTag = tag
        },

        setActiveNote(note) {   // ✅ 新增
            this.activeNote = note
        },

        async createNote() {
            try {
                const res = await api.post('/api/note/add', {})
                this.notes.unshift(res.data.data)
            } catch (e) {
                console.error('createNote error:', e)
            }
        }
    }
})