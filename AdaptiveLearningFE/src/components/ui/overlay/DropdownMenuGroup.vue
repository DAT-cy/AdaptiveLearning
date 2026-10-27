<script setup lang="ts">
import { DropdownMenuGroup as RekaDropdownMenuGroup } from 'reka-ui'

/** Groups related dropdown actions while preserving Reka keyboard navigation. */
defineProps<{ ariaLabel?: string }>()
</script>

<template>
  <RekaDropdownMenuGroup :aria-label="ariaLabel"><slot /></RekaDropdownMenuGroup>
</template>
