<script setup lang="ts">
import type { Component } from 'vue'
import { nextTick, watch } from 'vue'
import {
  DrawerContent,
  DrawerDescription,
  DrawerOverlay,
  DrawerPortal,
  DrawerRoot,
  DrawerTitle,
} from 'reka-ui'
import { CircleAlert, ShieldCheck, X } from 'lucide-vue-next'
import OverlayTooltip from './Tooltip.vue'
import { useConfirm } from './useConfirm'
import type { OverlayVariant } from './types'

/** Shared right-side editor panel props. */
interface Props {
  open: boolean
  title: string
  description?: string
  icon?: Component
  size?: 'md' | 'lg'
  variant?: OverlayVariant
  dirty?: boolean
  submitting?: boolean
  dismissible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  variant: 'default',
  dirty: false,
  submitting: false,
  dismissible: true,
})
const emit = defineEmits<{
  'update:open': [value: boolean]
  openChange: [value: boolean]
}>()
const { confirm } = useConfirm()
const id = `shared-drawer-${Math.random().toString(36).slice(2)}`
let closePending = false
let returnFocusElement: HTMLElement | null = null

function setOpen(value: boolean) {
  emit('update:open', value)
  emit('openChange', value)
}

async function requestClose() {
  if (closePending) return
  closePending = true
  try {
    if (props.submitting || !props.dismissible) return
    if (props.dirty) {
      const discard = await confirm({
        title: 'Bỏ thay đổi chưa lưu?',
        description: 'Các thay đổi bạn vừa nhập sẽ không được lưu.',
        confirmText: 'Bỏ thay đổi',
        cancelText: 'Tiếp tục chỉnh sửa',
      })
      if (!discard) return
    }
    setOpen(false)
  } finally {
    closePending = false
  }
}

async function focusInitialField() {
  await nextTick()
  const drawer = document.querySelector<HTMLElement>('.overlay-drawer[data-state="open"]')
  const firstField = drawer?.querySelector<HTMLElement>(
    'input:not([disabled]), select:not([disabled]), textarea:not([disabled])'
  )
  const fallback = drawer?.querySelector<HTMLElement>('button:not([disabled])')
  ;(firstField || fallback || drawer)?.focus()
}

watch(() => props.open, async (open, previous) => {
  if (open && !previous) {
    returnFocusElement = document.activeElement instanceof HTMLElement
      ? document.activeElement
      : null
    await focusInitialField()
  } else if (!open && previous) {
    await nextTick()
    if (returnFocusElement?.isConnected) returnFocusElement.focus()
    returnFocusElement = null
  }
})

function onOpenChange(value: boolean, details: { cancel: () => void }) {
  if (value) {
    setOpen(true)
    return
  }
  details.cancel()
  void requestClose()
}

defineExpose({ requestClose })
</script>

<template>
  <DrawerRoot :open="open" swipe-direction="right" @update:open="onOpenChange">
    <DrawerPortal>
      <DrawerOverlay force-mount class="overlay-backdrop drawer-backdrop" />
      <DrawerContent force-mount class="overlay-drawer" :class="[`size-${size}`, `is-${variant}`]">
        <header class="overlay-header">
          <span class="overlay-icon-tile" :class="`is-${variant}`">
            <component :is="icon || (variant === 'danger' ? CircleAlert : ShieldCheck)" :size="21" aria-hidden="true" />
          </span>
          <div class="overlay-header-copy">
            <DrawerTitle class="overlay-title">{{ title }}</DrawerTitle>
            <DrawerDescription v-if="description" class="overlay-description">{{ description }}</DrawerDescription>
          </div>
          <OverlayTooltip v-if="dismissible" label="Đóng">
            <template #trigger>
              <button type="button" class="overlay-close" aria-label="Đóng" :disabled="submitting" @click="requestClose">
                <X :size="18" aria-hidden="true" />
              </button>
            </template>
          </OverlayTooltip>
        </header>
        <div class="overlay-drawer-body"><slot :request-close="requestClose" :submitting="submitting" /></div>
        <footer v-if="$slots.footer" class="overlay-footer">
          <slot name="footer" :request-close="requestClose" :submitting="submitting" />
        </footer>
      </DrawerContent>
    </DrawerPortal>
  </DrawerRoot>
</template>

<style scoped>
.drawer-backdrop { z-index: 90; }
.overlay-drawer[data-state='closed'] { animation: drawer-exit 180ms ease both; visibility: hidden; transition: visibility 180ms linear 180ms; pointer-events: none; }
.overlay-drawer { position: fixed; z-index: 91; inset-block: 0; right: 0; display: flex; width: min(var(--drawer-width,480px),100vw); flex-direction: column; overflow: hidden; border-left: 1px solid var(--ds-border); border-radius: var(--radius-3xl) 0 0 var(--radius-3xl); outline: none; background: var(--surface); box-shadow: var(--overlay-shadow); transform: translateX(var(--drawer-swipe-movement-x,0px)); animation: drawer-enter 220ms cubic-bezier(.16,1,.3,1) both; }
.overlay-drawer.size-md { --drawer-width: 480px; }
.overlay-drawer.size-lg { --drawer-width: 640px; }
.overlay-drawer .overlay-header { flex: 0 0 auto; }
.overlay-drawer-body { min-height: 0; flex: 1; overflow: auto; padding: 24px; }
.overlay-drawer .overlay-footer { flex: 0 0 auto; }
@keyframes drawer-enter { from { opacity: 0; transform: translateX(24px); } to { opacity: 1; transform: translateX(var(--drawer-swipe-movement-x,0px)); } }
@keyframes drawer-exit { from { opacity: 1; transform: translateX(var(--drawer-swipe-movement-x,0px)); } to { opacity: 0; transform: translateX(24px); } }
@media (max-width: 640px) {
  .overlay-drawer { width: 100vw; max-width: 100vw; border-radius: 0; border-left: 0; }
  .overlay-drawer .overlay-header { padding: 16px; }
  .overlay-drawer-body { padding: 16px; }
  .overlay-drawer .overlay-footer { flex-direction: column-reverse; padding: 12px 16px max(12px,env(safe-area-inset-bottom)); }
  .overlay-drawer .overlay-footer > * { width: 100%; }
}
@media (prefers-reduced-motion: reduce) { .overlay-drawer { animation-duration: 1ms; } }
</style>
