<script setup lang="ts">
import { onUnmounted, watch } from 'vue'
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-vue-next'
import { useOverlayToasts } from './toast'

const { items, dismissToast } = useOverlayToasts()
const icons = {
  success: CheckCircle2,
  error: AlertCircle,
  info: Info,
} as const

const activeTimers = new Map<number, number>()

function startTimer(id: number, duration: number) {
  if (activeTimers.has(id)) return
  const timer = window.setTimeout(() => {
    activeTimers.delete(id)
    dismissToast(id)
  }, duration)
  activeTimers.set(id, timer)
}

watch(
  items,
  (newItems) => {
    for (const item of newItems) {
      if (!activeTimers.has(item.id)) {
        startTimer(item.id, item.duration || 2000)
      }
    }
  },
  { immediate: true, deep: true }
)

onUnmounted(() => {
  for (const timer of activeTimers.values()) {
    clearTimeout(timer)
  }
  activeTimers.clear()
})
</script>

<template>
  <Teleport to="body">
    <aside class="overlay-toast-viewport" aria-live="assertive" role="region" aria-label="Thông báo hệ thống">
      <TransitionGroup name="toast-popup">
        <div
          v-for="item in items"
          :key="item.id"
          class="overlay-toast"
          :class="`is-${item.type}`"
          role="alert"
        >
          <span class="overlay-toast-icon" :class="`is-${item.type}`">
            <component :is="icons[item.type]" :size="19" aria-hidden="true" />
          </span>

          <div class="overlay-toast-copy">
            <strong class="overlay-toast-title">{{ item.title }}</strong>
            <p v-if="item.description" class="overlay-toast-description">
              {{ item.description }}
            </p>
            <button
              v-if="item.action"
              type="button"
              class="overlay-toast-action"
              @click="item.action.onClick"
            >
              {{ item.action.label }}
            </button>
          </div>

          <button
            type="button"
            class="overlay-toast-close"
            aria-label="Đóng thông báo"
            @click="dismissToast(item.id)"
          >
            <X :size="15" aria-hidden="true" />
          </button>
        </div>
      </TransitionGroup>
    </aside>
  </Teleport>
</template>

<style scoped>
.overlay-toast-viewport {
  position: fixed !important;
  z-index: 999999 !important;
  top: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(420px, calc(100vw - 32px));
  margin: 0;
  padding: 0;
  pointer-events: none;
}

.overlay-toast {
  pointer-events: auto;
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 12px 36px -4px rgba(15, 23, 42, 0.18), 0 4px 12px -2px rgba(15, 23, 42, 0.08);
}

.overlay-toast.is-error {
  border-color: #fecaca;
  background-color: #fffafb;
}

.overlay-toast.is-success {
  border-color: #bbf7d0;
  background-color: #f6fdf9;
}

.overlay-toast.is-info {
  border-color: #bfdbfe;
  background-color: #f8faff;
}

.overlay-toast-icon {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 10px;
}

.overlay-toast-icon.is-error {
  color: #dc2626;
  background-color: #fee2e2;
}

.overlay-toast-icon.is-success {
  color: #16a34a;
  background-color: #dcfce7;
}

.overlay-toast-icon.is-info {
  color: #2563eb;
  background-color: #dbeafe;
}

.overlay-toast-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  flex: 1;
  padding-top: 1px;
}

.overlay-toast-title {
  color: #0f172a;
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.35;
}

.overlay-toast.is-error .overlay-toast-title {
  color: #991b1b;
}

.overlay-toast.is-success .overlay-toast-title {
  color: #166534;
}

.overlay-toast.is-info .overlay-toast-title {
  color: #1e40af;
}

.overlay-toast-description {
  margin: 0;
  color: #475569;
  font-size: 12.5px;
  line-height: 1.45;
  word-break: break-word;
}

.overlay-toast.is-error .overlay-toast-description {
  color: #7f1d1d;
}

.overlay-toast-action {
  align-self: flex-start;
  margin-top: 6px;
  padding: 4px 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
  cursor: pointer;
}

.overlay-toast-close {
  display: grid;
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
  place-items: center;
  margin: -2px -4px 0 0;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.15s ease;
}

.overlay-toast-close:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #0f172a;
}

/* Animations */
.toast-popup-enter-active,
.toast-popup-leave-active {
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-popup-enter-from {
  opacity: 0;
  transform: translateX(40px) scale(0.94);
}

.toast-popup-leave-to {
  opacity: 0;
  transform: translateX(40px) scale(0.92);
}

@media (max-width: 640px) {
  .overlay-toast-viewport {
    top: 12px;
    right: 12px;
    left: 12px;
    width: auto;
  }
}
</style>
