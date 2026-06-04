import { createRouter, createWebHistory } from 'vue-router'
import ScheduleView from '@/views/ScheduleView.vue'
import TagView from "@/views/TagView.vue"
import NoteEdit from '@/views/NoteEdit.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/notes/edit/:id?', // 可选参数，无id时表示新建
    name: 'note-edit',
    component: NoteEdit,
  },
  {
    path: '/schedules',
    name: 'schedules',
    component: ScheduleView,
  },
  {
    path: '/schedule/detail',
    name: 'ScheduleDetail',
    component: () => import('@/views/ScheduleDetail.vue')
  },
  {
    path: '/calendar',
    name: 'calendar',
    component: () => import('@/views/CalendarView.vue'),
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
  },
]

export default createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})