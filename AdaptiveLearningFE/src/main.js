import '@/assets/index.css'
import '@/components/ui/overlay/overlays.css'
import '@/config/permission' // permission control

import App from './App.vue'
import clickOutsideDirective from "@/hooks/clickOutsideDirective";
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { debounceDirective } from './hooks/debounce';
import infiniteScrollDirective from "@/hooks/infiniteScroll";
import { registerGlobalComponent } from "@/utils/import";
import router from './router'

const app = createApp(App)
registerGlobalComponent(app);
app.use(createPinia())
app.use(router)

app.directive('click-outside', clickOutsideDirective)
app.directive('infinite-scroll', infiniteScrollDirective)
app.directive('debounce', debounceDirective)
app.config.productionTip = false

app.mount('#app')
