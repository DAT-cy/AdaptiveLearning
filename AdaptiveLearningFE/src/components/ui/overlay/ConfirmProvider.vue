<script setup lang="ts">
import ConfirmDialog from './ConfirmDialog.vue'
import { useConfirmProviderState } from './useConfirm'

const { currentRequest, currentOpen, settleCurrentConfirmation } = useConfirmProviderState()
</script>

<template>
  <slot />
  <ConfirmDialog
    v-if="currentRequest"
    :key="currentRequest.id"
    :open="currentOpen"
    :title="currentRequest.title"
    :description="currentRequest.description"
    :confirm-text="currentRequest.confirmText"
    :cancel-text="currentRequest.cancelText"
    :variant="currentRequest.variant"
    @confirm="settleCurrentConfirmation(true)"
    @cancel="settleCurrentConfirmation(false)"
  />
</template>
