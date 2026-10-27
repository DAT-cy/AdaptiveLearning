<script setup lang="ts">
import {
  DropdownMenuContent,
  DropdownMenuPortal,
  DropdownMenuRoot,
  DropdownMenuTrigger,
  TooltipContent,
  TooltipPortal,
  TooltipRoot,
  TooltipTrigger,
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
  <DropdownMenuRoot :open="open" @update:open="emit('update:open', $event)">
    <TooltipRoot v-if="triggerLabel" :delay-duration="400" ignore-non-keyboard-focus>
      <TooltipTrigger as-child>
        <DropdownMenuTrigger as-child><slot name="trigger" /></DropdownMenuTrigger>
      </TooltipTrigger>
      <TooltipPortal><TooltipContent class="overlay-tooltip" :side-offset="6">{{ triggerLabel }}</TooltipContent></TooltipPortal>
    </TooltipRoot>
    <DropdownMenuTrigger v-else as-child><slot name="trigger" /></DropdownMenuTrigger>
    <DropdownMenuPortal>
      <DropdownMenuContent class="overlay-menu" :side="side" :align="align" :side-offset="sideOffset" :collision-padding="8" :avoid-collisions="true" loop>
        <slot />
      </DropdownMenuContent>
    </DropdownMenuPortal>
  </DropdownMenuRoot>
</template>

<style scoped>
.overlay-menu { z-index: 95; min-width: 200px; max-width: calc(100vw - 16px); padding: 6px; border: 1px solid var(--ds-border); border-radius: var(--radius-lg); color: var(--text); background: var(--surface); box-shadow: var(--popover-shadow); }
</style>
