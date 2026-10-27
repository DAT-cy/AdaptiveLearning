import { shallowRef } from 'vue'
import type { ConfirmOptions, ConfirmRequest } from './types'

const currentRequest = shallowRef<ConfirmRequest | null>(null)
const currentOpen = shallowRef(false)
const pendingRequests: ConfirmRequest[] = []
let nextRequestId = 0
let isClosing = false

function showNextRequest() {
  if (currentRequest.value) return
  currentRequest.value = pendingRequests.shift() || null
  currentOpen.value = Boolean(currentRequest.value)
}

/** Opens the shared confirmation dialog and resolves to the user's choice. */
export function confirm(options: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    pendingRequests.push({ id: ++nextRequestId, ...options, resolve })
    showNextRequest()
  })
}

export function settleCurrentConfirmation(confirmed: boolean) {
  const request = currentRequest.value
  if (!request || isClosing) return
  isClosing = true
  currentOpen.value = false
  window.setTimeout(() => {
    currentRequest.value = null
    isClosing = false
    request.resolve(confirmed)
    showNextRequest()
  }, 130)
}

export function useConfirm() {
  return { confirm }
}

export function useConfirmProviderState() {
  return { currentRequest, currentOpen, settleCurrentConfirmation }
}
