<script setup lang="ts">
import {
  DropdownMenuContent,
  DropdownMenuPortal,
  DropdownMenuRoot,
  DropdownMenuTrigger,
} from 'reka-ui'

/** Keyboard-navigable dropdown menu with collision-aware positioning. */
interface Props {
  open?: boolean
  side?: 'top' | 'right' | 'bottom' | 'left'
  align?: 'start' | 'center' | 'end'
  sideOffset?: number
  triggerLabel?: string
}

withDefaults(defineProps<Props>(), {
  side: 'bottom',
  align: 'end',
  sideOffset: 6,
})
const emit = defineEmits<{ 'update:open': [value: boolean] }>()
</script>

<template>
  <DropdownMenuRoot v-bind="open !== undefined ? { open } : {}" @update:open="emit('update:open', $event)">
    <DropdownMenuTrigger as-child :title="triggerLabel"><slot name="trigger" /></DropdownMenuTrigger>
    <DropdownMenuPortal>
      <DropdownMenuContent class="overlay-menu" :side="side" :align="align" :side-offset="sideOffset" :collision-padding="8" :avoid-collisions="true" loop>
        <slot />
      </DropdownMenuContent>
    </DropdownMenuPortal>
  </DropdownMenuRoot>
</template>

<style scoped>
.overlay-menu {
  z-index: 9999;
  min-width: 240px;
  max-width: calc(100vw - 16px);
  padding: 8px 6px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  color: #0f172a;
  background-color: #ffffff !important;
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.14), 0 4px 10px -2px rgba(15, 23, 42, 0.08);
}
</style>
