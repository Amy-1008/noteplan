import { createRouter, createWebHistory } from 'vue-router'

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
    component: () => import('@/views/PlaceholderView.vue'),
    props: { title: '日程管理', desc: '日程增删改查、完成状态、关联笔记（待实现）' },
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
