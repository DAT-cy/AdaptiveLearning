<script setup lang="ts">
import { nextTick, watch } from 'vue'
import type { Component } from 'vue'
import {
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'
import { CircleAlert, ShieldCheck, X } from 'lucide-vue-next'
import OverlayTooltip from './Tooltip.vue'
import { useConfirm } from './useConfirm'
import type { OverlaySize, OverlayVariant } from './types'

/** Shared app modal props; body and footer are supplied through slots. */
interface Props {
  open: boolean
  title: string
  description?: string
  icon?: Component
  size?: OverlaySize
  variant?: OverlayVariant
  footer?: boolean
  dismissible?: boolean
  dirty?: boolean
  submitting?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  variant: 'default',
  footer: false,
  dismissible: true,
  dirty: false,
  submitting: false,
})
const emit = defineEmits<{
  'update:open': [value: boolean]
  openChange: [value: boolean]
}>()
const { confirm } = useConfirm()
let closePending = false
const contentId = `shared-modal-${Math.random().toString(36).slice(2)}`
const titleId = `${contentId}-title`
const descriptionId = `${contentId}-description`
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

function onRootOpenChange(value: boolean) {
  if (value) setOpen(true)
  else void requestClose()
}

function onDismissAttempt(event: Event) {
  event.preventDefault()
  void requestClose()
}

