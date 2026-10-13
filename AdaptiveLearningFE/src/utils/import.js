import { defineAsyncComponent } from "vue";

export function registerGlobalComponent(app) {
  app.component('only-view-layout', defineAsyncComponent(() => import("../layouts/OnlyView.vue")))
  app.component('default-layout', defineAsyncComponent(() => import("../layouts/Default.vue")))
}