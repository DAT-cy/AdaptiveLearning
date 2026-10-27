<script setup lang="ts">
import {
  PopoverArrow,
  PopoverContent,
  PopoverPortal,
  PopoverRoot,
  PopoverTrigger,
} from 'reka-ui'

/** Collision-aware popover configured through composable trigger/content slots. */
interface Props {
  open?: boolean
  side?: 'top' | 'right' | 'bottom' | 'left'
  align?: 'start' | 'center' | 'end'
  arrow?: boolean
  modal?: boolean
}

withDefaults(defineProps<Props>(), {
  side: 'bottom',
  align: 'center',
  arrow: false,
  modal: false,
})
const emit = defineEmits<{ 'update:open': [value: boolean] }>()
</script>

<template>
  <PopoverRoot :open="open" :modal="modal" @update:open="emit('update:open', $event)">
    <PopoverTrigger as-child><slot name="trigger" /></PopoverTrigger>
    <PopoverPortal>
      <PopoverContent class="overlay-popover" :side="side" :align="align" :side-offset="8" :collision-padding="8" :avoid-collisions="true">
        <slot />
        <PopoverArrow v-if="arrow" class="overlay-popover-arrow" :width="12" :height="6" />
      </PopoverContent>
    </PopoverPortal>
  </PopoverRoot>
</template>

<style scoped>
.overlay-popover { z-index: 95; max-width: min(360px,calc(100vw - 24px)); padding: 16px; border: 1px solid var(--ds-border); border-radius: var(--radius-lg); color: var(--text); background: var(--surface); box-shadow: var(--popover-shadow); transform-origin: var(--reka-popover-content-transform-origin); animation: popover-enter 120ms cubic-bezier(.16,1,.3,1) both; }
.overlay-popover-arrow { fill: var(--surface); }
@keyframes popover-enter { from { opacity: 0; scale: .96; } to { opacity: 1; scale: 1; } }
@media (prefers-reduced-motion: reduce) { .overlay-popover { animation-duration: 1ms; } }
</style>
