<script setup>
import { cn } from "@/lib/utils";

const props = defineProps({
  class: { type: null, required: false },
});
</script>

<template>
  <p :class="cn('text-sm text-text-secondary leading-relaxed', props.class)">
    <slot />
  </p>
</template>