async function focusInitialField() {
  await nextTick()
  const content = document.getElementById(contentId)
  const firstField = content?.querySelector<HTMLElement>(
    'input:not([disabled]), select:not([disabled]), textarea:not([disabled])'
  )
  const fallback = content?.querySelector<HTMLElement>('button:not([disabled])')
  ;(firstField || fallback || content)?.focus()
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

defineExpose({ requestClose })
</script>

<template>
  <DialogRoot :open="open" @update:open="onRootOpenChange">
    <DialogPortal>
      <DialogOverlay force-mount class="overlay-backdrop" />
      <DialogContent
        force-mount
        :id="contentId"
        class="overlay-modal"
        :class="[`size-${size}`, `is-${variant}`]"
        :aria-labelledby="titleId"
        :aria-describedby="descriptionId"
        tabindex="-1"
        @escape-key-down="onDismissAttempt"
        @pointer-down-outside="onDismissAttempt"
      >
        <header class="overlay-header">
          <span class="overlay-icon-tile" :class="`is-${variant}`">
            <component :is="icon || (variant === 'danger' ? CircleAlert : ShieldCheck)" :size="21" aria-hidden="true" />
          </span>
          <div class="overlay-header-copy">
            <DialogTitle :id="titleId" class="overlay-title">{{ title }}</DialogTitle>
            <DialogDescription :id="descriptionId" class="overlay-description" :class="{ 'overlay-visually-hidden': !description }">
              {{ description || title }}
            </DialogDescription>
          </div>
          <OverlayTooltip v-if="dismissible" label="Đóng">
            <template #trigger>
              <button type="button" class="overlay-close" aria-label="Đóng" :disabled="submitting" @click="requestClose">
                <X :size="18" aria-hidden="true" />
              </button>
            </template>
          </OverlayTooltip>
        </header>
        <div class="overlay-body"><slot :request-close="requestClose" :submitting="submitting" /></div>
        <footer v-if="footer || $slots.footer" class="overlay-footer">
          <slot name="footer" :request-close="requestClose" :submitting="submitting" />
        </footer>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<style scoped>
.overlay-backdrop { position: fixed; z-index: 90; inset: 0; background: var(--overlay-scrim); backdrop-filter: blur(4px); animation: overlay-fade-in 150ms ease both; }
.overlay-modal[data-state='closed'] { animation: modal-exit 120ms ease both; visibility: hidden; transition: visibility 120ms linear 120ms; pointer-events: none; }
.overlay-modal { position: fixed; z-index: 91; top: 50%; left: 50%; display: flex; width: min(var(--modal-width, 560px), calc(100vw - 32px)); max-height: calc(100vh - 48px); flex-direction: column; overflow: hidden; border: 1px solid var(--ds-border); border-radius: var(--radius-3xl); outline: none; background: var(--surface); box-shadow: var(--overlay-shadow); transform: translate(-50%, -50%); animation: modal-enter 180ms cubic-bezier(.16,1,.3,1) both; }
.overlay-modal.size-sm { --modal-width: 400px; }
.overlay-modal.size-md { --modal-width: 560px; }
.overlay-modal.size-lg { --modal-width: 720px; }
.overlay-modal.size-xl { --modal-width: 960px; }
.overlay-header { display: flex; min-width: 0; align-items: flex-start; gap: 14px; padding: 20px 24px; border-bottom: 1px solid var(--border-subtle); }
.overlay-icon-tile { display: grid; width: 42px; height: 42px; flex: 0 0 auto; place-items: center; border-radius: var(--radius-lg); color: #fff; background: var(--brand-gradient); }
.overlay-icon-tile.is-danger { color: var(--rose-ink); background: var(--rose-soft); }
.overlay-icon-tile.is-success { color: var(--primary-ink); background: var(--primary-soft); }
.overlay-header-copy { min-width: 0; flex: 1; padding-top: 1px; }
.overlay-title { margin: 0; color: var(--text); font-size: 17px; line-height: 24px; font-weight: 600; }
.overlay-description { margin-top: 3px; color: var(--text-muted); font-size: 13px; line-height: 1.5; }
.overlay-close { display: grid; width: 40px; height: 40px; flex: 0 0 auto; place-items: center; border: 0; border-radius: var(--radius-md); color: var(--text-secondary); background: transparent; cursor: pointer; }
.overlay-close:hover { background: var(--surface-hover); }
.overlay-body { min-height: 0; overflow: auto; padding: 24px; }
.overlay-footer { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; padding: 14px 24px; border-top: 1px solid var(--border-subtle); background: var(--surface-footer); }
.overlay-button { display: inline-flex; min-height: 44px; align-items: center; justify-content: center; gap: 8px; padding: 0 18px; border: 1px solid transparent; border-radius: var(--radius-lg); font: inherit; font-size: 14px; font-weight: 600; cursor: pointer; }
.overlay-button-primary { color: #fff; background: var(--ds-primary); box-shadow: var(--shadow-primary-btn); }
.overlay-button-primary:hover { background: var(--primary-hover); }
.overlay-button-secondary { border-color: var(--border-control); color: var(--text-strong); background: var(--surface); }
.overlay-button-secondary:hover { background: var(--surface-hover); }
.overlay-button-danger { color: #fff; background: var(--danger); }
.overlay-button-danger:hover { background: var(--danger-hover); }
@keyframes overlay-fade-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes modal-exit { from { opacity: 1; transform: translate(-50%,-50%) scale(1); } to { opacity: 0; transform: translate(-50%,-50%) scale(.96); } }
@keyframes modal-enter { from { opacity: 0; transform: translate(-50%,-48%) scale(.96); } to { opacity: 1; transform: translate(-50%,-50%) scale(1); } }
@media (max-width: 640px) {
  .overlay-modal { top: auto; bottom: 0; left: 0; width: 100%; max-width: 100%; max-height: calc(100dvh - 24px); border-radius: 18px 18px 0 0; transform: none; animation: sheet-enter 180ms cubic-bezier(.16,1,.3,1) both; }
  .overlay-modal::before { align-self: center; width: 36px; height: 4px; margin: 8px 0 -10px; border-radius: var(--radius-full); background: var(--drawer-handle); content: ''; }
  .overlay-header { padding: 16px; }
  .overlay-body { padding: 16px; }
  .overlay-footer { flex-direction: column-reverse; padding: 12px 16px max(12px,env(safe-area-inset-bottom)); }
  .overlay-footer > * { width: 100%; }
  @keyframes sheet-enter { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
}
@media (prefers-reduced-motion: reduce) {
  .overlay-backdrop,.overlay-modal { animation-duration: 1ms; }
  @keyframes modal-enter { from { opacity: 0; } to { opacity: 1; } }
  @keyframes modal-exit { from { opacity: 1; } to { opacity: 0; } }
}
</style>
