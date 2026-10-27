<script setup lang="ts">
import { DropdownMenuLabel as RekaDropdownMenuLabel } from 'reka-ui'

/** A non-interactive overline title within a dropdown menu. */
</script>

<template><RekaDropdownMenuLabel class="overlay-menu-label"><slot /></RekaDropdownMenuLabel></template>

<style scoped>
.overlay-menu-label { padding: 7px 10px 5px; color: var(--text-overline); font-size: 11px; font-weight: 700; }
</style>
