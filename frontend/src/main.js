import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// Element Plus 全量引入：新手团队最稳的接入方式，后续需要优化体积再切按需引入
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
