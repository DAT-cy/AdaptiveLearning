<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogOverlay,
  AlertDialogPortal,
  AlertDialogRoot,
  AlertDialogTitle,
} from 'reka-ui'
import { AlertTriangle, CircleHelp } from 'lucide-vue-next'

/** Accessible confirmation prompt shown by the app-level provider. */
interface Props {
  open: boolean
  title: string
  description?: string
  confirmText?: string
  cancelText?: string
  variant?: 'default' | 'danger'
}

const props = withDefaults(defineProps<Props>(), {
  cancelText: 'Hủy',
  confirmText: 'Xác nhận',
  variant: 'default',
})
const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
const cancelButton = ref<HTMLButtonElement | null>(null)
let settled = false

watch(() => props.open, (open) => { settled = !open })

function settle(choice: 'confirm' | 'cancel') {
  if (settled) return
  settled = true
  emit(choice)
}

function onOpenAutoFocus(event: Event) {
  event.preventDefault()
  cancelButton.value?.focus()
}
</script>

<template>
  <AlertDialogRoot :open="open" @update:open="(value) => !value && settle('cancel')">
    <AlertDialogPortal>
      <AlertDialogOverlay class="overlay-backdrop overlay-confirm-backdrop" />
      <AlertDialogContent class="overlay-confirm" @open-auto-focus="onOpenAutoFocus">
        <header class="overlay-confirm-header">
          <span class="overlay-icon-tile" :class="`is-${variant}`">
            <component :is="variant === 'danger' ? AlertTriangle : CircleHelp" :size="21" aria-hidden="true" />
          </span>
          <div class="overlay-confirm-copy">
            <AlertDialogTitle class="overlay-confirm-title">{{ title }}</AlertDialogTitle>
            <AlertDialogDescription class="overlay-confirm-description" :class="{ 'overlay-visually-hidden': !description }">
              {{ description || title }}
            </AlertDialogDescription>
          </div>
        </header>
        <footer class="overlay-footer">
          <AlertDialogCancel as-child>
            <button ref="cancelButton" type="button" class="overlay-button overlay-button-secondary" @click="settle('cancel')">
              {{ cancelText }}
            </button>
          </AlertDialogCancel>
          <AlertDialogAction as-child>
            <button type="button" class="overlay-button" :class="variant === 'danger' ? 'overlay-button-danger' : 'overlay-button-primary'" @click="settle('confirm')">
              {{ confirmText }}
            </button>
          </AlertDialogAction>
        </footer>
      </AlertDialogContent>
    </AlertDialogPortal>
  </AlertDialogRoot>
</template>

<style scoped>
.overlay-confirm { position: fixed; z-index: 91; top: 50%; left: 50%; display: grid; width: min(400px,calc(100vw - 32px)); overflow: hidden; border: 1px solid var(--ds-border); border-radius: var(--radius-3xl); outline: none; background: var(--surface); box-shadow: var(--overlay-shadow); transform: translate(-50%,-50%); animation: modal-enter 180ms cubic-bezier(.16,1,.3,1) both; }
.overlay-confirm[data-state='closed'] { animation: modal-exit 120ms ease both; visibility: hidden; transition: visibility 120ms linear 120ms; pointer-events: none; }
.overlay-confirm-backdrop[data-state='closed'] { animation: overlay-fade-out 120ms ease both; }
.overlay-confirm-header { display: flex; align-items: flex-start; gap: 14px; padding: 20px 24px; }
.overlay-confirm-copy { min-width: 0; padding-top: 1px; }
.overlay-confirm-title { color: var(--text); font-size: 17px; line-height: 24px; font-weight: 600; }
.overlay-confirm-description { margin-top: 4px; color: var(--text-muted); font-size: 13px; line-height: 1.5; }
@media (max-width: 640px) {
  .overlay-confirm { top: auto; bottom: 0; left: 0; width: 100%; max-width: 100%; border-radius: 18px 18px 0 0; transform: none; animation: sheet-enter 180ms cubic-bezier(.16,1,.3,1) both; }
  .overlay-confirm::before { display: block; width: 36px; height: 4px; margin: 8px auto -6px; border-radius: var(--radius-full); background: var(--drawer-handle); content: ''; }
  .overlay-confirm-header { padding: 18px 16px; }
  .overlay-confirm .overlay-footer { flex-direction: column-reverse; padding: 12px 16px max(12px,env(safe-area-inset-bottom)); }
  .overlay-confirm .overlay-footer > * { width: 100%; }
}
@media (prefers-reduced-motion: reduce) { .overlay-confirm { animation-duration: 1ms; } }
</style>
