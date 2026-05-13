import { createRouter, createWebHistory } from 'vue-router'
import ScheduleView from '@/views/ScheduleView.vue'  // 导入你写的日程页面

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
  },
  {
    path: '/notes',
    name: 'notes',
    component: () => import('@/views/PlaceholderView.vue'),
    props: { title: '笔记管理', desc: '备忘录 CRUD、版本历史（待实现）' },
  },
  {
    path: '/schedules',
    name: 'schedules',
    component: ScheduleView,
  },
  {
    path: '/calendar',
    name: 'calendar',
    component: () => import('@/views/PlaceholderView.vue'),
    props: { title: '日历视图', desc: '月视图与按日列表（待实现）' },
  },
  {
    path: '/tags',
    name: 'tags',
    component: () => import('@/views/PlaceholderView.vue'),
    props: { title: '标签管理', desc: '标签库与筛选（待实现）' },
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