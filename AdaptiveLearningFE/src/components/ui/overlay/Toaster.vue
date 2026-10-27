<script setup lang="ts">
import {
  ToastAction,
  ToastClose,
  ToastDescription,
  ToastProvider,
  ToastRoot,
  ToastTitle,
  ToastViewport,
} from 'reka-ui'
import { CheckCircle, Info, X, XCircle } from 'lucide-vue-next'
import { useOverlayToasts } from './toast'
import OverlayTooltip from './Tooltip.vue'

const { items, dismissToast } = useOverlayToasts()
const icons = { success: CheckCircle, error: XCircle, info: Info } as const

function scheduleDismiss(id: number) {
  window.setTimeout(() => dismissToast(id), 150)
}
</script>

<template>
  <ToastProvider :duration="5000" label="Thông báo">
    <ToastViewport class="overlay-toast-viewport" />
    <ToastRoot
      v-for="item in items"
      :key="item.id"
      :duration="item.duration"
      :default-open="true"
      :role="item.type === 'error' ? 'alert' : 'status'"
      class="overlay-toast"
      :class="`is-${item.type}`"
      @update:open="(open) => !open && scheduleDismiss(item.id)"
    >
      <span class="overlay-toast-icon" :class="`is-${item.type}`">
        <component :is="icons[item.type]" :size="18" aria-hidden="true" />
      </span>
      <div class="overlay-toast-copy">
        <ToastTitle class="overlay-toast-title">{{ item.title }}</ToastTitle>
        <ToastDescription v-if="item.description" class="overlay-toast-description">
          {{ item.description }}
        </ToastDescription>
      </div>
      <ToastAction v-if="item.action" class="overlay-toast-action" :alt-text="item.action.altText || item.action.label" @click="item.action.onClick">
        {{ item.action.label }}
      </ToastAction>
      <ToastClose as-child>
        <OverlayTooltip label="Đóng thông báo">
          <template #trigger>
            <button type="button" class="overlay-toast-close" aria-label="Đóng thông báo">
              <X :size="16" aria-hidden="true" />
            </button>
          </template>
        </OverlayTooltip>
      </ToastClose>
    </ToastRoot>
  </ToastProvider>
</template>

<style scoped>
.overlay-toast-viewport { position: fixed; z-index: 120; right: 20px; bottom: 20px; display: flex; width: min(420px,calc(100vw - 32px)); max-height: calc(100vh - 40px); flex-direction: column; gap: 10px; margin: 0; padding: 0; outline: none; list-style: none; }
.overlay-toast { display: flex; min-width: 0; align-items: flex-start; gap: 12px; padding: 14px 16px; border: 1px solid var(--ds-border); border-radius: var(--radius-xl); color: var(--text); background: var(--surface); box-shadow: var(--shadow-hover); }
.overlay-toast-icon { display: grid; width: 32px; height: 32px; flex: 0 0 auto; place-items: center; border-radius: 10px; }
.overlay-toast-icon.is-success { color: var(--primary-ink); background: var(--primary-soft); }
.overlay-toast-icon.is-error { color: var(--rose-ink); background: var(--rose-soft); }
.overlay-toast-icon.is-info { color: var(--blue-ink); background: var(--blue-soft); }
.overlay-toast-copy { display: grid; min-width: 0; flex: 1; gap: 2px; padding-top: 1px; }
.overlay-toast-title { color: var(--text-strong); font-size: 14px; line-height: 20px; font-weight: 600; }
.overlay-toast-description { color: var(--text-muted); font-size: 13px; line-height: 1.45; }
.overlay-toast-action { align-self: center; padding: 4px; border: 0; color: var(--ds-primary); background: transparent; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
.overlay-toast-close { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; margin: -6px -8px 0 0; border: 0; border-radius: var(--radius-sm); color: var(--text-muted); background: transparent; cursor: pointer; }
.overlay-toast-close:hover { background: var(--surface-hover); }
@media (max-width: 640px) { .overlay-toast-viewport { top: 12px; right: 12px; bottom: auto; left: 12px; width: auto; max-height: calc(100vh - 24px); } }
</style>
