<script setup lang="ts">
/** Renders a right-aligned keyboard shortcut beside a menu item. */
</script>

<template><span class="overlay-menu-shortcut"><slot /></span></template>

<style scoped>
.overlay-menu-shortcut { margin-left: auto; color: var(--text-muted); font-size: 12px; }
</style>
