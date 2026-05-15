import { createRouter, createWebHistory } from 'vue-router'
import ScheduleView from '@/views/ScheduleView.vue'
import TagView from "@/views/TagView.vue"
import NoteView from '@/views/NoteView.vue'
import NoteEdit from '@/views/NoteEdit.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/notes',
    name: 'notes',
    component: NoteView,
  },
  {
    path: '/notes/create',
    name: 'note-create',
    component: NoteEdit,
  },
  {
    path: '/notes/edit/:id',
    name: 'note-edit',
    component: NoteEdit,
  },
  {
    path: '/schedules',
    name: 'schedules',
    component: ScheduleView,
  },
  {
    path: '/calendar',
    name: 'calendar',
    component: () => import('@/views/CalendarView.vue'),
    props: { title: '日历视图', desc: '月视图与按日列表（待实现）' },
  },
  {
    path: '/tags',
    name: 'tags',
    component: TagView,
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('@/views/PlaceholderView.vue'),
    props: { title: '搜索', desc: '关键词模糊搜索与高亮（待实现）' },
  },
]

export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})