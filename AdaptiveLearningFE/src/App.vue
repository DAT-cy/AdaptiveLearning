<script setup>
import { useRoute } from "vue-router"
import { KeepAlive, computed } from "vue"
import { ConfirmProvider, Toaster } from '@/components/ui/overlay'
import { TooltipProvider } from 'reka-ui'
import { settings } from "./config/settings";
import Loading from '@/components/Loading.vue'

const { keepAliveMaxNum } = settings
const currentRoute = useRoute()
const layout = computed(
  () => (currentRoute.meta.layout || "only-view") + "-layout"
)
</script>

<template>
  <ConfirmProvider>
    <TooltipProvider :delay-duration="400" :skip-delay-duration="300">
      <component :is="layout">
        <router-view v-slot="{ Component }">
          <KeepAlive :max="keepAliveMaxNum">
            <component :is="Component" />
          </KeepAlive>
        </router-view>
      </component>
      <Toaster />
      <Loading />
    </TooltipProvider>
  </ConfirmProvider>
</template>
