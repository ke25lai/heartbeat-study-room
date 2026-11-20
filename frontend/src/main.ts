import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'
import './styles.css'
import { useUserStore } from './store/user'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

// 挂载前先用 Cookie 恢复登录态，避免刷新后被路由守卫误判为未登录
const userStore = useUserStore(pinia)
userStore.fetchMe().finally(() => {
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
})
