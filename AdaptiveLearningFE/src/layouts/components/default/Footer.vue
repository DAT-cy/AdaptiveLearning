<script setup>
import { settings } from '@/config/settings'

const { footerCopyright, copyright } = settings
</script>


<template>
  <footer class="py-2 bg-muted-foreground">
    <article class="container mx-auto">
      <del v-if="footerCopyright" class="flex items-center justify-center">{{ copyright }}</del>
    </article>
  </footer>
</template>
