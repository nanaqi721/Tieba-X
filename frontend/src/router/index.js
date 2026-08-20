import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

// 路由表：首页帖子流 + 吧主页 + 登录/注册（独立布局）
const routes = [
  {
    path: '/',
    component: () => import('../layouts/PostLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('../views/HomeView.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'user',
        name: 'userHome',
        component: () => import('../views/UserHomeView.vue'),
        meta: { title: '我的主页', requiresAuth: true },
      },
      {
        path: 'search',
        name: 'search',
        component: () => import('../views/SearchView.vue'),
        meta: { title: '搜索' },
      },
      {
        path: 'bar/create',
        name: 'barCreate',
        component: () => import('../views/BarCreateView.vue'),
        meta: { title: '创建吧', requiresAuth: true },
      },
      {
        path: 'bar/:barId',
        name: 'barHome',
        component: () => import('../views/BarHomeView.vue'),
        meta: { title: '吧主页' },
      },
      {
        path: 'post/:postId',
        name: 'postDetail',
        component: () => import('../views/PostDetailView.vue'),
        meta: { title: '帖子详情' },
      },
    ],
  },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { title: '注册' } },
  // 未匹配路由，回首页
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：
// - 标记 requiresAuth 的页面未登录 → 跳登录页并带来源地址
// - 已登录用户访问登录/注册页 → 直接回首页
// 目前首页为匿名可看，requiresAuth 留给后续写操作页面使用
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (auth.isLoggedIn && (to.name === 'login' || to.name === 'register')) {
    return { path: '/' }
  }
})

// 每次跳转后更新浏览器标题
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 贴吧 X` : '贴吧 X'
})

export default router
