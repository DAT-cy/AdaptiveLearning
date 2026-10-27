import { readonly, shallowRef } from 'vue'
import type { OverlayToast, OverlayToastOptions } from './types'

const MAX_VISIBLE_TOASTS = 3
const items = shallowRef<OverlayToast[]>([])
let nextToastId = 0

function pushToast(type: OverlayToast['type'], title: string, options: OverlayToastOptions = {}) {
  const duration = options.duration ?? (type === 'error' ? 0 : 5000)
  const item: OverlayToast = {
    id: ++nextToastId,
    type,
    title,
    description: options.description,
    action: options.action,
    duration,
  }
  items.value = [item, ...items.value].slice(0, MAX_VISIBLE_TOASTS)
  return { id: item.id, dismiss: () => dismissToast(item.id) }
}

export function dismissToast(id: number) {
  items.value = items.value.filter((item) => item.id !== id)
}

/** Typed toast API shared by pages and service interceptors. */
export const toast = {
  success: (title: string, options?: OverlayToastOptions) => pushToast('success', title, options),
  error: (title: string, options?: OverlayToastOptions) => pushToast('error', title, { ...options, duration: 0 }),
  info: (title: string, options?: OverlayToastOptions) => pushToast('info', title, options),
}

export function useOverlayToasts() {
  return { items: readonly(items), dismissToast }
}
