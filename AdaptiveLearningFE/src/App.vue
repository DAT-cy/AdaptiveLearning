<script setup>
import { useRoute } from "vue-router"
import { KeepAlive, computed } from "vue"
import Toaster from '@/components/ui/toast/Toaster.vue'
import { settings } from "./config/settings";
import Loading from '@/components/Loading.vue'

const { keepAliveMaxNum } = settings
const currentRoute = useRoute()
const layout = computed(
  () => (currentRoute.meta.layout || "default") + "-layout"
)
</script>

<template>
  <component :is="layout">
    <KeepAlive :max="keepAliveMaxNum">
      <router-view />
    </KeepAlive>
  </component>
  <Toaster />
  <Loading />
</template>