<script setup>
import { cn } from "@/lib/utils";

const props = defineProps({
  class: { type: null, required: false },
});
</script>

<template>
  <h3
    :class="
      cn('text-base sm:text-lg font-semibold leading-tight text-text-primary', props.class)
    "
  >
    <slot />
  </h3>
</template>
