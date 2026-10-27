<script setup lang="ts">
import { DropdownMenuItem as RekaDropdownMenuItem } from 'reka-ui'

/** An arrow-key reachable menu item with optional danger styling. */
withDefaults(defineProps<{ destructive?: boolean; disabled?: boolean }>(), { destructive: false, disabled: false })

const emit = defineEmits<{
  (e: 'select', event: Event): void
  (e: 'click', event: MouseEvent): void
}>()
</script>

<template>
  <RekaDropdownMenuItem
    :disabled="disabled"
    class="overlay-menu-item"
    :class="{ 'is-danger': destructive }"
    @select="emit('select', $event)"
    @click="emit('click', $event)"
  >
    <span v-if="$slots.icon" class="overlay-menu-item-icon"><slot name="icon" /></span>
    <slot />
    <span v-if="$slots.shortcut" class="overlay-menu-shortcut"><slot name="shortcut" /></span>
  </RekaDropdownMenuItem>
</template>

<style scoped>
.overlay-menu-item {
  position: relative;
  display: flex;
  min-height: 38px;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  border-radius: 8px;
  color: #1e293b;
  font-size: 13px;
  font-weight: 500;
  outline: none;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.12s ease, color 0.12s ease;
}
.overlay-menu-item[data-highlighted] {
  color: #0f172a;
  background: #f1f5f9;
}
.overlay-menu-item[data-disabled] {
  opacity: 0.5;
  cursor: not-allowed;
}
.overlay-menu-item.is-danger {
  color: #dc2626;
}
.overlay-menu-item.is-danger[data-highlighted] {
  background: #fef2f2;
  color: #b91c1c;
}
.overlay-menu-item-icon {
  display: grid;
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  place-items: center;
}
.overlay-menu-shortcut {
  margin-left: auto;
  color: #94a3b8;
  font-size: 12px;
}
</style>
