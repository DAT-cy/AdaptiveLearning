<script setup lang="ts">
import { DropdownMenuItem as RekaDropdownMenuItem } from 'reka-ui'

/** An arrow-key reachable menu item with optional danger styling. */
withDefaults(defineProps<{ destructive?: boolean; disabled?: boolean }>(), { destructive: false, disabled: false })
</script>

<template>
  <RekaDropdownMenuItem :disabled="disabled" class="overlay-menu-item" :class="{ 'is-danger': destructive }">
    <span v-if="$slots.icon" class="overlay-menu-item-icon"><slot name="icon" /></span>
    <slot />
    <span v-if="$slots.shortcut" class="overlay-menu-shortcut"><slot name="shortcut" /></span>
  </RekaDropdownMenuItem>
</template>

<style scoped>
.overlay-menu-item { position: relative; display: flex; min-height: 40px; align-items: center; gap: 9px; padding: 0 10px; border-radius: var(--radius-sm); color: var(--text-strong); font-size: 14px; outline: none; cursor: pointer; user-select: none; }
.overlay-menu-item[data-highlighted] { color: var(--text); background: var(--surface-hover); }
.overlay-menu-item[data-disabled] { opacity: .5; cursor: not-allowed; }
.overlay-menu-item.is-danger { color: var(--rose-ink); }
.overlay-menu-item.is-danger[data-highlighted] { background: var(--rose-soft); }
.overlay-menu-item-icon { display: grid; width: 18px; height: 18px; flex: 0 0 auto; place-items: center; }
.overlay-menu-shortcut { margin-left: auto; color: var(--text-muted); font-size: 12px; }
</style>
