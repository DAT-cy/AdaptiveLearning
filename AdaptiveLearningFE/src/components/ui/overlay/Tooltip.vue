<script setup lang="ts">
import {
  TooltipContent,
  TooltipPortal,
  TooltipRoot,
  TooltipTrigger,
} from 'reka-ui'

/** Delayed hover and immediate keyboard-focus tooltip. */
interface Props {
  label: string
  side?: 'top' | 'right' | 'bottom' | 'left'
  delayDuration?: number
}

withDefaults(defineProps<Props>(), { side: 'top', delayDuration: 400 })
</script>

<template>
  <TooltipRoot :delay-duration="delayDuration">
    <TooltipTrigger as-child><slot name="trigger" /></TooltipTrigger>
    <TooltipPortal>
      <TooltipContent class="overlay-tooltip" :side="side" :side-offset="6" :collision-padding="8">
        {{ label }}
      </TooltipContent>
    </TooltipPortal>
  </TooltipRoot>
</template>

<style scoped>
.overlay-tooltip { z-index: 110; max-width: 240px; padding: 6px 10px; border-radius: var(--radius-sm); color: #fff; background: var(--tooltip-surface); font-size: 12.5px; line-height: 1.4; box-shadow: var(--shadow-card); }
</style>
